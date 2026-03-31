package com.example.activityclassifierdemo.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Displays a line chart for sensor data with configurable channels.
 *
 * Supports both single-channel and multi-channel plotting. Each channel is defined by
 * an index (which element in each FloatArray to read) and a color.
 *
 * @param data List of FloatArrays where each array contains sensor values
 * @param modifier Composable modifier
 * @param channels List of (index, color) pairs defining what to plot. 
 *                 Defaults to 3-channel plot: (0→Red, 1→Green, 2→Blue)
 *                 For single line: pass listOf(0 to Color.Amber)
 */
@Composable
fun LineChart(
    data: List<FloatArray>,
    modifier: Modifier = Modifier,
    channels: List<Pair<Int, Color>> = listOf(
        0 to Color.Red,
        1 to Color.Green,
        2 to Color.Blue
    )
) {
    if (data.size < 2) {
        Text(
            text = "Waiting for data...",
            modifier = modifier.padding(16.dp)
        )
        return
    }

    val padding = 40f
    val gridColor = Color.LightGray
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
    ) {
        val width = size.width - padding * 2
        val height = size.height - padding * 2

        // Collect all values across all channels to determine min/max for Y axis
        val allYValues = data.flatMap { array ->
            channels.map { (index, _) -> array[index] }
        }
        val minY = allYValues.minOrNull() ?: 0f
        val maxY = allYValues.maxOrNull() ?: 1f
        val yRange = (maxY - minY).coerceAtLeast(0.01f)

        // Draw axes
        drawLine(
            color = gridColor,
            start = Offset(padding, padding),
            end = Offset(padding, size.height - padding),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(padding, size.height - padding),
            end = Offset(size.width - padding, size.height - padding),
            strokeWidth = 1f
        )

        // X and Y coordinate mapping helpers
        val drawX = { index: Int ->
            padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * width
        }

        val drawY = { value: Float ->
            padding + ((maxY - value) / yRange) * height
        }

        // Draw each channel as a separate line
        channels.forEach { (dataIndex, lineColor) ->
            val path = Path().apply {
                data.forEachIndexed { index, array ->
                    val x = drawX(index)
                    val y = drawY(array[dataIndex])
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(path, lineColor, style = Stroke(width = 3f))
        }

        // Draw Y-axis labels
        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.drawText(
            String.format(Locale.US, "%.1f", maxY),
            padding - 5f,
            padding + 10f,
            paint
        )
        drawContext.canvas.nativeCanvas.drawText(
            String.format(Locale.US, "%.1f", minY),
            padding - 5f,
            size.height - padding + 10f,
            paint
        )
    }
}

/**
 * Convenience wrapper for a single-line chart.
 *
 * @param data List of FloatArrays where [valueIndex] holds the scalar value to plot
 * @param modifier Composable modifier
 * @param valueIndex Index within each array to read the value from (default 0)
 * @param lineColor Color of the plotted line
 */
@Composable
fun SingleLineChart(
    data: List<FloatArray>,
    modifier: Modifier = Modifier,
    valueIndex: Int = 0,
    lineColor: Color = Color(0xFFFF6F00) // amber
) {
    LineChart(data, modifier, listOf(valueIndex to lineColor))
}

/**
 * Convenience wrapper for a three-channel chart (XYZ).
 *
 * @param data List of FloatArrays where each array is [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z]
 * @param modifier Composable modifier
 * @param xIndex Index for X axis data (default 0)
 * @param yIndex Index for Y axis data (default 1)
 * @param zIndex Index for Z axis data (default 2)
 */
@Composable
fun SimpleLineChart(
    data: List<FloatArray>,
    modifier: Modifier = Modifier,
    xIndex: Int = 0,
    yIndex: Int = 1,
    zIndex: Int = 2
) {
    LineChart(
        data,
        modifier,
        listOf(
            xIndex to Color.Red,
            yIndex to Color.Green,
            zIndex to Color.Blue
        )
    )
}
