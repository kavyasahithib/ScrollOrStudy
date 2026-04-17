package com.example.scrollorstudy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class BackgroundStyle {
    MESH, ORBS
}

/**
 * A configurable premium animated background.
 * Combines the deeply blurred gradient mesh and soft floating orbs into one component.
 */
@Composable
fun ConfigurableBackground(
    modifier: Modifier = Modifier,
    style: BackgroundStyle = BackgroundStyle.MESH
) {
    val infiniteTransition = rememberInfiniteTransition(label = "config_bg")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B0F1A), Color(0xFF0A1F3C))
                )
            )
    ) {
        if (style == BackgroundStyle.MESH) {
            MeshContent(infiniteTransition)
        } else {
            OrbsContent(infiniteTransition)
        }
    }
}

@Composable
private fun MeshContent(infiniteTransition: InfiniteTransition) {
    val blob1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1Progress"
    )

    val blob2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2Progress"
    )

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    val particles = remember {
        List(15) {
            ParticleData(
                xSeed = Random.nextFloat(),
                ySeed = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                speedX = Random.nextFloat() * 0.05f + 0.02f,
                speedY = Random.nextFloat() * 0.05f + 0.02f,
                color = if (Random.nextBoolean()) Color(0xFF5B8CFF) else Color(0xFF00E5FF),
                alpha = Random.nextFloat() * 0.2f + 0.1f
            )
        }
    }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .blur(120.dp)
    ) {
        val blob1X = 0.2f + 0.15f * sin(blob1Progress * 2 * Math.PI.toFloat())
        val blob1Y = 0.3f + 0.1f * cos(blob1Progress * 2 * Math.PI.toFloat() * 0.7f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width * blob1X, size.height * blob1Y),
                radius = size.width * 0.8f
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width * blob1X, size.height * blob1Y)
        )

        val blob2X = 0.8f + 0.1f * cos(blob2Progress * 2 * Math.PI.toFloat() * 0.8f)
        val blob2Y = 0.7f + 0.15f * sin(blob2Progress * 2 * Math.PI.toFloat())

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E88E5).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * blob2X, size.height * blob2Y),
                radius = size.width * 0.7f
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * blob2X, size.height * blob2Y)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val offsetX = sin((time + p.xSeed) * 2 * Math.PI.toFloat()) * 30f
            val offsetY = cos((time + p.ySeed) * 2 * Math.PI.toFloat()) * 30f
            
            val center = Offset(
                (p.xSeed * size.width + offsetX).coerceIn(0f, size.width),
                (p.ySeed * size.height + offsetY).coerceIn(0f, size.height)
            )

            drawCircle(color = p.color.copy(alpha = p.alpha), radius = p.size, center = center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(p.color.copy(alpha = p.alpha * 0.5f), Color.Transparent),
                    center = center,
                    radius = p.size * 3f
                ),
                radius = p.size * 3f,
                center = center
            )
        }
    }
}

@Composable
private fun OrbsContent(infiniteTransition: InfiniteTransition) {
    val orb1Offset by infiniteTransition.animateValue(
        initialValue = Offset(0.2f, 0.3f), targetValue = Offset(0.3f, 0.4f), typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(animation = tween(25000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "orb1"
    )
    val orb2Offset by infiniteTransition.animateValue(
        initialValue = Offset(0.8f, 0.2f), targetValue = Offset(0.7f, 0.3f), typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(animation = tween(30000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "orb2"
    )
    val orb3Offset by infiniteTransition.animateValue(
        initialValue = Offset(0.5f, 0.7f), targetValue = Offset(0.4f, 0.6f), typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(animation = tween(28000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "orb3"
    )
    val orb4Offset by infiniteTransition.animateValue(
        initialValue = Offset(0.1f, 0.85f), targetValue = Offset(0.2f, 0.75f), typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(animation = tween(22000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "orb4"
    )
    val orb5Offset by infiniteTransition.animateValue(
        initialValue = Offset(0.9f, 0.8f), targetValue = Offset(0.8f, 0.9f), typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(animation = tween(26000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "orb5"
    )

    Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
        drawOrb(Color(0xFF1E88E5), 0.65f, orb1Offset, 0.45f)
        drawOrb(Color(0xFF42A5F5), 0.35f, orb1Offset, 0.2f)
        drawOrb(Color(0xFF7C3AED), 0.5f, orb2Offset, 0.4f)
        drawOrb(Color(0xFFB388FF), 0.25f, orb2Offset, 0.15f)
        drawOrb(Color(0xFF00E5FF), 0.4f, orb3Offset, 0.35f)
        drawOrb(Color(0xFFE0F7FA), 0.2f, orb3Offset, 0.1f)
        drawOrb(Color(0xFF9C27B0), 0.35f, orb4Offset, 0.3f)
        drawOrb(Color(0xFF2196F3), 0.45f, orb5Offset, 0.25f)
    }
}

private fun DrawScope.drawOrb(color: Color, radiusScale: Float, offset: Offset, alpha: Float) {
    val radius = size.width * radiusScale
    val center = Offset(size.width * offset.x, size.height * offset.y)
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = radius),
        radius = radius, center = center
    )
}

private data class ParticleData(
    val xSeed: Float, val ySeed: Float, val size: Float, val speedX: Float, val speedY: Float, val color: Color, val alpha: Float
)
