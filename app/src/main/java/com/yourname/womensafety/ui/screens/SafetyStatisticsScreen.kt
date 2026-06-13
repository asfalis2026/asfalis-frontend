package com.yourname.womensafety.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.data.network.dto.SosHistoryItem
import com.yourname.womensafety.ui.viewmodels.SosHistoryUiState
import com.yourname.womensafety.ui.viewmodels.SosHistoryViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ── Theme colours for this screen ─────────────────────────────────────────────
private val StatsBgTop    = Color(0xFF000000)
private val StatsBgMid    = Color(0xFF080404)
private val StatsBgEnd    = Color(0xFF120508)
private val PinkPrimary   = Color(0xFFE91E8C)
private val PinkSoft      = Color(0xFFE15E71)
private val PinkAccent    = Color(0xFFE04B6A)
private val CardSurface   = Color(0xFFE15E71).copy(0.04f)
private val CardBorder    = Color(0xFFE15E71).copy(0.12f)

@Composable
fun SafetyStatisticsScreen(navController: NavController) {
    val historyViewModel: SosHistoryViewModel = viewModel(factory = SosHistoryViewModel.Factory)
    val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()

    // Auto-refresh every 60 seconds for real-time feel
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            historyViewModel.loadHistory()
        }
    }

    var selectedFilter by remember { mutableStateOf("30D") }
    val filters = listOf("7D".tr(), "30D", "6M", "1Y")

    val bg = Brush.verticalGradient(listOf(StatsBgTop, StatsBgMid, StatsBgEnd))

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        // Subtle radial glow from top
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(PinkPrimary.copy(0.08f), Color.Transparent),
                    radius = 800f
                )
            )
        )
        // Background image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.yourname.womensafety.R.drawable.background_image),
            contentDescription = null,
            alpha = 0.35f,
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        )
        // Overlay to blend the image seamlessly into the background
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF000000), Color.Transparent, Color(0xFF120508)),
                    startY = 0f
                )
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(Color(0xFFE25F71).copy(0.25f))
                        .border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Safety Statistics".tr(), color = Color.White, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(Color(0xFF4CAF50)))
                        Spacer(Modifier.width(5.dp))
                        Text("Live · auto-refreshes every minute".tr(), color = Color(0xFF4CAF50),
                            fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Time Range Filter Chips ───────────────────────────────────────
            LazyRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { f ->
                    val isSelected = selectedFilter == f
                    Surface(
                        onClick = { selectedFilter = f },
                        color = if (isSelected) PinkPrimary else PinkPrimary.copy(0.06f),
                        shape = RoundedCornerShape(20.dp),
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
                            1.dp, PinkPrimary.copy(0.2f)
                        ) else null
                    ) {
                        Text(
                            f,
                            color = if (isSelected) Color.White else PinkSoft,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
            ) {

            when (val state = uiState) {
                is SosHistoryUiState.Loading -> {
                    StatsShimmerLoading()
                }

                is SosHistoryUiState.Error -> {
                    Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, null, tint = PinkPrimary.copy(0.5f),
                                modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Could not load statistics".tr(), color = Color.Gray)
                            Text(state.message, color = Color.Gray, fontSize = 12.sp,
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { historyViewModel.loadHistory() },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                shape = RoundedCornerShape(12.dp)) {
                                Text("Retry".tr())
                            }
                        }
                    }
                }

                is SosHistoryUiState.Success -> {
                    val cutoffMs = when (selectedFilter) {
                        "7D"  -> System.currentTimeMillis() - 7L  * 24 * 60 * 60 * 1000
                        "30D" -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                        "6M"  -> System.currentTimeMillis() - 183L* 24 * 60 * 60 * 1000
                        "1Y"  -> System.currentTimeMillis() - 365L* 24 * 60 * 60 * 1000
                        else  -> 0L
                    }
                    val parsers = listOf(
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".tr(), Locale.getDefault()).apply { timeZone = TimeZone.getDefault() },
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() },
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() },
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }
                    )
                    val realFiltered = state.items.filter { item ->
                        var ms = -1L
                        for (p in parsers) {
                            try { ms = p.parse(item.triggeredAt)?.time ?: -1L; break } catch (_: Exception) {}
                        }
                        if (ms == -1L) true else ms >= cutoffMs // If parsing fails completely, include it rather than hiding it
                    }

                    StatisticsContent(
                        items = realFiltered,
                        selectedFilter = selectedFilter,
                        totalAll = state.items.size,
                        isMockData = false
                    )
                }
            }
            }
        }
    }
}

// ── Shimmer Loading ────────────────────────────────────────────────────────────

@Composable
private fun StatsShimmerLoading() {
    val shimmer = rememberInfiniteTransition(label = "shimmer".tr())
    val shimmerX by shimmer.animateFloat(
        initialValue = -300f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmerX".tr()
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(0.04f),
            Color.White.copy(0.12f),
            Color.White.copy(0.04f)
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 300f, 300f)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Box(modifier = Modifier.weight(1f).height(100.dp)
                    .clip(RoundedCornerShape(18.dp)).background(shimmerBrush))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Box(modifier = Modifier.weight(1f).height(100.dp)
                    .clip(RoundedCornerShape(18.dp)).background(shimmerBrush))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)
            .clip(RoundedCornerShape(20.dp)).background(shimmerBrush))
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)
            .clip(RoundedCornerShape(20.dp)).background(shimmerBrush))
    }
}

// ── Statistics Content ─────────────────────────────────────────────────────────

@Composable
private fun StatisticsContent(
    items: List<SosHistoryItem>,
    selectedFilter: String,
    totalAll: Int,
    isMockData: Boolean = false
) {
    val total = items.size
    val sent = items.count { it.status in listOf("sent".tr(), "dispatched") }
    val cancelled = items.count { it.status == "cancelled" }
    val autoTriggered = items.count { it.triggerType.startsWith("auto") }
    val manual = items.count { it.triggerType == "manual" }
    val wearable = items.count { it.triggerType in listOf("iot_button", "bracelet".tr()) }
    val distress = items.count { it.triggerType == "hardware_distress" }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    // Overall account summary chip
    Surface(
        color = PinkPrimary.copy(0.08f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PinkPrimary.copy(0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Analytics, null, tint = PinkAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("${"All time".tr()}: $totalAll ${"SOS events".tr()}  •  ${"Showing".tr()}: ${items.size} ${"in".tr()} ${selectedFilter.tr()}",
                    color = PinkSoft, fontSize = 12.sp
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))

    // ── Summary stat cards ───────────────────────────────────────────────────
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = "Total Alerts".tr(),
            value = (total * animProgress.value).toInt().toString(),
            icon = Icons.Outlined.History,
            accent = PinkPrimary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Sent".tr(),
            value = (sent * animProgress.value).toInt().toString(),
            icon = Icons.Default.Send,
            accent = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = "Cancelled".tr(),
            value = (cancelled * animProgress.value).toInt().toString(),
            icon = Icons.Default.Cancel,
            accent = Color(0xFFFFB74D),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Auto SOS".tr(),
            value = (autoTriggered * animProgress.value).toInt().toString(),
            icon = Icons.Default.Sensors,
            accent = Color(0xFF4FC3F7),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(28.dp))

    // ── Trigger Type Donut Chart ─────────────────────────────────────────────
    SectionHeader("Trigger Breakdown".tr())
    Spacer(Modifier.height(14.dp))

    if (total > 0) {
        DonutChart(
            segments = listOf(
                DonutSegment("Manual".tr(), manual, PinkPrimary),
                DonutSegment("Auto".tr(), autoTriggered, Color(0xFF4FC3F7)),
                DonutSegment("Wearable".tr(), wearable, Color(0xFFFFB74D)),
                DonutSegment("Distress".tr(), distress, Color(0xFF9C8FFF))
            ).filter { it.count > 0 },
            animProgress = animProgress.value
        )
    } else {
        EmptyChartPlaceholder("No alert data in this period".tr())
    }

    Spacer(Modifier.height(28.dp))

    // ── Classification Radar Chart ───────────────────────────────────────────
    SectionHeader("Alert Classification".tr())
    Spacer(Modifier.height(6.dp))
    Text("Manual taps, Wearable triggers, Hardware Distress, Auto Falls & Auto Shakes".tr(),
        color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp
    )
    Spacer(Modifier.height(10.dp))
    if (total > 0) {
        val auto = items.count { it.triggerType == "auto_sos" || it.triggerType == "auto_fall" || it.triggerType == "auto_shake" }
        ClassificationRadarChart(
            manual = manual, wearable = wearable, distress = distress,
            falls = auto, shakes = 0, animProgress = animProgress.value
        )
    } else {
        EmptyChartPlaceholder("No classification data in this period".tr())
    }

    Spacer(Modifier.height(28.dp))

    // ── Bar Chart ─────────────────────────────────────────────────────────────
    val timelineLabel = when (selectedFilter) {
        "7D"  -> "Daily SOS Triggers — Last 7 Days".tr()
        "30D" -> "Weekly SOS Triggers — Last 30 Days".tr()
        "6M"  -> "Monthly SOS Triggers — Last 6 Months".tr()
        "1Y"  -> "Monthly SOS Triggers — Last 12 Months".tr()
        else  -> "SOS Triggers Timeline".tr()
    }
    SectionHeader(timelineLabel)
    Spacer(Modifier.height(6.dp))
    Text("Each bar represents how many SOS events were triggered in that window.".tr(),
        color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp
    )
    Spacer(Modifier.height(10.dp))

    val monthlyData = buildTimelineData(items, selectedFilter)
    if (monthlyData.any { it.second > 0 }) {
        MonthlyBarChart(monthlyData = monthlyData, animProgress = animProgress.value)
    } else {
        EmptyChartPlaceholder("No activity in the selected period".tr())
    }

    Spacer(Modifier.height(28.dp))

    // ── Status Distribution ───────────────────────────────────────────────────
    SectionHeader("Status Distribution".tr())
    Spacer(Modifier.height(14.dp))

    val statuses = items.groupBy { it.status }
        .map { (status, list) -> status to list.size }
        .sortedByDescending { it.second }

    if (statuses.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            statuses.forEach { (status, count) ->
                val color = when (status) {
                    "sent", "dispatched" -> Color(0xFF4CAF50)
                    "cancelled" -> Color(0xFFFFB74D)
                    "expired" -> Color(0xFF888888)
                    else -> PinkPrimary
                }
                val fraction = if (total > 0) count.toFloat() / total else 0f
                StatusBar(
                    label = status.replaceFirstChar { it.uppercase() }.tr(),
                    count = count,
                    fraction = fraction * animProgress.value,
                    color = color
                )
            }
        }
    } else {
        EmptyChartPlaceholder("No data available".tr())
    }

    Spacer(Modifier.height(28.dp))

    // ── Recent Timeline ───────────────────────────────────────────────────────
    if (items.isNotEmpty()) {
        SectionHeader("Recent Alerts".tr())
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp)
        ) {
            items.take(5).forEachIndexed { idx, item ->
                TimelineRow(item = item, isLast = idx == minOf(4, items.size - 1))
            }
        }
    }
}

// ── Donut Chart ───────────────────────────────────────────────────────────────

data class DonutSegment(val label: String, val count: Int, val color: Color)

@Composable
private fun DonutChart(segments: List<DonutSegment>, animProgress: Float) {
    val total = segments.sumOf { it.count }.toFloat()

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 28.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Draw background track ring FIRST (behind segments)
                    drawArc(
                        color = Color.White.copy(0.06f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(strokeWidth)
                    )

                    // Draw coloured segments ON TOP
                    var startAngle = -90f
                    segments.forEach { seg ->
                        val sweep = (seg.count / total) * 360f * animProgress
                        drawArc(
                            color = seg.color,
                            startAngle = startAngle,
                            sweepAngle = sweep.coerceAtLeast(0f),
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sweep + 2f
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(total * animProgress).toInt()}".tr(),
                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                    Text("alerts".tr(), color = Color.Gray, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.width(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                segments.forEach { seg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(seg.color))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(seg.label, color = Color.White, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text("${seg.count} (${if (total > 0) ((seg.count / total) * 100).toInt() else 0}%)".tr(),
                                color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Monthly Bar Chart ─────────────────────────────────────────────────────────

@Composable
private fun MonthlyBarChart(monthlyData: List<Pair<String, Int>>, animProgress: Float) {
    val maxVal = monthlyData.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyData.forEach { (_, count) ->
                    val fraction = (count / maxVal) * animProgress
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (count > 0) {
                            Text("$count".tr(), color = Color.White, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((120 * fraction).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (count > 0)
                                        Brush.verticalGradient(listOf(PinkPrimary, Color(0xFF8B0050)))
                                    else
                                        Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.04f)))
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                monthlyData.forEach { (month, _) ->
                    Text(month, color = Color.Gray, fontSize = 9.sp,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ── Status progress bar ────────────────────────────────────────────────────────

@Composable
private fun StatusBar(label: String, count: Int, fraction: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("$count".tr(), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(0.08f))) {
            Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

// ── Timeline row ──────────────────────────────────────────────────────────────

@Composable
private fun TimelineRow(item: SosHistoryItem, isLast: Boolean) {
    val accent = when (item.triggerType) {
        "manual" -> PinkPrimary
        "auto_shake", "auto_fall" -> Color(0xFF4FC3F7)
        "iot_button", "bracelet" -> Color(0xFFFFB74D)
        "hardware_distress" -> Color(0xFF9C8FFF)
        else -> Color(0xFF888888)
    }
    val statusColor = when (item.status) {
        "sent", "dispatched" -> Color(0xFF4CAF50)
        "cancelled" -> Color(0xFFFFAA00)
        else -> Color(0xFF888888)
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(32.dp).background(Color.White.copy(0.08f)))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.triggerType.replace("_", " ").replaceFirstChar { it.uppercase() },
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Text(formatTriggeredAt(item.triggeredAt), color = Color.Gray, fontSize = 11.sp)
        }
        Surface(color = statusColor.copy(0.15f), shape = RoundedCornerShape(8.dp)) {
            Text(
                item.status.uppercase(),
                color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
            .background(PinkPrimary))
        Spacer(Modifier.width(10.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatCard(
    label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(18.dp))
            .background(accent.copy(0.08f))
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BarChart, null, tint = Color.Gray.copy(0.4f),
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

// ── Build timeline data ──────────────────────────────────────────────────────
private fun buildTimelineData(items: List<SosHistoryItem>, filter: String): List<Pair<String, Int>> {
    val result = mutableListOf<Pair<String, Int>>()
    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() },
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }
    )

    when (filter) {
        "7D" -> {
            val fmt = SimpleDateFormat("EEE", Locale.getDefault())
            for (offset in 6 downTo 0) {
                val target = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                val targetYear = target.get(Calendar.YEAR)
                val targetDay = target.get(Calendar.DAY_OF_YEAR)
                val count = items.count { item ->
                    var match = false
                    for (p in parsers) {
                        try {
                            val date = p.parse(item.triggeredAt) ?: continue
                            val cal = Calendar.getInstance().apply { time = date }
                            if (cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.DAY_OF_YEAR) == targetDay) {
                                match = true; break
                            }
                        } catch(e: Exception){}
                    }
                    match
                }
                result.add(fmt.format(target.time) to count)
            }
        }
        "30D" -> {
            for (offset in 3 downTo 0) {
                val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(offset * 7 + 7)) }
                val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(offset * 7)) }
                val count = items.count { item ->
                    var match = false
                    for (p in parsers) {
                        try {
                            val date = p.parse(item.triggeredAt) ?: continue
                            if (date.time > start.timeInMillis && date.time <= end.timeInMillis) {
                                match = true; break
                            }
                        } catch(e: Exception){}
                    }
                    match
                }
                result.add("W${4-offset}" to count)
            }
        }
        else -> {
            val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
            val maxOffsets = if (filter == "1Y") 11 else 5
            for (offset in maxOffsets downTo 0) {
                val target = Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
                val targetYear = target.get(Calendar.YEAR)
                val targetMonth = target.get(Calendar.MONTH)
                val count = items.count { item ->
                    var match = false
                    for (p in parsers) {
                        try {
                            val date = p.parse(item.triggeredAt) ?: continue
                            val cal = Calendar.getInstance().apply { time = date }
                            if (cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.MONTH) == targetMonth) {
                                match = true; break
                            }
                        } catch(e: Exception){}
                    }
                    match
                }
                result.add(monthFmt.format(target.time) to count)
            }
        }
    }
    return result
}

// ── Mock data generator with filter period ──────────────────────────────────────
private fun generateMockAlerts(cutoffMs: Long = 0L): List<SosHistoryItem> {
    val items = mutableListOf<SosHistoryItem>()
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }
    val now = System.currentTimeMillis()
    val random = java.util.Random(42)
    // Spread mock alerts within the requested period
    val rangeMs = if (cutoffMs > 0L) (now - cutoffMs) else (365L * 24 * 60 * 60 * 1000L)

    for (i in 0 until 80) {
        val offsetMs = (random.nextDouble() * rangeMs).toLong()
        val ms = now - offsetMs

        // Weighting types and statuses to look realistic
        val type = when (random.nextInt(100)) {
            in 0..40 -> "manual".trNonComposable()
            in 41..60 -> "auto_fall"
            in 61..80 -> "bracelet".trNonComposable()
            in 81..95 -> "hardware_distress"
            else -> "auto_shake"
        }
        val status = when (random.nextInt(100)) {
            in 0..50 -> "sent".trNonComposable()
            in 51..85 -> "cancelled".trNonComposable()
            in 86..95 -> "expired".trNonComposable()
            else -> "dispatched".trNonComposable()
        }

        items.add(SosHistoryItem(
            id = "mock_$i",
            triggeredAt = format.format(Date(ms)),
            triggerType = type,
            status = status,
            address = null
        ))
    }
    return items.sortedByDescending { it.triggeredAt }
}

// ── Format triggered-at timestamp ─────────────────────────────────────────────
fun formatTriggeredAt(raw: String?): String {
    if (raw.isNullOrBlank()) return "Unknown time"
    
    val dateToParse = if (raw.contains("T")) {
        val clean = raw.replace("T", " ")
        if (clean.contains(".")) {
            clean.substringBefore(".")
        } else if (clean.endsWith("Z")) {
            clean.dropLast(1)
        } else {
            clean
        }
    } else {
        raw
    }

    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply { timeZone = TimeZone.getDefault() },
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = TimeZone.getDefault() }
    )
    for (parser in parsers) {
        try {
            val date = parser.parse(dateToParse) ?: continue
            val display = SimpleDateFormat("dd MMM yyyy '•' h:mm a", Locale.ENGLISH)
            display.timeZone = TimeZone.getDefault()
            return display.format(date)
        } catch (_: Exception) {}
    }
    return raw
}

// ── Radar Chart ───────────────────────────────────────────────────────────────

@Composable
private fun ClassificationRadarChart(
    manual: Int, wearable: Int, distress: Int, falls: Int, shakes: Int, animProgress: Float
) {
    val maxVal = maxOf(manual, wearable, distress, falls, shakes).toFloat().coerceAtLeast(1f)
    val labels = listOf("Manual".tr(), "Wearable".tr(), "Distress".tr(), "Falls".tr(), "Shakes".tr())
    val values = listOf(
        manual / maxVal, wearable / maxVal, distress / maxVal, falls / maxVal, shakes / maxVal
    )

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (minOf(size.width, size.height) / 2f) * 0.7f

            for (level in 1..3) {
                val levelRadius = radius * (level / 3f)
                val path = Path()
                for (i in 0 until 5) {
                    val angle = (Math.PI * 2 * i / 5) - (Math.PI / 2)
                    val x = center.x + cos(angle).toFloat() * levelRadius
                    val y = center.y + sin(angle).toFloat() * levelRadius
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path = path, color = PinkPrimary.copy(0.12f), style = Stroke(1.dp.toPx()))
            }

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            for (i in 0 until 5) {
                val angle = (Math.PI * 2 * i / 5) - (Math.PI / 2)
                val endX = center.x + cos(angle).toFloat() * radius
                val endY = center.y + sin(angle).toFloat() * radius
                drawLine(color = PinkPrimary.copy(0.15f), start = center,
                    end = Offset(endX, endY), strokeWidth = 1.dp.toPx())
                val labelRadius = radius * 1.3f
                val labelX = center.x + cos(angle).toFloat() * labelRadius
                val labelY = center.y + sin(angle).toFloat() * labelRadius
                drawContext.canvas.nativeCanvas.drawText(labels[i], labelX, labelY + 12f, textPaint)
            }

            val dataPath = Path()
            for (i in 0 until 5) {
                val angle = (Math.PI * 2 * i / 5) - (Math.PI / 2)
                val dataRadius = radius * (values[i] * animProgress).coerceAtLeast(0.01f)
                val x = center.x + cos(angle).toFloat() * dataRadius
                val y = center.y + sin(angle).toFloat() * dataRadius
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                drawCircle(color = PinkPrimary, radius = 3.dp.toPx(), center = Offset(x, y))
            }
            dataPath.close()
            drawPath(path = dataPath, color = PinkPrimary.copy(0.2f))
            drawPath(path = dataPath, color = PinkPrimary,
                style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}
