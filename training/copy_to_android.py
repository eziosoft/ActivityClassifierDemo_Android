"""
Copy Model Files to Android Assets

This script copies the trained ONNX model and normalization parameters
to the Android project's assets folder.
"""

import os
import shutil
from pathlib import Path

# =============================================================================
# Configuration
# =============================================================================

# Source paths (relative to training folder)
ONNX_MODEL_PATH = "model/har_model.onnx"
ONNX_MODEL_DATA_PATH = "model/har_model.onnx.data"
NORMALIZATION_PARAMS_PATH = "processed_data/normalization_params.json"

# Destination path (Android assets folder)
ANDROID_ASSETS_DIR = "../app/src/main/assets"

# =============================================================================
# Copy Files
# =============================================================================

def copy_file(source, dest_dir, dest_name=None):
    """Copy a file to destination directory."""
    source_path = Path(source)
    dest_dir_path = Path(dest_dir)
    
    # Create destination directory if it doesn't exist
    dest_dir_path.mkdir(parents=True, exist_ok=True)
    
    # Determine destination file name
    if dest_name is None:
        dest_name = source_path.name
    
    dest_path = dest_dir_path / dest_name
    
    # Check if source exists
    if not source_path.exists():
        print(f"✗ Source file not found: {source}")
        return False
    
    # Copy the file
    try:
        shutil.copy2(source, dest_path)
        file_size = dest_path.stat().st_size
        print(f"✓ Copied {source_path.name}")
        print(f"  From: {source_path.absolute()}")
        print(f"  To:   {dest_path.absolute()}")
        print(f"  Size: {file_size:,} bytes ({file_size / 1024:.2f} KB)")
        return True
    except Exception as e:
        print(f"✗ Failed to copy {source}: {e}")
        return False

print("=" * 70)
print("Copying Model Files to Android Assets")
print("=" * 70)
print()

# Check if Android assets directory exists
assets_path = Path(ANDROID_ASSETS_DIR)
if not assets_path.parent.exists():
    print(f"✗ Android project directory not found: {assets_path.parent.absolute()}")
    print("  Make sure you're running this script from the training folder")
    exit(1)

success_count = 0
total_count = 2

# Copy ONNX model
print("1. ONNX Model:")
if copy_file(ONNX_MODEL_PATH, ANDROID_ASSETS_DIR, "har_model.onnx"):
    success_count += 1
print()

# Copy normalization parameters
print("2. Normalization Parameters:")
if copy_file(NORMALIZATION_PARAMS_PATH, ANDROID_ASSETS_DIR, "normalization_params.json"):
    success_count += 1
print()

# Summary
print("=" * 70)
print(f"Summary: {success_count}/{total_count} files copied successfully")
print("=" * 70)

if success_count == total_count:
    print("✓ All files copied to Android assets folder!")
    print(f"  Location: {assets_path.absolute()}")
    print()
    print("Files copied:")
    print("  ✓ har_model.onnx (self-contained ONNX model)")
    print("  ✓ normalization_params.json (z-score normalization)")
    print()
    print("Note: har_model.onnx.data is not needed (model is self-contained)")
    print()
    print("You can now use these files in your Android app for inference.")
else:
    print(f"⚠ Warning: Only {success_count}/{total_count} files were copied")
    print("  Please check the error messages above")
    exit(1)

