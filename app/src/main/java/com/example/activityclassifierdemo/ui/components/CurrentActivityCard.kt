package com.example.activityclassifierdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.ui.Labels
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme
import com.example.activityclassifierdemo.ui.theme.ActivityJumping
import com.example.activityclassifierdemo.ui.theme.ActivityStanding
import com.example.activityclassifierdemo.ui.theme.ActivityWalking

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
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (currentActivity != null) {
                // Activity name and confidence - main focus
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = getActivityColor(currentActivity.activityId).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentActivity.activityName,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = getActivityColor(currentActivity.activityId),
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(currentActivity.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = getActivityColor(currentActivity.activityId),
                                fontSize = 44.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confidence",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (currentActivity.probabilities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activity Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Text(
                            text = "⚡ ${currentActivity.inferenceTimeMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    movementLabels.forEachIndexed { index, labelData ->
                        if (index < currentActivity.probabilities.size) {
                            val prob = currentActivity.probabilities[index]
                            val isActive = index == currentActivity.activityId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = labelData.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) getActivityColor(index) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.width(80.dp)
                                )

                                LinearProgressIndicator(
                                    progress = { prob },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = getActivityColor(index),
                                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                )

                                Text(
                                    text = "${(prob * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(40.dp),
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isActive) getActivityColor(index) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Waiting for data...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getActivityColor(activityId: Int): Color {
    return when (activityId) {
        0 -> ActivityStanding
        1 -> ActivityWalking
        2 -> ActivityJumping
        else -> MaterialTheme.colorScheme.primary
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
