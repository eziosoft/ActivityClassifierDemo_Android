"""
HAR Model Evaluation

Evaluate trained model on test set and show detailed metrics.
"""

import numpy as np
import torch
import torch.nn as nn
from sklearn.metrics import classification_report, confusion_matrix
import json

from model.har_cnn1d import HAR_CNN1D

# ============================================================================
# Configuration
# ============================================================================
DATA_PATH = "processed_data/processed_data.npz"
MODEL_PATH = "model/har_cnn1d.pth"
NORM_PARAMS_PATH = "processed_data/normalization_params.json"

# ============================================================================
# Load Activity Labels
# ============================================================================
with open(NORM_PARAMS_PATH, 'r') as f:
    norm_params = json.load(f)
    activity_labels = norm_params.get('activity_labels', {})
    # Convert string keys to int
    activity_labels = {int(k): v for k, v in activity_labels.items()}

print("Activity Labels:", activity_labels)
print()

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
# Load Test Data
# ============================================================================
print("Loading test data...")
data = np.load(DATA_PATH)
X_test = torch.from_numpy(data['X_test']).float()
y_test = torch.from_numpy(data['y_test']).long()

print(f"Test set: {X_test.shape}")
print(f"Test labels distribution: {torch.bincount(y_test).tolist()}")
print()

# ============================================================================
# Load Model
# ============================================================================
print("Loading model...")
checkpoint = torch.load(MODEL_PATH, map_location=device)

# Extract model parameters from checkpoint
if isinstance(checkpoint, dict):
    input_channels = checkpoint.get('input_channels', 8)
    num_classes = checkpoint.get('num_classes', 3)
    model_state = checkpoint['model_state_dict']
else:
    # Old format - just state dict
    input_channels = 8
    num_classes = 3
    model_state = checkpoint

model = HAR_CNN1D(input_size=input_channels, num_classes=num_classes).to(device)
model.load_state_dict(model_state)
model.eval()

print(f"Model loaded: HAR_CNN1D(input_size={input_channels}, num_classes={num_classes})")
print()

# ============================================================================
# Evaluate
# ============================================================================
print("Evaluating on test set...")
criterion = nn.CrossEntropyLoss()

all_predictions = []
all_labels = []
test_loss = 0.0

with torch.no_grad():
    for i in range(len(X_test)):
        x = X_test[i:i+1].to(device)
        y = y_test[i:i+1].to(device)
        
        output = model(x)
        loss = criterion(output, y)
        test_loss += loss.item()
        
        _, predicted = torch.max(output, 1)
        all_predictions.append(predicted.cpu().item())
        all_labels.append(y.cpu().item())

test_loss /= len(X_test)
test_acc = 100 * np.mean(np.array(all_predictions) == np.array(all_labels))

print(f"Test Loss: {test_loss:.4f}")
print(f"Test Accuracy: {test_acc:.2f}%")
print()

# ============================================================================
# Detailed Metrics
# ============================================================================
print("=" * 60)
print("Classification Report")
print("=" * 60)

# Get label names in order
label_names = [activity_labels.get(i, f"Class_{i}") for i in range(num_classes)]

print(classification_report(
    all_labels, 
    all_predictions,
    target_names=label_names,
    digits=4
))

print("=" * 60)
print("Confusion Matrix")
print("=" * 60)
cm = confusion_matrix(all_labels, all_predictions)
print()
print("          ", end="")
for name in label_names:
    print(f"{name:>10}", end="")
print()
print("-" * (11 + 10 * len(label_names)))

for i, name in enumerate(label_names):
    print(f"{name:>10}|", end="")
    for j in range(len(label_names)):
        print(f"{cm[i, j]:10d}", end="")
    print()

print()
print("=" * 60)
print(f"Final Test Accuracy: {test_acc:.2f}%")
print("=" * 60)

