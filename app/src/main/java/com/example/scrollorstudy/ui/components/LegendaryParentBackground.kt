package com.example.scrollorstudy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LegendaryParentBackground(modifier: Modifier = Modifier) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Clean, professional ambient colors
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC) // Slate 900 or Slate 50
    val color1 = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFDBEAFE).copy(alpha = 0.8f) // Blue 900/100
    val color2 = if (isDark) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFE0E7FF).copy(alpha = 0.8f) // Indigo 900/100

    val infiniteTransition = rememberInfiniteTransition(label = "neat_bg")
    
    // Very gentle drifting animations
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "drift1"
    )
    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing)),
        label = "drift2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Solid clean foundation
        drawRect(color = bgColor)
        
        val width = size.width
        val height = size.height
        // Enormous radius so the gradient is extremely soft and covers the screen smoothly
        val radius = maxOf(width, height) * 0.85f 

        // Orb 1 drifting gracefully
        val x1 = width / 2f + cos(Math.toRadians(angle1.toDouble())).toFloat() * (width * 0.25f)
        val y1 = height / 3f + sin(Math.toRadians(angle1.toDouble())).toFloat() * (height * 0.15f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = Offset(x1, y1),
                radius = radius
            ),
            radius = radius,
            center = Offset(x1, y1)
        )

        // Orb 2 drifting gracefully
        val x2 = width / 2f + cos(Math.toRadians(angle2.toDouble())).toFloat() * (width * 0.3f)
        val y2 = height * 0.7f + sin(Math.toRadians(angle2.toDouble())).toFloat() * (height * 0.2f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = Offset(x2, y2),
                radius = radius
            ),
            radius = radius,
            center = Offset(x2, y2)
        )
    }
}
