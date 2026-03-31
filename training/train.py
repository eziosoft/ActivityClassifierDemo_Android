"""
HAR Model Training

Training CNN1D model for Human Activity Recognition.
Input: 8 channels (acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq)
Output: 3 classes (STANDING=0, WALKING=1, JUMPING=2)
"""

import random

import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

from model.har_cnn1d import HAR_CNN1D
from tools.graph import TrainingGraph

# ============================================================================
# Configuration
# ============================================================================
DATA_PATH = "processed_data/processed_data.npz"
MODEL_PATH = "model/har_cnn1d.pth"
CHECKPOINT_PATH = "model/checkpoint.pth"
PLOT_PATH = "model/training_plots.png"

SEED = 42
BATCH_SIZE = 32
NUM_EPOCHS = 200
LEARNING_RATE = 1e-3
PATIENCE = 10
MIN_LR = 1e-6
EARLY_STOP_PATIENCE = 15  # Stop if val acc doesn't improve for 15 epochs

torch.manual_seed(SEED)
np.random.seed(SEED)
random.seed(SEED)


# ============================================================================
# Load Data
# ============================================================================
print("Loading data...")
data = np.load(DATA_PATH)
X_train = torch.from_numpy(data['X_train']).float()
y_train = torch.from_numpy(data['y_train']).long()
X_val = torch.from_numpy(data['X_val']).float()
y_val = torch.from_numpy(data['y_val']).long()

print(f"Train: {X_train.shape}, Val: {X_val.shape}")
print(f"Input channels: {X_train.shape[2]}")
print(f"Sequence length: {X_train.shape[1]}")
print(f"Number of classes: {len(torch.unique(y_train))}")
print(f"Train label distribution: {torch.bincount(y_train).tolist()}")
print(f"Val label distribution: {torch.bincount(y_val).tolist()}")
print()

train_dataset = TensorDataset(X_train, y_train)
val_dataset = TensorDataset(X_val, y_val)
train_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, shuffle=True)
val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE, shuffle=False)


# ============================================================================
# Setup Device
# ============================================================================
if torch.cuda.is_available():
    device = torch.device("cuda")
elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
    device = torch.device("mps")
else:
    device = torch.device("cpu")
print(f"Device: {device}")


# ============================================================================
# Initialize Model, Loss, Optimizer
# ============================================================================
input_channels = X_train.shape[2]  # Should be 8
num_classes = len(torch.unique(y_train))  # Should be 3

model = HAR_CNN1D(input_size=input_channels, num_classes=num_classes).to(device)
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=LEARNING_RATE)
scheduler = optim.lr_scheduler.ReduceLROnPlateau(
    optimizer, mode='max', factor=0.5, patience=PATIENCE
)

print(f"Model: HAR_CNN1D(input_size={input_channels}, num_classes={num_classes})")
print(f"Total parameters: {sum(p.numel() for p in model.parameters()):,}")
print()

start_epoch = 0
best_acc = 0
epochs_without_improvement = 0
graph = TrainingGraph(save_path=PLOT_PATH)


# ============================================================================
# Training Loop
# ============================================================================
print(f"{'Epoch':>5} | {'LR':>8} | {'Train Loss':>10} | {'Train Acc':>9} | {'Val Loss':>9} | {'Val Acc':>7}")
print('-' * 65)

for epoch in range(start_epoch, NUM_EPOCHS):

    # Training
    model.train()
    train_loss = 0.0
    train_correct = 0
    train_total = 0

    for X_batch, y_batch in train_loader:
        X_batch, y_batch = X_batch.to(device), y_batch.to(device)
        optimizer.zero_grad()
        outputs = model(X_batch)
        loss = criterion(outputs, y_batch)
        loss.backward()
        optimizer.step()

        train_loss += loss.item() * X_batch.size(0)
        _, predicted = torch.max(outputs, 1)
        train_total += y_batch.size(0)
        train_correct += (predicted == y_batch).sum().item()

    avg_train_loss = train_loss / train_total
    train_acc = 100 * train_correct / train_total

    # Validation
    model.eval()
    val_loss = 0.0
    val_correct = 0
    val_total = 0

    with torch.no_grad():
        for X_batch, y_batch in val_loader:
            X_batch, y_batch = X_batch.to(device), y_batch.to(device)
            outputs = model(X_batch)
            loss = criterion(outputs, y_batch)
            val_loss += loss.item() * X_batch.size(0)
            _, predicted = torch.max(outputs, 1)
            val_total += y_batch.size(0)
            val_correct += (predicted == y_batch).sum().item()

    avg_val_loss = val_loss / val_total
    val_acc = 100 * val_correct / val_total
    
    current_lr = optimizer.param_groups[0]['lr']
    
    print(f"{epoch+1:5d} | {current_lr:.2e} | {avg_train_loss:10.4f} | {train_acc:9.2f} | {avg_val_loss:9.4f} | {val_acc:7.2f}")

    # Update graph
    graph.update(epoch + 1, avg_train_loss, avg_val_loss, train_acc, val_acc)

    scheduler.step(val_acc)

    # Save best model
    if val_acc > best_acc:
        best_acc = val_acc
        epochs_without_improvement = 0
        torch.save({
            'epoch': epoch,
            'model_state_dict': model.state_dict(),
            'optimizer_state_dict': optimizer.state_dict(),
            'val_acc': val_acc,
            'input_channels': input_channels,
            'num_classes': num_classes
        }, MODEL_PATH)
        print(f"         → Saved best model (val_acc: {val_acc:.2f}%)")
    else:
        epochs_without_improvement += 1

    # Save checkpoint every 10 epochs
    if (epoch + 1) % 10 == 0:
        torch.save({
            'epoch': epoch,
            'model_state_dict': model.state_dict(),
            'optimizer_state_dict': optimizer.state_dict(),
            'val_acc': val_acc,
            'best_acc': best_acc,
            'input_channels': input_channels,
            'num_classes': num_classes
        }, CHECKPOINT_PATH)

    # Early stopping: check if validation accuracy hasn't improved
    if epochs_without_improvement >= EARLY_STOP_PATIENCE:
        print(f"\nEarly stopping: No improvement in validation accuracy for {EARLY_STOP_PATIENCE} epochs")
        print(f"Best validation accuracy: {best_acc:.2f}%")
        break

graph.close()
print(f"\nTraining complete!")
print(f"Best validation accuracy: {best_acc:.2f}%")
print(f"Best model saved to: {MODEL_PATH}")
