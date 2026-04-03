package com.example.activityclassifierdemo.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.activityclassifierdemo.presentation.theme.ActivityClassifierDemoTheme

@Composable
fun BottomModeNavigation(
    isInferenceMode: Boolean,
    onModeChanged: (Boolean) -> Unit
) {
    NavigationBar(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        NavigationBarItem(
            label = { Text("Inference") },
            selected = isInferenceMode,
            onClick = { onModeChanged(true) },
            icon = { Text("🔮") },
            alwaysShowLabel = true,
            modifier = Modifier
        )
        NavigationBarItem(
            label = { Text("Recorder") },
            selected = !isInferenceMode,
            onClick = { onModeChanged(false) },
            icon = { Text("📝") },
            alwaysShowLabel = true,
            modifier = Modifier
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
