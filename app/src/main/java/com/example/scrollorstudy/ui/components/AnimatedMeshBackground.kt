package com.example.scrollorstudy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AnimatedMeshBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_bg")
    
    // Animate coordinates to move the glowing orbs around the screen
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0.1f, 
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, 
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2"
    )

    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top-left slowly moving orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary, Color.Transparent),
                center = Offset(w * offset1, h * offset2),
                radius = w * 0.9f
            ),
            radius = w * 0.9f,
            center = Offset(w * offset1, h * offset2)
        )

        // Bottom-right slowly moving orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary, Color.Transparent),
                center = Offset(w * (1 - offset2), h * (1 - offset1)),
                radius = w
            ),
            radius = w,
            center = Offset(w * (1 - offset2), h * (1 - offset1))
        )
    }
}
