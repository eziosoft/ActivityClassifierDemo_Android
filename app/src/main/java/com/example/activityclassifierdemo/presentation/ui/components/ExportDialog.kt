package com.example.activityclassifierdemo.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.activityclassifierdemo.presentation.theme.ActivityClassifierDemoTheme

@Composable
fun ExportDialog(csvData: String, onDismiss: () -> Unit, onShare: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Training Data") },
        text = {
            Column {
                Text("CSV data is ready to export.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${csvData.lines().size - 1} samples ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onShare) { Text("Share") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview
@Composable
private fun ExportDialogPreview() {
    ActivityClassifierDemoTheme {
        ExportDialog(
            csvData = "header1,header2,header3\n" +
                    "1.0,2.0,3.0\n" +
                    "1.5,2.5,3.5\n" +
                    "1.2,2.2,3.2\n" +
                    "1.8,2.8,3.8",
            onDismiss = {},
            onShare = {}
        )
    }
}

