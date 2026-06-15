package com.yourname.womensafety.ui.tour

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Draws a full-screen dark scrim with an animated transparent "spotlight" cutout
 * punched at [targetRect] using [BlendMode.Clear].
 *
 * The [graphicsLayer] with [CompositingStrategy.Offscreen] is required for
 * BlendMode.Clear to work correctly on all API levels in Compose.
 *
 * Animates smoothly (400 ms) whenever [targetRect] changes between steps.
 * A pulsing rose-pink glow ring is drawn around the cutout edge.
 */
@Composable
fun SpotlightCanvas(
    targetRect: Rect?,
    spotlightShape: SpotlightShape,
    modifier: Modifier = Modifier
) {
    // ── Glow pulse ─────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation   = tween(1200, easing = FastOutSlowInEasing),
            repeatMode  = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // ── Animate bounds between steps ───────────────────────────────────────
    val spec       = tween<Float>(400, easing = FastOutSlowInEasing)
    val animLeft   by animateFloatAsState(targetRect?.left   ?: 0f, animationSpec = spec, label = "spl")
    val animTop    by animateFloatAsState(targetRect?.top    ?: 0f, animationSpec = spec, label = "spt")
    val animRight  by animateFloatAsState(targetRect?.right  ?: 0f, animationSpec = spec, label = "spr")
    val animBottom by animateFloatAsState(targetRect?.bottom ?: 0f, animationSpec = spec, label = "spb")

    val animRect = if (targetRect != null) Rect(animLeft, animTop, animRight, animBottom) else null

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // Offscreen compositing layer is REQUIRED for BlendMode.Clear to work
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        // 1. Dark scrim
        drawRect(color = Color.Black.copy(alpha = 0.76f))

        if (animRect != null && spotlightShape != SpotlightShape.NONE) {
            val pad         = 18.dp.toPx()
            val paddedRect  = animRect.inflate(pad)

            // 2. Build the cutout path
            val holePath = Path().apply {
                when (spotlightShape) {
                    SpotlightShape.CIRCLE -> {
                        val cx     = paddedRect.center.x
                        val cy     = paddedRect.center.y
                        val radius = maxOf(paddedRect.width, paddedRect.height) / 2f
                        addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
                    }
                    SpotlightShape.ROUNDED_RECT -> {
                        addRoundRect(RoundRect(paddedRect, CornerRadius(28.dp.toPx())))
                    }
                    SpotlightShape.FULL_WIDTH_BAR -> {
                        addRoundRect(
                            RoundRect(
                                rect         = Rect(Offset(0f, paddedRect.top), Size(size.width, paddedRect.height)),
                                cornerRadius = CornerRadius(14.dp.toPx())
                            )
                        )
                    }
                    SpotlightShape.NONE -> { /* nothing */ }
                }
            }

            // 3. Punch the hole — BlendMode.Clear erases to transparent
            drawPath(
                path      = holePath,
                color     = Color.Transparent,
                blendMode = BlendMode.Clear
            )

            // 4. Soft outer glow ring
            drawPath(
                path  = holePath,
                color = Color(0xFFE25F71).copy(alpha = glowAlpha * 0.20f),
                style = Stroke(width = 18.dp.toPx())
            )

            // 5. Tight accent ring
            drawPath(
                path  = holePath,
                color = Color(0xFFE25F71).copy(alpha = glowAlpha * 0.85f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
