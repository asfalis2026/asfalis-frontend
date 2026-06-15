package com.yourname.womensafety.ui.tour

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Master tour overlay.
 *
 * ## All Bug Fixes Applied
 *
 * ### Fix 1 — Tooltip stuck at top-left
 * Tooltip now has NO positioning modifier of its own. A wrapper [Box] that IS
 * a direct BoxScope child of [BoxWithConstraints] carries all positioning,
 * so [Modifier.offset] and [Modifier.align] work correctly.
 *
 * ### Fix 2 — Wrong coordinates / wrong widget highlighted
 * Navigation happens FIRST. The engine step index advances ONLY after a smart
 * wait: minimum 450 ms (screen render + first compose pass) followed by
 * polling [TourTargetRegistry] every 100 ms until the target bounds appear,
 * or a 1 100 ms hard cap. This guarantees bounds are valid before the overlay
 * re-renders with the new step.
 *
 * ### Fix 3 — Back button highlights wrong screen
 * goBack() reads the previous step's [TourStep.screenRoute], navigates there
 * first (with the same smart wait), THEN calls [TourEngine.back].
 *
 * ### Fix 4 — Laggy / flickering transitions
 * [isTransitioning] collapses the spotlight to nothing and hides the tooltip
 * during cross-screen navigation. The scrim stays up (smooth hand-off). Once
 * bounds are ready, [AnimatedVisibility] fades the tooltip back in and
 * [SpotlightCanvas]'s built-in tween animates the cutout to the new position.
 *
 * ## Positioning Algorithm (pure math, no coroutines, instant)
 * BELOW → ABOVE → RIGHT → LEFT → fallback BELOW.
 * Card width: 252 dp (~14% wider than previous 220 dp).
 */
@Composable
fun TourOverlay(
    engine: TourEngine,
    navController: NavController,
    onTourCompleted: suspend () -> Unit,
) {
    val isActive  by engine.isActive.collectAsState()
    val stepIndex by engine.currentStepIndex.collectAsState()

    if (!isActive) return

    val step          = TOUR_STEPS.getOrNull(stepIndex) ?: return
    val isWelcome     = stepIndex == 0
    val isCelebration = stepIndex == TOUR_STEPS.lastIndex

    val coroutineScope  = rememberCoroutineScope()
    val interactionSrc  = remember { MutableInteractionSource() }

    // ── Transition gate ──────────────────────────────────────────────────────
    // When true: spotlight is hidden, tooltip is invisible, scrim stays up.
    // The new step renders only after bounds are confirmed valid.
    var isTransitioning by remember { mutableStateOf(false) }

    // Read target bounds (null when transitioning → hides spotlight cleanly)
    val targetRect: Rect? = if (!isTransitioning && step.targetTag.isNotEmpty())
        TourTargetRegistry.getBounds(step.targetTag)
    else null

    // Celebration shield pulse
    val infTrans = rememberInfiniteTransition(label = "cel")
    val shieldScale by infTrans.animateFloat(
        1f, 1.12f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "shield"
    )

    // ── Smart wait helper ────────────────────────────────────────────────────
    // Waits for the target widget to register its bounds in TourTargetRegistry.
    // Min 450 ms so the screen + first compose pass always finish.
    // A 3 000 ms hard-cap prevents an infinite hang if a widget never registers
    // (e.g. shown conditionally or clipped off-screen on a particular device).
    suspend fun awaitBounds(tag: String) {
        delay(450L) // Wait for screen transition + first compose pass
        if (tag.isEmpty()) return
        val deadlineMs = System.currentTimeMillis() + 3_000L
        while (TourTargetRegistry.getBounds(tag) == null) {
            if (System.currentTimeMillis() >= deadlineMs) break
            delay(100L)
        }
    }

    // ── Navigation helpers ───────────────────────────────────────────────────

    fun navigateThenRun(route: String, targetTag: String, onReady: () -> Unit) {
        val currentRoute = navController.currentDestination?.route ?: ""
        val needsNav = route.isNotEmpty() && route != currentRoute
        if (needsNav) {
            isTransitioning = true
            coroutineScope.launch {
                // Bug Fix: Clear any stale bounds from a previous visit to this screen.
                // Without this, awaitBounds() finds the old cached rect and returns
                // immediately — the overlay renders before the new screen has laid out,
                // causing the spotlight to land on the wrong position.
                if (targetTag.isNotEmpty()) TourTargetRegistry.unregister(targetTag)
                navController.navigate(route) { launchSingleTop = true }
                awaitBounds(targetTag)
                onReady()
                isTransitioning = false
            }
        } else {
            onReady()
        }
    }

    fun advance() {
        val nextStep = engine.peekNext()

        if (nextStep == null) {
            // Celebration step → complete
            engine.advance()
            coroutineScope.launch {
                onTourCompleted()
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = false }
                    launchSingleTop = true
                }
            }
            return
        }

        navigateThenRun(
            route     = nextStep.navigateTo ?: "",
            targetTag = nextStep.targetTag
        ) {
            val stillActive = engine.advance()
            if (!stillActive) {
                coroutineScope.launch {
                    onTourCompleted()
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    fun goBack() {
        val prevStep = engine.peekPrev() ?: return
        navigateThenRun(
            route     = prevStep.screenRoute,
            targetTag = prevStep.targetTag
        ) {
            engine.back()
        }
    }

    fun skip() {
        engine.skip()
        coroutineScope.launch {
            onTourCompleted()
            navController.navigate("dashboard") {
                popUpTo("dashboard") { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // ── Root fullscreen box — eats all touches ────────────────────────────────
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSrc, indication = null) { /* consume */ }
    ) {

        // 1. Scrim + spotlight (stays up during transition; cutout hides via null targetRect)
        SpotlightCanvas(
            targetRect     = targetRect,
            spotlightShape = if (isTransitioning) SpotlightShape.NONE else step.spotlightShape,
            modifier       = Modifier.fillMaxSize()
        )

        // 2. Celebration extras (only on last step)
        if (isCelebration) {
            ConfettiOverlay(Modifier.fillMaxSize())
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint     = Color(0xFFE25F71),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .size(66.dp)
                    .scale(shieldScale)
            )
        }

        // 3. Smart-positioned tooltip (hidden while transitioning)
        // FIX: fast animation prevents border-loading visual delay
        // scaleIn from 0.97 (barely perceptible scale) + very fast fade
        AnimatedVisibility(
            visible = !isTransitioning,
            enter   = scaleIn(tween(110, easing = FastOutSlowInEasing), 0.97f) + fadeIn(tween(90)),
            exit    = scaleOut(tween(90), 0.98f) + fadeOut(tween(80)),
            modifier = Modifier.fillMaxSize()
        ) {
            // CRITICAL: BoxWithConstraints exposes BoxScope.
            // All positioning modifiers below are on direct BoxScope children → they work.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val screenW = constraints.maxWidth.toFloat()
                val screenH = constraints.maxHeight.toFloat()

        // Constants (px)
                val SPOT_PAD  = with(density) { 14.dp.toPx() }   // spotlight inflation
                val GAP       = with(density) { 10.dp.toPx() }   // spotlight edge → tooltip
                val SAFE      = with(density) { 16.dp.toPx() }   // screen-edge clearance
                // Full-width card: fills screen minus safe margins on both sides
                // giving a wide, rectangle shape (much wider than it is tall)
                val TIP_W     = screenW - 2f * SAFE
                val TIP_H_EST = with(density) { 130.dp.toPx() }  // shorter = rectangle shape
                val ARROW_H   = with(density) { 10.dp.toPx() }   // arrow triangle height
                val tipWDp    = with(density) { TIP_W.toDp() }

                val hasTarget = targetRect != null && !isWelcome && !isCelebration

                if (hasTarget && targetRect != null) {
                    // ── Spotlight boundary ─────────────────────────────────
                    val sTop  = targetRect.top    - SPOT_PAD
                    val sBot  = targetRect.bottom + SPOT_PAD
                    val sLeft = targetRect.left   - SPOT_PAD
                    val sRight= targetRect.right  + SPOT_PAD
                    val tCX   = targetRect.center.x
                    val tCY   = targetRect.center.y

                    // Space on each side
                    val spBelow = screenH - sBot - SAFE
                    val spAbove = sTop - SAFE
                    val spRight = screenW - sRight - SAFE
                    val spLeft  = sLeft - SAFE

                    // For full-width cards: only BELOW / ABOVE
                    // tipX always = SAFE (card fills the whole width)
                    val placement: ArrowDirection
                    val tipX = SAFE
                    val tipY: Float
                    val arrowFrac: Float

                    val preferAbove = step.bubbleAlign == BubbleAlign.TOP
                    val preferBelow = step.bubbleAlign == BubbleAlign.BOTTOM

                    when {
                        preferAbove && spAbove >= TIP_H_EST + ARROW_H -> {
                            placement = ArrowDirection.TOWARD_BOTTOM
                            tipY      = (sTop - GAP - TIP_H_EST - ARROW_H).coerceAtLeast(SAFE)
                            arrowFrac = ((tCX - tipX) / TIP_W).coerceIn(0.12f, 0.88f)
                        }
                        preferBelow && spBelow >= TIP_H_EST + ARROW_H -> {
                            placement = ArrowDirection.TOWARD_TOP
                            tipY      = sBot + GAP
                            arrowFrac = ((tCX - tipX) / TIP_W).coerceIn(0.12f, 0.88f)
                        }
                        spBelow >= TIP_H_EST + ARROW_H -> {
                            placement = ArrowDirection.TOWARD_TOP
                            tipY      = sBot + GAP
                            arrowFrac = ((tCX - tipX) / TIP_W).coerceIn(0.12f, 0.88f)
                        }
                        spAbove >= TIP_H_EST + ARROW_H -> {
                            placement = ArrowDirection.TOWARD_BOTTOM
                            tipY      = (sTop - GAP - TIP_H_EST - ARROW_H).coerceAtLeast(SAFE)
                            arrowFrac = ((tCX - tipX) / TIP_W).coerceIn(0.12f, 0.88f)
                        }
                        else -> {
                            // Fallback: force below, clamped
                            placement = ArrowDirection.TOWARD_TOP
                            tipY      = (sBot + GAP).coerceAtMost(screenH - TIP_H_EST - SAFE)
                            arrowFrac = ((tCX - tipX) / TIP_W).coerceIn(0.12f, 0.88f)
                        }
                    }

                    // Direct BoxScope child → offset() works correctly here
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(tipX.roundToInt(), tipY.roundToInt()) }
                            .width(tipWDp)
                    ) {
                        TourTooltipBubble(
                            stepIndex           = stepIndex,
                            totalSteps          = engine.totalSteps,
                            isWelcome           = false,
                            isCelebration       = false,
                            arrowDirection      = placement,
                            arrowOffsetFraction = arrowFrac,
                            onNext              = { advance() },
                            onSkip              = { skip() },
                            onBack              = { goBack() },
                            onGoToHelp          = {
                                engine.skip()
                                coroutineScope.launch {
                                    onTourCompleted()
                                    navController.navigate("help") {
                                        popUpTo("dashboard") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // No target: centered card (Welcome / Celebration / full-screen intro)
                    // Direct BoxScope child → align() works correctly here
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(tipWDp)
                    ) {
                        TourTooltipBubble(
                            stepIndex           = stepIndex,
                            totalSteps          = engine.totalSteps,
                            isWelcome           = isWelcome,
                            isCelebration       = isCelebration,
                            arrowDirection      = ArrowDirection.NONE,
                            arrowOffsetFraction = 0.5f,
                            onNext              = { advance() },
                            onSkip              = { skip() },
                            onBack              = { goBack() },
                            onGoToHelp          = {
                                engine.skip()
                                coroutineScope.launch {
                                    onTourCompleted()
                                    navController.navigate("help") {
                                        popUpTo("dashboard") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
