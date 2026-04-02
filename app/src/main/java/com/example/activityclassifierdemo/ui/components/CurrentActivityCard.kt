package com.example.activityclassifierdemo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.ui.Labels
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme

@Composable
fun CurrentActivityCard(currentActivity: InferenceResult?, movementLabels: List<Labels>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentActivity != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (currentActivity != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentActivity.activityName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${(currentActivity.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                if (currentActivity.probabilities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    movementLabels.forEachIndexed { index, labelData ->
                        if (index < currentActivity.probabilities.size) {
                            val prob = currentActivity.probabilities[index]
                            val isActive = index == currentActivity.activityId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = labelData.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(70.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { prob },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${(prob * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(35.dp),
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (index < movementLabels.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Waiting for data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun CurrentActivityCardWithActivityPreview() {
    ActivityClassifierDemoTheme {
        CurrentActivityCard(
            currentActivity = InferenceResult(
                activityId = 0,
                activityName = "Standing",
                confidence = 0.92f,
                probabilities = floatArrayOf(0.92f, 0.05f, 0.03f)
            ),
            movementLabels = listOf(
                Labels("STANDING", 0),
                Labels("WALKING", 1),
                Labels("JUMPING", 2)
            )
        )
    }
}

@Preview
@Composable
private fun CurrentActivityCardEmptyPreview() {
    ActivityClassifierDemoTheme {
        CurrentActivityCard(
            currentActivity = null,
            movementLabels = listOf(
                Labels("STANDING", 0),
                Labels("WALKING", 1),
                Labels("JUMPING", 2)
            )
        )
    }
}


