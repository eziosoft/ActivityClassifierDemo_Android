package com.example.activityclassifierdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme

@Composable
fun ChartLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ChartLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        LegendItem(color = Color.Red, label = "X")
        LegendItem(color = Color.Green, label = "Y")
        LegendItem(color = Color.Blue, label = "Z")
        LegendItem(color = Color(0xFFFF6F00), label = "Acc mag")
        LegendItem(color = Color(0xFF7B1FA2), label = "Gyro mag")
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun ChartLabelPreview() {
    ActivityClassifierDemoTheme {
        ChartLabel(text = "Linear Acceleration (m/s²)")
    }
}

@Preview
@Composable
private fun ChartLegendPreview() {
    ActivityClassifierDemoTheme {
        ChartLegend()
    }
}

@Preview
@Composable
private fun LegendItemPreview() {
    ActivityClassifierDemoTheme {
        LegendItem(color = Color.Red, label = "X")
    }
}

