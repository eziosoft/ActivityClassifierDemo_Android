package com.example.activityclassifierdemo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme

@Composable
fun ModeToggle(isInferenceMode: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isInferenceMode) "Inference Mode" else "Training Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isInferenceMode) "Detecting activity in real-time" else "Recording labeled training data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Checkbox(checked = isInferenceMode, onCheckedChange = onToggle)
        }
    }
}

@Preview
@Composable
private fun ModeTogglePreview() {
    ActivityClassifierDemoTheme {
        ModeToggle(isInferenceMode = false, onToggle = {})
    }
}

@Preview
@Composable
private fun ModeToggleInferenceModePreview() {
    ActivityClassifierDemoTheme {
        ModeToggle(isInferenceMode = true, onToggle = {})
    }
}

