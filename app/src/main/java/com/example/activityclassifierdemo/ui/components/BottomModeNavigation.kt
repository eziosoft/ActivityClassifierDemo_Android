package com.example.activityclassifierdemo.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme

@Composable
fun BottomModeNavigation(
    isInferenceMode: Boolean,
    onModeChanged: (Boolean) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            label = { Text("Inference") },
            selected = isInferenceMode,
            onClick = { onModeChanged(true) },
            icon = { Text("🔮") }
        )
        NavigationBarItem(
            label = { Text("Training") },
            selected = !isInferenceMode,
            onClick = { onModeChanged(false) },
            icon = { Text("📝") }
        )
    }
}

@Preview
@Composable
private fun BottomModeNavigationTrainingPreview() {
    ActivityClassifierDemoTheme {
        BottomModeNavigation(isInferenceMode = false, onModeChanged = {})
    }
}

@Preview
@Composable
private fun BottomModeNavigationInferencePreview() {
    ActivityClassifierDemoTheme {
        BottomModeNavigation(isInferenceMode = true, onModeChanged = {})
    }
}





