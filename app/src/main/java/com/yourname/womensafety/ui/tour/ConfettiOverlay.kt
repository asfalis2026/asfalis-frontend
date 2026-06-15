package com.yourname.womensafety.ui.tour

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val startX: Float,   // 0f–1f relative to screen width
    val startY: Float,   // 0f–1f relative to screen height (starting near top)
    val angle: Float,    // degrees — spread direction
    val speed: Float,    // 0.3–1.0 relative
    val color: Color,
    val radius: Float,   // particle size px
    val phase: Float     // 0f–1f offset so particles don't all start at same position
)

/**
 * Full-screen confetti particle system shown on the Celebration step.
 * Particles fall from the top third of the screen with a random spread.
 * The animation loops infinitely so the screen feels alive.
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val palette = listOf(
        Color(0xFFE25F71), // rose
        Color(0xFFFFD700), // gold
        Color(0xFFFFFFFF), // white
        Color(0xFF9C8FFF), // purple
        Color(0xFF4FC3F7), // cyan
        Color(0xFFFF8A65), // coral
    )

    val particles = remember {
        List(65) {
            ConfettiParticle(
                startX    = Random.nextFloat(),
                startY    = Random.nextFloat() * 0.35f,
                angle     = Random.nextFloat() * 360f,
                speed     = 0.25f + Random.nextFloat() * 0.75f,
                color     = palette.random(),
                radius    = 5f + Random.nextFloat() * 9f,
                phase     = Random.nextFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti_anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            // Offset the progress by each particle's phase so they don't all move together
            val t = ((progress + p.phase) % 1f)

            // Horizontal drift using angle
            val driftX = cos(Math.toRadians(p.angle.toDouble())).toFloat() * t * p.speed * 0.25f
            val x = (p.startX + driftX).coerceIn(0f, 1f) * size.width

            // Fall downward
            val y = (p.startY + t * p.speed) * size.height

            // Fade out as the particle reaches the bottom
            val alpha = (1f - t).coerceIn(0f, 1f)

            if (y <= size.height) {
                drawCircle(
                    color  = p.color.copy(alpha = alpha),
                    radius = p.radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
