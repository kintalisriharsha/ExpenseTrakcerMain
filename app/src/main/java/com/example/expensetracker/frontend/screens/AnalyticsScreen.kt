package com.example.expensetracker.frontend.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.core.graphics.toColorInt
import com.example.expensetracker.frontend.components.BottomNavBar
import com.example.expensetracker.frontend.components.LogoIcon
import com.example.expensetracker.data.local.entity.analytics.AnalyticsSummary
import com.example.expensetracker.frontend.services.analyticsService.AnalyticsUiState
import com.example.expensetracker.frontend.services.analyticsService.AnalyticsViewModel
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.ErrorStateContent
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.services.analyticsService.CategoryAmountRow
import com.example.expensetracker.data.local.entity.analytics.MonthTrendItem
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.BackgroundLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.SurfaceLight
import com.example.expensetracker.ui.theme.TextPrimary
import com.example.expensetracker.ui.theme.TextSecondary
import java.time.YearMonth

// ─── Palette ──────────────────────────────────────────────────────────────────

private val PrimaryBlue   = Color(0xFF2563EB)
private val CardBg        = Color(0xFFFFFFFF)
private val ProgressTrack = Color(0xFFE5E7EB)

private val categoryPalette = listOf(
    Color(0xFF1E3A8A),
    Color(0xFFDC2626),
    Color(0xFF374151),
    Color(0xFF6B7280),
    Color(0xFFD1D5DB),
)

// Default daily-spend thresholds used to color the heatmap.
// Below LOW = "Low", between LOW and HIGH = "Medium"/"High", at/above HIGH = "Most spent" (over budget).
private const val DEFAULT_DAILY_BUDGET_LOW  = 160f
private const val DEFAULT_DAILY_BUDGET_HIGH = 200f

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AnalyticsScreen(
    navController      : NavController,
    isDark             : Boolean,
    analyticsViewModel : AnalyticsViewModel,
) {
    val background  = if (isDark) BackgroundDark else BackgroundLight
    val surface     = if (isDark) SurfaceDark    else SurfaceLight
    val border      = if (isDark) BorderDark     else BorderLight
    val textPrimary = if (isDark) Color.White    else TextPrimary

    val uiState by analyticsViewModel.state.collectAsState()
    var toast by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is AnalyticsUiState.Error) {
            toast = ToastMessage((uiState as AnalyticsUiState.Error).message, ToastType.ERROR)
        }
    }

    LaunchedEffect(Unit) { analyticsViewModel.loadSummary() }

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
                title = {
                    Column(
                        Modifier.fillMaxWidth(),
                        Arrangement.Top,
                        Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Analytics",
                            color      = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 24.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint               = textPrimary,
                            modifier           = Modifier.size(32.dp)
                        )
                    }
                },
                navigationIcon = {
                    Column(Modifier.padding(start = 16.dp)) {
                        LogoIcon(navController = navController)
                    }
                },
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab   = -1,
                navController = navController,
                surface       = surface,
                border        = border,
                textPrimary   = textPrimary,
                isDark        = isDark
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxWidth()){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val state = uiState) {
                    is AnalyticsUiState.Loading, AnalyticsUiState.Idle -> {
                        CircularProgressIndicator(
                            color = Primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is AnalyticsUiState.Error -> {
                        ErrorStateContent(
                            errorMessage = state.message,
                            onRetry      = { analyticsViewModel.loadSummary() },
                            isDark       = isDark,
                            modifier     = Modifier.align(Alignment.Center)
                        )
                    }

                    is AnalyticsUiState.Loaded -> {
                        AnalyticsContent(
                            data = state.data,
                            isDark = isDark,
                        )
                    }
                }
            }
            CustomToast(
                toast = toast,
                onDismiss = { toast = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }
}

// ─── Content ──────────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AnalyticsContent(
    data   : AnalyticsSummary,
    isDark : Boolean,
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else CardBg

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TotalSpentCard(
            spent   = data.totalSpent.totalSpent.toFloat(),
            budget  = data.totalSpent.monthlyBudget.toFloat(),
            usedPct = data.totalSpent.usedPct.toFloat(),
            cardBg  = cardBg,
            isDark  = isDark,
        )

        if (data.categoryBreakdown.categories.isNotEmpty()) {
            CategoryBreakdownCard(
                categories = data.categoryBreakdown.categories,
                total      = data.categoryBreakdown.total,
                cardBg     = cardBg,
                isDark     = isDark,
            )
        }

        if (data.monthlyTrend.months.isNotEmpty()) {
            MonthlyTrendCard(
                months   = data.monthlyTrend.months,
                trendPct = data.monthlyTrend.trendPct.toFloat(),
                budget   = data.monthlyTrend.budget.toFloat(),
                cardBg   = cardBg,
                isDark   = isDark,
            )
        }

        SpendingHeatmapCard(
            yearMonth = YearMonth.of(data.heatmap.year, data.heatmap.month),
            dailyData = data.heatmap.daily.associate { it.day to it.amount.toFloat() },
            avgSpend  = data.heatmap.avgSpend.toFloat(),
            cardBg    = cardBg,
            isDark    = isDark,
            // Tune these to match your actual per-day budget. Defaults: 160 / 200.
            dailyBudgetLow  = DEFAULT_DAILY_BUDGET_LOW,
            dailyBudgetHigh = DEFAULT_DAILY_BUDGET_HIGH,
        )

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Card 1: Total Spent ──────────────────────────────────────────────────────

@Composable
fun TotalSpentCard(
    spent   : Float,
    budget  : Float,
    usedPct : Float,
    cardBg  : Color,
    isDark  : Boolean,
) {
    val hasBudget      = budget > 0f
    val progress       = if (hasBudget) (usedPct / 100f).coerceIn(0f, 1f) else 0f
    val textPrimary    = if (isDark) Color.White else TextPrimary
    val textSecondary  = if (isDark) Color(0xFFADB5C7) else TextSecondary

    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label         = "budget_progress"
    )

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total Spent This Month", fontSize = 13.sp, color = textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "₹${"%.2f".format(spent)}",
                fontSize   = 34.sp,
                fontWeight = FontWeight.Bold,
                color      = textPrimary
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (hasBudget) "Budget: ₹${"%.2f".format(budget)}" else "No budget set",
                    fontSize = 13.sp,
                    color    = textSecondary
                )
                if (hasBudget) {
                    Text(
                        "${usedPct.toInt()}% Used",
                        fontSize   = 13.sp,
                        color      = if (usedPct >= 100f) Color(0xFFDC2626) else PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (hasBudget) {
                LinearProgressIndicator(
                    progress   = { animatedProgress },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color      = if (usedPct >= 100f) Color(0xFFDC2626) else PrimaryBlue,
                    trackColor = if (isDark) Color(0xFF2A3347) else ProgressTrack,
                )
            } else {
                // Empty track as a subtle placeholder so the card height doesn't jump
                // once a budget is added later.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF2A3347) else ProgressTrack)
                )
            }
        }
    }
}

// ─── Card 2: Category Breakdown ──────────────────────────────────────────────

@Composable
fun CategoryBreakdownCard(
    categories : List<CategoryAmountRow>,
    total      : Double,
    cardBg     : Color,
    isDark     : Boolean,
) {
    val textPrimary   = if (isDark) Color.White else TextPrimary
    val textSecondary = if (isDark) Color(0xFFADB5C7) else TextSecondary

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Category Breakdown",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textPrimary
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                DonutChart(categories = categories, total = total)
            }

            Spacer(Modifier.height(16.dp))

            categories.forEachIndexed { index, cat ->
                val color      = categoryPalette.getOrElse(index) { Color(0xFF9CA3AF) }
                val percentage = if (total > 0) (cat.amount / total) * 100.0 else 0.0
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        cat.category,
                        fontSize = 14.sp,
                        color    = textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${"%.1f".format(percentage)}%",
                        fontSize = 14.sp,
                        color    = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChart(categories: List<CategoryAmountRow>, total: Double) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = size.minDimension * 0.18f
        val radius      = (size.minDimension - strokeWidth) / 2f
        val center      = Offset(size.width / 2f, size.height / 2f)
        var startAngle  = -90f

        categories.forEachIndexed { index, cat ->
            val color      = categoryPalette.getOrElse(index) { Color(0xFF9CA3AF) }
            val fraction   = if (total > 0) (cat.amount / total).toFloat() else 0f
            val sweepAngle = fraction * 360f
            drawArc(
                color      = color,
                startAngle = startAngle,
                sweepAngle = (sweepAngle - 2f),
                useCenter  = false,
                topLeft    = Offset(center.x - radius, center.y - radius),
                size       = Size(radius * 2, radius * 2),
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

// ─── Card 3: Monthly Trend ────────────────────────────────────────────────────

@Composable
fun MonthlyTrendCard(
    months   : List<MonthTrendItem>,
    trendPct : Float,
    budget   : Float,
    cardBg   : Color,
    isDark   : Boolean,
) {
    val textPrimary   = if (isDark) Color.White else TextPrimary
    val textSecondary = if (isDark) Color(0xFFADB5C7) else TextSecondary

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Monthly Trend",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = textPrimary
                )

                // Only show badge when there is a meaningful trend
                if (trendPct != 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (trendPct >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint     = if (trendPct >= 0) Color(0xFFEF4444) else Color(0xFF22C55E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${"%.1f".format(kotlin.math.abs(trendPct))}% vs last month",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color      = if (trendPct >= 0) Color(0xFFEF4444) else Color(0xFF22C55E)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Full-width axis chart — always drawn, even with a single month of history,
            // so the card never collapses down to a bare number.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                LineChart(months = months, budget = budget, isDark = isDark)
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                months.forEach { m ->
                    Text(
                        m.month,
                        fontSize  = 11.sp,
                        color     = textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Simple dot legend — no filled swatch boxes
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(PrimaryBlue))
                Spacer(Modifier.width(6.dp))
                Text("Actual", fontSize = 11.sp, color = textSecondary)
                if (budget > 0f) {
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                    Spacer(Modifier.width(6.dp))
                    Text("Budget", fontSize = 11.sp, color = textSecondary)
                }
            }
        }
    }
}

@Composable
fun LineChart(months: List<MonthTrendItem>, budget: Float, isDark: Boolean) {
    val gridColor  = if (isDark) Color(0xFF334155) else Color(0xFFE5E7EB)
    val labelColor = if (isDark) "#94A3B8".toColorInt() else "#6B7280".toColorInt()
    val dotFill    = if (isDark) Color(0xFF1E293B) else CardBg

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (months.isEmpty()) return@Canvas

        val dataMax = months.maxOf { it.amount }.toFloat()
        val axisMax = (maxOf(dataMax, budget) * 1.2f).let { if (it <= 0f) 100f else it }
        val tickCount = 4

        val leftPad  = 46f
        val topPad   = 8f
        val plotWidth  = size.width - leftPad
        val plotHeight = size.height - topPad

        fun yFor(amount: Float) = topPad + plotHeight - (amount / axisMax) * plotHeight
        fun xFor(i: Int) = leftPad + if (months.size > 1) i / (months.size - 1f) * plotWidth else plotWidth / 2f

        val labelPaint = android.graphics.Paint().apply {
            textSize    = 24f
            color       = labelColor
            textAlign   = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }

        // Horizontal gridlines + ₹ axis labels, like a standard line-chart axis
        for (t in 0..tickCount) {
            val value = axisMax / tickCount * t
            val y = yFor(value)
            drawLine(gridColor, Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1f)
            val label = if (value >= 1000) "₹${"%.1f".format(value / 1000)}k" else "₹${value.toInt()}"
            drawContext.canvas.nativeCanvas.drawText(label, leftPad - 10f, y + 8f, labelPaint)
        }

        // Flat "Budget" goal line across the whole window, same idea as the reference chart
        if (budget > 0f) {
            val goalY = yFor(budget)
            drawLine(
                color       = Color(0xFFF59E0B),
                start       = Offset(leftPad, goalY),
                end         = Offset(size.width, goalY),
                strokeWidth = 3f
            )
            months.indices.forEach { i ->
                drawCircle(Color(0xFFF59E0B), radius = 4f, center = Offset(xFor(i), goalY))
            }
        }

        // Actual spend line, smoothed between points
        val points = months.mapIndexed { i, m -> Offset(xFor(i), yFor(m.amount.toFloat())) }
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }
        drawPath(
            path  = linePath,
            color = PrimaryBlue,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        points.forEach { pt ->
            drawCircle(color = dotFill,     radius = 5f, center = pt)
            drawCircle(color = PrimaryBlue, radius = 4f, center = pt, style = Stroke(width = 2f))
        }
    }
}

// ─── Card 4: Spending Heatmap ─────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SpendingHeatmapCard(
    yearMonth       : YearMonth,
    dailyData       : Map<Int, Float>,
    avgSpend        : Float,
    cardBg          : Color,
    isDark          : Boolean,
    // Absolute daily-spend thresholds (not relative to this month's max).
    // A day below `dailyBudgetLow` is "Low/Medium", at/above `dailyBudgetHigh` is "Most spent" (over budget).
    dailyBudgetLow  : Float = DEFAULT_DAILY_BUDGET_LOW,
    dailyBudgetHigh : Float = DEFAULT_DAILY_BUDGET_HIGH,
) {
    val daysOfWeek  = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val firstDay    = yearMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1
    val totalDays   = yearMonth.lengthOfMonth()
    val textPrimary   = if (isDark) Color.White else TextPrimary
    val textSecondary = if (isDark) Color(0xFFADB5C7) else TextSecondary

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                "Spending Heatmap",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textPrimary
            )
            Spacer(Modifier.height(14.dp))

            // Day-of-week header
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        day,
                        modifier   = Modifier.weight(1f),
                        textAlign  = TextAlign.Center,
                        fontSize   = 11.sp,
                        color      = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar grid
            val cells = buildList {
                repeat(startOffset) { add(null) }
                (1..totalDays).forEach { add(it) }
            }

            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    repeat(7) { col ->
                        val day = week.getOrNull(col)
                        Box(
                            modifier         = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                val spend   = dailyData[day] ?: 0f
                                // Color tiers based on fixed budget thresholds instead of the month's relative max.
                                val bgColor = when {
                                    spend <= 0f                    -> if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6)
                                    spend < dailyBudgetLow * 0.5f   -> Color(0xFFBFDBFE)
                                    spend < dailyBudgetLow          -> Color(0xFF60A5FA)
                                    spend < dailyBudgetHigh         -> Color(0xFF3B82F6)
                                    else                             -> Color(0xFF1D4ED8) // at/over budget
                                }
                                val isDarkCell = spend >= dailyBudgetLow
                                val textColor  = if (isDarkCell) Color.White else textPrimary

                                Box(
                                    modifier         = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day",
                                        fontSize   = 12.sp,
                                        color      = textColor,
                                        fontWeight = if (isDarkCell) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Color guide legend
            Column {
                Text(
                    "Color guide (daily budget ₹${dailyBudgetLow.toInt()}–₹${dailyBudgetHigh.toInt()}):",
                    fontSize = 12.sp,
                    color    = textSecondary
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    listOf(
                        Pair(if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6), "No spend"),
                        Pair(Color(0xFFBFDBFE), "Low"),
                        Pair(Color(0xFF60A5FA), "Medium"),
                        Pair(Color(0xFF3B82F6), "High"),
                        Pair(Color(0xFF1D4ED8), "Over budget"),
                    ).forEach { (color, label) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                label,
                                fontSize  = 9.sp,
                                color     = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Avg ₹${"%.0f".format(avgSpend)}/day",
                    fontSize = 10.sp,
                    color    = textSecondary
                )
            }
        }
    }
}