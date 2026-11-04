package com.jarves.mh.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    val xRatio: Float,
    val startYRatio: Float,
    val speedY: Float,
    val speedX: Float,
    val sizePx: Float,
    val color: Color,
    val rotationSpeed: Float,
    val isCircle: Boolean,
)

/**
 * Joyful confetti particle celebration animation.
 * Emits colorful flakes fluttering across the screen when tasks complete successfully.
 */
@Composable
fun ConfettiCelebration(
    triggerKey: Any?,
    modifier: Modifier = Modifier,
    durationMillis: Int = 2800,
    onFinished: () -> Unit = {},
) {
    if (triggerKey == null) return

    val particles = remember(triggerKey) {
        val colors = listOf(
            Color(0xFF2EA043), // PocketGreen
            Color(0xFFFF9800), // PocketOrange
            Color(0xFF58A6FF), // Cyan / Blue
            Color(0xFFA371F7), // Purple
            Color(0xFFFF7B72), // Coral
            Color(0xFFFFD700), // Gold
            Color(0xFF388BFD), // Sapphire
        )
        val rand = Random(triggerKey.hashCode().toLong())
        List(60) {
            ConfettiParticle(
                xRatio = rand.nextFloat(),
                startYRatio = -0.1f - rand.nextFloat() * 0.3f,
                speedY = 0.8f + rand.nextFloat() * 0.8f,
                speedX = (rand.nextFloat() - 0.5f) * 0.35f,
                sizePx = 10f + rand.nextFloat() * 14f,
                color = colors[rand.nextInt(colors.size)],
                rotationSpeed = (rand.nextFloat() - 0.5f) * 720f,
                isCircle = rand.nextBoolean(),
            )
        }
    }

    val progress = remember(triggerKey) { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
        )
        onFinished()
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = progress.value
            val alpha = (1f - (t - 0.7f).coerceAtLeast(0f) / 0.3f).coerceIn(0f, 1f)

            particles.forEach { p ->
                val x = (p.xRatio + p.speedX * t) * size.width
                val y = (p.startYRatio + p.speedY * t) * size.height
                val rotation = p.rotationSpeed * t

                if (y in -50f..size.height + 50f) {
                    rotate(rotation, pivot = Offset(x, y)) {
                        if (p.isCircle) {
                            drawCircle(
                                color = p.color.copy(alpha = alpha),
                                radius = p.sizePx * 0.45f,
                                center = Offset(x, y),
                            )
                        } else {
                            drawRect(
                                color = p.color.copy(alpha = alpha),
                                topLeft = Offset(x - p.sizePx * 0.5f, y - p.sizePx * 0.25f),
                                size = Size(p.sizePx, p.sizePx * 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }
}
