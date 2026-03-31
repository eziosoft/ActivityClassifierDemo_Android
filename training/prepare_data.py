"""
Human Activity Recognition (HAR) Data Preparation

Loads continuous sensor data from CSV, creates sliding windows (128 samples, 50% overlap),
applies Z-score normalization, and splits into train/validation/test sets.

CSV format: timestamp,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,acc_mag_sq,gyro_mag_sq,label_id
Activity labels defined in dataset/activity_labels.txt
"""

import glob
import json
import numpy as np
import pandas as pd
import os


# ============================================================================
# Configuration
# ============================================================================
INPUT_CSV_FOLDER = "dataset/"
ACTIVITY_LABELS_FILE = "dataset/activity_labels.txt"
OUTPUT_FOLDER = "processed_data/"

WINDOW_SIZE = 128
STEP_SIZE = 64  # 50% overlap

if not os.path.exists(OUTPUT_FOLDER):
    os.makedirs(OUTPUT_FOLDER)


# ============================================================================
# Load Activity Labels
# ============================================================================
def load_activity_labels(filepath):
    """Load activity labels from file. Format: 'id ACTIVITY_NAME'"""
    labels = {}
    if os.path.exists(filepath):
        with open(filepath, 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('UNKNOWN'):
                    parts = line.split(maxsplit=1)
                    if len(parts) == 2:
                        label_id = int(parts[0])
                        label_name = parts[1]
                        labels[label_id] = label_name
    return labels

activity_labels = load_activity_labels(ACTIVITY_LABELS_FILE)
print(f"Loaded activity labels: {activity_labels}")
print()


# ============================================================================
# Helper Functions
# ============================================================================
def normalize(data, mean, std):
    """Apply z-score normalization: (x - mean) / std."""
    return (data - mean) / std


def create_windows(data, labels, window_size, step_size):
    """
    Create sliding windows from continuous data.
    
    Args:
        data: numpy array of shape (n_samples, n_features)
        labels: numpy array of shape (n_samples,)
        window_size: number of samples per window
        step_size: step size for sliding window
    
    Returns:
        windows: numpy array of shape (n_windows, window_size, n_features)
        window_labels: numpy array of shape (n_windows,) - majority label per window
    """
    n_samples = len(data)
    windows = []
    window_labels = []
    
    for start_idx in range(0, n_samples - window_size + 1, step_size):
        end_idx = start_idx + window_size
        
        # Extract window
        window = data[start_idx:end_idx]
        window_label_segment = labels[start_idx:end_idx]
        
        # Use majority voting for window label
        unique, counts = np.unique(window_label_segment, return_counts=True)
        majority_label = unique[np.argmax(counts)]
        
        windows.append(window)
        window_labels.append(majority_label)
    
    return np.array(windows), np.array(window_labels)


# ============================================================================
# Load Raw Data
# ============================================================================
csv_files = sorted(glob.glob(os.path.join(INPUT_CSV_FOLDER, "*.csv")))
if not csv_files:
    raise FileNotFoundError(f"No CSV files found in {INPUT_CSV_FOLDER}")

print(f"Found {len(csv_files)} CSV file(s):")
for f in csv_files:
    print(f"  {f}")

df = pd.concat([pd.read_csv(f) for f in csv_files], ignore_index=True)

print(f"Loaded {len(df)} samples")
print(f"Columns: {df.columns.tolist()}")

# Show label distribution with names
label_counts = df['label_id'].value_counts().sort_index()
print(f"Labels distribution:")
for label_id, count in label_counts.items():
    label_name = activity_labels.get(label_id, f"UNKNOWN_{label_id}")
    print(f"  {label_id} ({label_name}): {count} samples")

# Extract features (8 channels)
feature_cols = ['acc_x', 'acc_y', 'acc_z', 'gyro_x', 'gyro_y', 'gyro_z', 'acc_mag_sq', 'gyro_mag_sq']
X_continuous = df[feature_cols].values.astype(np.float32)
y_continuous = df['label_id'].values.astype(np.int32)

print(f"\nContinuous data shape: {X_continuous.shape}")


# ============================================================================
# Split Data by Activity Segments (to avoid class imbalance)
# ============================================================================
print("\nSplitting data by activity segments (to ensure all classes in all splits)...")

# Group continuous data by activity (find activity change points)
activity_changes = np.where(np.diff(y_continuous) != 0)[0] + 1
activity_segments = np.split(np.arange(len(y_continuous)), activity_changes)

print(f"Found {len(activity_segments)} activity segments")

# Split each activity's segments into train/val/test (70%/15%/15%)
train_indices = []
val_indices = []
test_indices = []

for label_id in np.unique(y_continuous):
    # Get all segments for this activity
    label_segments = [seg for seg in activity_segments if y_continuous[seg[0]] == label_id]
    label_indices = np.concatenate(label_segments)
    
    # Shuffle segments for this activity
    np.random.seed(42)
    np.random.shuffle(label_indices)
    
    # Split 70/15/15
    n_samples = len(label_indices)
    n_train = int(0.70 * n_samples)
    n_val = int(0.15 * n_samples)
    
    train_indices.extend(label_indices[:n_train])
    val_indices.extend(label_indices[n_train:n_train + n_val])
    test_indices.extend(label_indices[n_train + n_val:])
    
    label_name = activity_labels.get(label_id, f"ID_{label_id}")
    print(f"  {label_name}: {len(label_indices)} samples → train={n_train}, val={n_val}, test={n_samples - n_train - n_val}")

# Convert to arrays and sort (to maintain some temporal order within each split)
train_indices = np.sort(np.array(train_indices))
val_indices = np.sort(np.array(val_indices))
test_indices = np.sort(np.array(test_indices))

X_train_continuous = X_continuous[train_indices]
y_train_continuous = y_continuous[train_indices]

X_val_continuous = X_continuous[val_indices]
y_val_continuous = y_continuous[val_indices]

X_test_continuous = X_continuous[test_indices]
y_test_continuous = y_continuous[test_indices]

print(f"\nContinuous data split:")
print(f"  Train: {X_train_continuous.shape}, labels: {np.bincount(y_train_continuous)}")
print(f"  Val: {X_val_continuous.shape}, labels: {np.bincount(y_val_continuous)}")
print(f"  Test: {X_test_continuous.shape}, labels: {np.bincount(y_test_continuous)}")


# ============================================================================
# Create Sliding Windows for Each Split
# ============================================================================
print(f"\nCreating windows (size={WINDOW_SIZE}, step={STEP_SIZE})...")

X_train_windows, y_train = create_windows(X_train_continuous, y_train_continuous, WINDOW_SIZE, STEP_SIZE)
X_val_windows, y_val = create_windows(X_val_continuous, y_val_continuous, WINDOW_SIZE, STEP_SIZE)
X_test_windows, y_test = create_windows(X_test_continuous, y_test_continuous, WINDOW_SIZE, STEP_SIZE)

print(f"Train windows: {X_train_windows.shape}")
print(f"Val windows: {X_val_windows.shape}")
print(f"Test windows: {X_test_windows.shape}")

# Show window label distribution with names
for split_name, windows in [("Train", y_train), ("Val", y_val), ("Test", y_test)]:
    window_label_counts = pd.Series(windows).value_counts().sort_index()
    print(f"\n{split_name} labels:")
    for label_id, count in window_label_counts.items():
        label_name = activity_labels.get(label_id, f"UNKNOWN_{label_id}")
        print(f"  {label_id} ({label_name}): {count} windows")

# Verify labels are 0-based as expected
unique_labels = np.unique(np.concatenate([y_train, y_val, y_test]))
print(f"\nUnique labels across all splits: {unique_labels}")
if unique_labels.min() != 0:
    print(f"WARNING: Expected labels to start from 0, but found min={unique_labels.min()}")
    print(f"Adjusting labels to be 0-based...")
    offset = unique_labels.min()
    y_train = y_train - offset
    y_val = y_val - offset
    y_test = y_test - offset
    print(f"New label range: 0 to {max(y_train.max(), y_val.max(), y_test.max())}")


# ============================================================================
# Compute Normalization Parameters (Z-score)
# ============================================================================
print("\nComputing normalization parameters from training data...")
# Compute mean/std per channel (across all samples and time steps)
channel_mean = X_train_windows.mean(axis=(0, 1)).astype(np.float32)
channel_std = X_train_windows.std(axis=(0, 1)).astype(np.float32)

print(f"Channel means: {channel_mean.tolist()}")
print(f"Channel stds: {channel_std.tolist()}")

# Save normalization params to JSON for Android app
normalization_params = {
    "mean": channel_mean.tolist(),
    "std": channel_std.tolist(),
    "feature_names": feature_cols,
    "activity_labels": activity_labels
}
norm_path = OUTPUT_FOLDER + "normalization_params.json"
with open(norm_path, "w") as f:
    json.dump(normalization_params, f, indent=2)
print(f"Saved normalization parameters to {norm_path}")


# ============================================================================
# Apply Normalization
# ============================================================================
print("\nApplying Z-score normalization...")
X_train = normalize(X_train_windows, channel_mean, channel_std)
X_val = normalize(X_val_windows, channel_mean, channel_std)
X_test = normalize(X_test_windows, channel_mean, channel_std)


# ============================================================================
# Save Processed Data
# ============================================================================
output_path = OUTPUT_FOLDER + "processed_data.npz"
np.savez(output_path,
         X_train=X_train, y_train=y_train,
         X_val=X_val, y_val=y_val,
         X_test=X_test, y_test=y_test)

print(f"\nSaved processed data to {output_path}")
print("\n" + "="*60)
print("Summary:")
print("="*60)
print(f"Input folder: {INPUT_CSV_FOLDER} ({len(csv_files)} file(s))")
print(f"Window size: {WINDOW_SIZE}")
print(f"Step size: {STEP_SIZE} (50% overlap)")
print(f"Features: {len(feature_cols)} channels")
print(f"Train samples: {len(X_train)}")
print(f"Val samples: {len(X_val)}")
print(f"Test samples: {len(X_test)}")
print(f"Output: {output_path}")
print(f"Normalization params: {norm_path}")
print("="*60)

