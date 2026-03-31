"""
Training Visualization Tools

Real-time plotting utilities for training metrics (loss and accuracy).
"""

import matplotlib.pyplot as plt


class TrainingGraph:
    def __init__(self, save_path="training_plots.png"):
        self.save_path = save_path
        self.epochs_list = []
        self.train_loss_list = []
        self.val_loss_list = []
        self.train_acc_list = []
        self.val_acc_list = []

        plt.ion()
        self.fig, (self.ax1, self.ax2) = plt.subplots(1, 2, figsize=(12, 5))

    def update(self, epoch, train_loss, val_loss, train_acc, val_acc):
        self.epochs_list.append(epoch)
        self.train_loss_list.append(train_loss)
        self.val_loss_list.append(val_loss)
        self.train_acc_list.append(train_acc)
        self.val_acc_list.append(val_acc)

        self.ax1.clear()
        self.ax2.clear()

        self.ax1.plot(self.epochs_list, self.train_loss_list, label='Train Loss')
        self.ax1.plot(self.epochs_list, self.val_loss_list, label='Val Loss')
        self.ax1.set_xlabel('Epoch')
        self.ax1.set_ylabel('Loss')
        self.ax1.set_title('Loss')
        self.ax1.legend()
        self.ax1.grid(True)

        self.ax2.plot(self.epochs_list, self.train_acc_list, label='Train Acc')
        self.ax2.plot(self.epochs_list, self.val_acc_list, label='Val Acc')
        self.ax2.set_xlabel('Epoch')
        self.ax2.set_ylabel('Accuracy (%)')
        self.ax2.set_title('Accuracy')
        self.ax2.legend()
        self.ax2.grid(True)

        plt.tight_layout()
        plt.savefig(self.save_path)
        plt.pause(0.01)

    def close(self):
        plt.ioff()
        plt.draw()
        plt.pause(1)