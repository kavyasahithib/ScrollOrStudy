package com.example.scrollorstudy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

data class RadarData(val label: String, val value: Float, val maxValue: Float)

@Composable
fun RadarChart(
    data: List<RadarData>,
    modifier: Modifier = Modifier,
    polyColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (data.size < 3) return
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (minOf(size.width, size.height) / 2f) * 0.7f // 30% margin for text labels completely inside canvas
            val angleStep = (2 * Math.PI) / data.size

            // Draw concentric grid polygons
            val steps = 4
            for (step in 1..steps) {
                val stepRadius = radius * (step / steps.toFloat())
                val path = Path()
                for (i in data.indices) {
                    val angle = i * angleStep - Math.PI / 2 // Start from top
                    val x = center.x + stepRadius * cos(angle).toFloat()
                    val y = center.y + stepRadius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // Draw axes and labels
            for (i in data.indices) {
                val angle = i * angleStep - Math.PI / 2
                val borderX = center.x + radius * cos(angle).toFloat()
                val borderY = center.y + radius * sin(angle).toFloat()
                drawLine(start = center, end = Offset(borderX, borderY), color = gridColor, strokeWidth = 1.5.dp.toPx())

                val labelDistance = radius * 1.25f // Scale text distance relative to size
                val labelX = center.x + labelDistance * cos(angle).toFloat()
                val labelY = center.y + labelDistance * sin(angle).toFloat()
                
                val textLayoutResult = textMeasurer.measure(
                    text = data[i].label, 
                    style = TextStyle(color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(labelX - textLayoutResult.size.width / 2f, labelY - textLayoutResult.size.height / 2f)
                )
            }

            // Draw dynamic data polygon
            val dataPath = Path()
            for (i in data.indices) {
                val angle = i * angleStep - Math.PI / 2
                val normalizedValue = (data[i].value / data[i].maxValue).coerceIn(0f, 1f)
                val dataRadius = radius * normalizedValue
                val x = center.x + dataRadius * cos(angle).toFloat()
                val y = center.y + dataRadius * sin(angle).toFloat()
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            
            drawPath(dataPath, color = polyColor.copy(alpha = 0.45f), style = Fill)
            drawPath(dataPath, color = polyColor, style = Stroke(width = 3.dp.toPx()))
            
            // Draw points at intersections
            for (i in data.indices) {
                val angle = i * angleStep - Math.PI / 2
                val normalizedValue = (data[i].value / data[i].maxValue).coerceIn(0f, 1f)
                val dataRadius = radius * normalizedValue
                val x = center.x + dataRadius * cos(angle).toFloat()
                val y = center.y + dataRadius * sin(angle).toFloat()
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(color = polyColor, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
