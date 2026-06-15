package com.yourname.womensafety.ui.tour

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.womensafety.utils.tr

/**
 * Which edge of the tooltip card the arrow is on (= where the target is relative to the card).
 *
 * TOWARD_TOP    → target is above  the card → arrow at top edge, tip points UP
 * TOWARD_BOTTOM → target is below  the card → arrow at bottom edge, tip points DOWN
 * TOWARD_LEFT   → target is left   of the card → arrow at left edge, tip points LEFT
 * TOWARD_RIGHT  → target is right  of the card → arrow at right edge, tip points RIGHT
 * NONE          → no arrow (welcome / celebration / full-screen intro steps)
 */
enum class ArrowDirection { TOWARD_TOP, TOWARD_BOTTOM, TOWARD_LEFT, TOWARD_RIGHT, NONE }

/**
 * Premium, compact coach-mark tooltip card.
 *
 * Width is controlled by the wrapper Box in TourOverlay (252 dp). This composable
 * fills that width via [fillMaxWidth] — it has NO positioning modifiers of its own.
 *
 * Design:
 *  - 93% opaque dark glass background
 *  - Arrow drawn in the same Canvas pass as the bubble (drawBehind)
 *  - Animated progress bar (rose-pink gradient fill)
 *  - Scale + fade transition between steps (premium feel)
 *  - All strings run through .tr() so language changes recompose automatically
 */
@Composable
fun TourTooltipBubble(
    stepIndex: Int,
    totalSteps: Int,
    isWelcome: Boolean,
    isCelebration: Boolean,
    arrowDirection: ArrowDirection,
    arrowOffsetFraction: Float = 0.5f,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onGoToHelp: () -> Unit = {}
) {
    if (stepIndex !in 0 until totalSteps) return

    // AnimatedContent is the outermost composable.
    // It inherits the 252 dp width constraint from TourOverlay's wrapper Box
    // and propagates it to the inner Box via fillMaxWidth().
    AnimatedContent(
        targetState = stepIndex,
        transitionSpec = {
            val enter = scaleIn(
                spring(Spring.DampingRatioMediumBouncy, 420f),
                initialScale = 0.87f
            ) + fadeIn(tween(200))
            val exit = scaleOut(tween(140), targetScale = 0.92f) + fadeOut(tween(120))
            enter togetherWith exit
        },
        label = "tooltip_step"
    ) { idx ->
        val step = TOUR_STEPS.getOrNull(idx) ?: return@AnimatedContent

        // ── Geometry constants ────────────────────────────────────────────────
        val arrowLenDp = if (arrowDirection != ArrowDirection.NONE) 10.dp else 0.dp
        val arrowWidDp = 20.dp
        val cornerRDp  = 18.dp

        val bgColor     = Color(0xFF160D0C).copy(alpha = 0.93f)
        val borderColor = Color(0xFFE25F71).copy(alpha = 0.40f)
        val accentRed   = Color(0xFFE25F71)

        // ── Progress bar animation ────────────────────────────────────────────
        val progress by animateFloatAsState(
            targetValue   = (idx + 1f) / totalSteps,
            animationSpec = tween(450, easing = FastOutSlowInEasing),
            label         = "progress_bar"
        )

        // ── Inner padding: reduced for rectangle (wide, short) shape ──────────
        val padTop    = arrowLenDp + 11.dp
        val padBottom = (if (arrowDirection == ArrowDirection.TOWARD_BOTTOM) arrowLenDp else 0.dp) + 11.dp
        val padStart  = (if (arrowDirection == ArrowDirection.TOWARD_RIGHT)  arrowLenDp else 0.dp) + 14.dp
        val padEnd    = (if (arrowDirection == ArrowDirection.TOWARD_LEFT)   arrowLenDp else 0.dp) + 14.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val aLen = arrowLenDp.toPx()
                    val aWid = arrowWidDp.toPx()
                    val cr   = cornerRDp.toPx()

                    // Bubble insets — leave space for arrow on the matching edge
                    val bLeft   = if (arrowDirection == ArrowDirection.TOWARD_RIGHT) aLen else 0f
                    val bTop    = if (arrowDirection == ArrowDirection.TOWARD_TOP)   aLen else 0f
                    val bRight  = if (arrowDirection == ArrowDirection.TOWARD_LEFT)  size.width - aLen else size.width
                    val bBottom = if (arrowDirection == ArrowDirection.TOWARD_BOTTOM) size.height - aLen else size.height

                    // ── Arrow triangle ─────────────────────────────────────────
                    if (arrowDirection != ArrowDirection.NONE) {
                        val ap = Path()
                        when (arrowDirection) {
                            ArrowDirection.TOWARD_TOP -> {
                                val ax = (size.width * arrowOffsetFraction)
                                    .coerceIn(aWid + cr, size.width - aWid - cr)
                                ap.moveTo(ax, 0f)
                                ap.lineTo(ax - aWid / 2f, aLen + 1f)
                                ap.lineTo(ax + aWid / 2f, aLen + 1f)
                            }
                            ArrowDirection.TOWARD_BOTTOM -> {
                                val ax = (size.width * arrowOffsetFraction)
                                    .coerceIn(aWid + cr, size.width - aWid - cr)
                                ap.moveTo(ax, size.height)
                                ap.lineTo(ax - aWid / 2f, size.height - aLen - 1f)
                                ap.lineTo(ax + aWid / 2f, size.height - aLen - 1f)
                            }
                            ArrowDirection.TOWARD_LEFT -> {
                                val ay = (size.height * arrowOffsetFraction)
                                    .coerceIn(aWid + cr, size.height - aWid - cr)
                                ap.moveTo(0f, ay)
                                ap.lineTo(aLen + 1f, ay - aWid / 2f)
                                ap.lineTo(aLen + 1f, ay + aWid / 2f)
                            }
                            ArrowDirection.TOWARD_RIGHT -> {
                                val ay = (size.height * arrowOffsetFraction)
                                    .coerceIn(aWid + cr, size.height - aWid - cr)
                                ap.moveTo(size.width, ay)
                                ap.lineTo(size.width - aLen - 1f, ay - aWid / 2f)
                                ap.lineTo(size.width - aLen - 1f, ay + aWid / 2f)
                            }
                            ArrowDirection.NONE -> {}
                        }
                        ap.close()
                        drawPath(ap, color = bgColor)
                        drawPath(ap, color = borderColor, style = Stroke(1.dp.toPx()))
                    }

                    // ── Bubble fill ────────────────────────────────────────────
                    drawRoundRect(
                        color        = bgColor,
                        topLeft      = Offset(bLeft, bTop),
                        size         = Size(bRight - bLeft, bBottom - bTop),
                        cornerRadius = CornerRadius(cr)
                    )

                    // ── Bubble border ──────────────────────────────────────────
                    drawRoundRect(
                        color        = borderColor,
                        topLeft      = Offset(bLeft, bTop),
                        size         = Size(bRight - bLeft, bBottom - bTop),
                        cornerRadius = CornerRadius(cr),
                        style        = Stroke(1.2.dp.toPx())
                    )
                }
        ) {
            Column(
                modifier = Modifier.padding(
                    top    = padTop,
                    bottom = padBottom,
                    start  = padStart,
                    end    = padEnd
                )
            ) {

                // ── 1. Emoji + Title ─────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(step.emoji, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = step.title.tr(),
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                        maxLines   = 1,
                        modifier   = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // ── 2. Body ──────────────────────────────────────────────────
                Text(
                    text       = step.body.tr(),
                    color      = Color(0xFFCCCCCC),
                    fontSize   = 12.sp,
                    lineHeight  = 16.sp,
                    maxLines   = 2
                )

                Spacer(Modifier.height(10.dp))

                // ── 3. Progress bar + step counter — translatable ─────────────
                // "Step" and "of" are translated separately so parameterised
                // strings like "Step 4 of 11" work in Hindi/Bengali too.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${ "Step".tr() } ${idx + 1} ${ "of".tr() } $totalSteps",
                        color      = Color.White.copy(0.45f),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(10.dp))
                    // Animated progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF8096), accentRed)
                                    )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── 4. Action buttons ─────────────────────────────────────────
                when {
                    isWelcome -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick  = onSkip,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Skip".tr(), color = Color.White.copy(0.38f), fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick  = onNext,
                                modifier = Modifier.weight(2.2f).height(38.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = accentRed),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "Start Tour".tr(),
                                    color      = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp
                                )
                            }
                        }
                    }

                    isCelebration -> {
                        Column {
                            Button(
                                onClick  = onNext,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = accentRed),
                                shape    = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Get Started 🎉".tr(),
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.5.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick  = onGoToHelp,
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.15f)),
                                shape    = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Go to Help & Support".tr(),
                                    color      = Color.White.copy(0.85f),
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Back button (hidden on step 1)
                            if (idx > 1) {
                                IconButton(
                                    onClick  = onBack,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.07f))
                                        .border(1.dp, Color.White.copy(0.12f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back".tr(),
                                        tint     = Color.White.copy(0.72f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            TextButton(
                                onClick  = onSkip,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Skip".tr(), color = Color.White.copy(0.38f), fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick  = onNext,
                                modifier = Modifier.weight(2.2f).height(38.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = accentRed),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "Next".tr(),
                                    color      = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint     = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
