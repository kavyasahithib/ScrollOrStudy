package com.example.scrollorstudy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.*

@Composable
fun FocusOrb3D(modifier: Modifier = Modifier) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val orbPrimary = if (isDark) Color(0xFF6366F1) else Color(0xFFF43F5E)
    val orbSecondary = if (isDark) Color(0xFF2DD4BF) else Color(0xFFF59E0B)

    val infiniteTransition = rememberInfiniteTransition(label = "3d_orb")
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "rotY"
    )
    val rotationX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)),
        label = "rotX"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000 // Total 2 second duration loop
                1.0f at 0 with FastOutSlowInEasing
                1.15f at 300 with FastOutSlowInEasing // Rapid expansion bounce
                1.0f at 600 with FastOutSlowInEasing // Drop back to core
                1.0f at 2000 // Stay still for the rest of the 2-second interval
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce"
    )

    Canvas(modifier = modifier) {
        val baseRadius = size.minDimension / 2 * 0.75f
        val radius = baseRadius * scale // Apply the bounce scale!
        val centerX = size.width / 2
        val centerY = size.height / 2
        val perspective = radius * 3f 

        val numBands = 16
        val pointsPerBand = 24

        val radY = Math.toRadians(rotationY.toDouble())
        val radX = Math.toRadians(rotationX.toDouble())
        val cosY = cos(radY); val sinY = sin(radY)
        val cosX = cos(radX); val sinX = sin(radX)

        for (i in 0..numBands) {
            val phi = Math.PI * i / numBands
            val yNode = radius * cos(phi)
            val ringRadius = radius * sin(phi)
            
            for (j in 0 until pointsPerBand) {
                val theta = 2 * Math.PI * j / pointsPerBand
                val xNode = ringRadius * cos(theta)
                val zNode = ringRadius * sin(theta)

                // 3D Rotations
                val yRotX = yNode * cosX - zNode * sinX
                val zRotX = yNode * sinX + zNode * cosX
                val xRotY = xNode * cosY + zRotX * sinY
                val zRotY = -xNode * sinY + zRotX * cosY

                // Orthographic Projection to 2D
                val scaleProjected = perspective / (perspective - zRotY).coerceAtLeast(1.0)
                val x2D = centerX + xRotY * scaleProjected
                val y2D = centerY + yRotX * scaleProjected

                // Depth-based Opacity and Scaling
                val depthAlpha = ((zRotY + radius) / (2 * radius)).coerceIn(0.1, 1.0).toFloat()
                val pointSize = max(1f, 5f * scaleProjected.toFloat() * depthAlpha)

                // Draw Primary Node
                drawCircle(
                    color = orbPrimary.copy(alpha = depthAlpha * 0.85f),
                    radius = pointSize,
                    center = Offset(x2D.toFloat(), y2D.toFloat())
                )
                
                // Draw Secondary Highlight Core
                drawCircle(
                    color = orbSecondary.copy(alpha = depthAlpha * 0.95f),
                    radius = pointSize * 0.4f,
                    center = Offset(x2D.toFloat() + 1f, y2D.toFloat() - 1f)
                )
            }
        }
    }
}
