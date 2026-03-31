"""
HAR CNN1D Model Definition

1D CNN model for Human Activity Recognition using accelerometer and gyroscope data.
Input: 8 channels (acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq)
Output: 3 classes (STANDING, WALKING, JUMPING)
"""

import torch
import torch.nn as nn


class HAR_CNN1D(nn.Module):
    def __init__(self, input_size=8, num_classes=3, dropout=0.5):
        super(HAR_CNN1D, self).__init__()

        self.conv_blocks = nn.Sequential(
            nn.Conv1d(in_channels=input_size, out_channels=64, kernel_size=7, padding=3),
            nn.BatchNorm1d(64),
            nn.ReLU(),
            nn.Dropout(dropout),

            nn.Conv1d(in_channels=64, out_channels=128, kernel_size=5, padding=2),
            nn.BatchNorm1d(128),
            nn.ReLU(),
            nn.Dropout(dropout),

            nn.Conv1d(in_channels=128, out_channels=256, kernel_size=3, padding=1),
            nn.BatchNorm1d(256),
            nn.ReLU(),
            nn.Dropout(dropout),
        )

        self.gap = nn.AdaptiveAvgPool1d(1)

        self.fc = nn.Sequential(
            nn.Linear(256, 128),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(128, num_classes)
        )

    def forward(self, x):
        x = x.permute(0, 2, 1)
        x = self.conv_blocks(x)
        x = self.gap(x)
        x = x.squeeze(-1)
        return self.fc(x)
