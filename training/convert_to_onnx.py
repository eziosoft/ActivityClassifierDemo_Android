"""
Convert HAR_CNN1D PyTorch Model to ONNX Format

This script converts the trained HAR_CNN1D model to ONNX format
for inference on Android devices.

Automatically detects model parameters from checkpoint.
"""

import torch
import onnxruntime as ort
import numpy as np

from model.har_cnn1d import HAR_CNN1D


# =============================================================================
# Configuration
# =============================================================================

MODEL_CHECKPOINT_PATH = "model/har_cnn1d.pth"
OUTPUT_ONNX_PATH = "model/har_model.onnx"

BATCH_SIZE = 1
SEQ_LENGTH = 128


# =============================================================================
# Load Checkpoint and Extract Model Parameters
# =============================================================================

print("Loading checkpoint...")
checkpoint = torch.load(MODEL_CHECKPOINT_PATH, map_location="cpu")

# Extract model parameters from checkpoint
if isinstance(checkpoint, dict):
    input_size = checkpoint.get('input_channels', 8)
    num_classes = checkpoint.get('num_classes', 3)
    model_state = checkpoint['model_state_dict']
    val_acc = checkpoint.get('val_acc', None)
    
    print(f"Checkpoint info:")
    print(f"  Input channels: {input_size}")
    print(f"  Number of classes: {num_classes}")
    if val_acc is not None:
        print(f"  Validation accuracy: {val_acc:.2f}%")
else:
    # Old format - just state dict
    print("Warning: Old checkpoint format detected, using default parameters")
    input_size = 8
    num_classes = 3
    model_state = checkpoint

print()


# =============================================================================
# Initialize Model and Load Weights
# =============================================================================

print("Creating model...")
model = HAR_CNN1D(
    input_size=input_size,
    num_classes=num_classes,
    dropout=0.5  # Dropout is ignored during eval mode
)

model.load_state_dict(model_state)
model.eval()

print(f"Model: HAR_CNN1D(input_size={input_size}, num_classes={num_classes})")
print()


# =============================================================================
# Create Dummy Input
# =============================================================================

# Shape: (batch, seq_length, features)
# - batch: 1 sample at a time
# - seq_length: 128 time steps (window size)
# - features: 8 (acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq)
dummy_input = torch.randn(BATCH_SIZE, SEQ_LENGTH, input_size)

print(f"Dummy input shape: {dummy_input.shape}")


# =============================================================================
# Test PyTorch Model
# =============================================================================

print("\nTesting PyTorch model...")
with torch.no_grad():
    pytorch_output = model(dummy_input)
    pytorch_probs = torch.softmax(pytorch_output, dim=1)
    
print(f"PyTorch output shape: {pytorch_output.shape}")
print(f"PyTorch probabilities: {pytorch_probs.numpy()}")


# =============================================================================
# Export to ONNX
# =============================================================================

print(f"\nExporting to ONNX...")
torch.onnx.export(
    model,
    dummy_input,
    OUTPUT_ONNX_PATH,
    input_names=["input"],
    output_names=["output"],
    dynamic_axes={
        "input": {0: "batch"},   # Allow dynamic batch size
        "output": {0: "batch"}
    },
    opset_version=17,
    do_constant_folding=True,
    export_params=True,
)

print(f"✓ Model exported to {OUTPUT_ONNX_PATH}")


# =============================================================================
# Validate ONNX Model
# =============================================================================

print("\nValidating ONNX model...")
try:
    ort_session = ort.InferenceSession(OUTPUT_ONNX_PATH)
    
    # Get input/output info
    input_info = ort_session.get_inputs()[0]
    output_info = ort_session.get_outputs()[0]
    
    print(f"  Input name: {input_info.name}")
    print(f"  Input shape: {input_info.shape}")
    print(f"  Output name: {output_info.name}")
    print(f"  Output shape: {output_info.shape}")
    
    # Run inference
    onnx_output = ort_session.run(
        None,
        {input_info.name: dummy_input.numpy()}
    )[0]
    
    # Compare outputs
    diff = np.abs(pytorch_output.numpy() - onnx_output).max()
    print(f"\n  Max difference between PyTorch and ONNX: {diff:.6f}")
    
    if diff < 1e-5:
        print("  ✓ ONNX model validated successfully!")
    else:
        print(f"  ⚠ Warning: Outputs differ by {diff:.6f}")
        
except Exception as e:
    print(f"  ✗ ONNX validation failed: {e}")

print("\n" + "="*60)
print("Conversion complete!")
print("="*60)
print(f"ONNX model saved to: {OUTPUT_ONNX_PATH}")
print(f"Model architecture: HAR_CNN1D({input_size} → {num_classes})")
print(f"Input shape: (batch, {SEQ_LENGTH}, {input_size})")
print(f"Output shape: (batch, {num_classes})")
print("="*60)
