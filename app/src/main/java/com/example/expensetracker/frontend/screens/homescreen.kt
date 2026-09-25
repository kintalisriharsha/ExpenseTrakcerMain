package com.example.expensetracker.frontend.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.components.BottomNavBar
import com.example.expensetracker.frontend.components.CircularProgress
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.ErrorStateContent
import com.example.expensetracker.frontend.components.LogoIcon
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.important.Appstate
import com.example.expensetracker.frontend.important.Appstate.isDark
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.frontend.services.homeService.HomeDashboard
import com.example.expensetracker.frontend.services.homeService.HomeUiState
import com.example.expensetracker.frontend.services.homeService.HomeViewModel
import com.example.expensetracker.services.entity.ExpenseEntity
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.BackgroundLight
import com.example.expensetracker.ui.theme.Blue
import com.example.expensetracker.ui.theme.BlueDark
import com.example.expensetracker.ui.theme.BlueLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.Green
import com.example.expensetracker.ui.theme.GreenDark
import com.example.expensetracker.ui.theme.GreenLight
import com.example.expensetracker.ui.theme.Orange
import com.example.expensetracker.ui.theme.OrangeDark
import com.example.expensetracker.ui.theme.OrangeLight
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryLight
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.SurfaceLight
import com.example.expensetracker.ui.theme.TextPrimary
import com.example.expensetracker.ui.theme.TextSecondary

// ─── UiScale ─────────────────────────────────────────────────────────────────

object UiScale {
    val title             = 20.sp
    val body              = 15.sp
    val small             = 13.sp
    val tiny              = 11.sp
    val screenPadding     = 22.dp
    val cardPadding       = 22.dp
    val sectionTopGap     = 18.dp
    val sectionGap        = 32.dp
    val itemGap           = 12.dp
    val cardCorner        = 18.dp
    val expenseCardCorner = 16.dp
    val expenseRowPadding = 18.dp
    val expenseIconBox    = 54.dp
    val expenseIcon       = 24.dp
    val progressRingSize  = 136.dp
    val navIconButton     = 36.dp
    val navIcon           = 26.dp
    val navLabel          = 12.sp
}

// ─── Category icon mapper ─────────────────────────────────────────────────────

private data class CategoryMeta(
    val icon    : ImageVector,
    val bgLight : Color,
    val bgDark  : Color,
    val tint    : Color
)

private fun categoryMeta(category: String): CategoryMeta = when (category.lowercase()) {
    "food"      -> CategoryMeta(Icons.Outlined.Restaurant,      OrangeLight,        OrangeDark,         Orange)
    "transport" -> CategoryMeta(Icons.Filled.DirectionsCar,     BlueLight,          BlueDark,           Blue)
    "shopping"  -> CategoryMeta(Icons.Outlined.ShoppingBag,     GreenLight,         GreenDark,          Green)
    "leisure"   -> CategoryMeta(Icons.Outlined.Movie,           Color(0xFFF3E8FF),  Color(0xFF2D1B4E),  Color(0xFF9333EA))
    "housing"   -> CategoryMeta(Icons.Outlined.Home,            Color(0xFFDBEAFE),  Color(0xFF1E2A40),  Color(0xFF3B82F6))
    "health"    -> CategoryMeta(Icons.Outlined.MedicalServices, Color(0xFFFFE4E6),  Color(0xFF3D1A1D),  Color(0xFFE74C3C))
    "education" -> CategoryMeta(Icons.Outlined.School,          Color(0xFFFEF9C3),  Color(0xFF2D2A0A),  Color(0xFFD4A017))
    "credit/debit card" -> CategoryMeta(Icons.Filled.CreditCard, Color(0xFFCCFBF1), Color(0xFF0F3D3A),  Color(0xFF0D9488))
    else        -> CategoryMeta(Icons.Outlined.MoreHoriz,       Color(0xFFF1F5F9),  Color(0xFF1E2230),  Color(0xFF94A3B8))
}

// ─── Main Screen ──────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExpenseTrackerScreen(
    isDark        : Boolean = isSystemInDarkTheme(),
    navController : NavController,
    homeViewModel : HomeViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val background    = if (isDark) BackgroundDark else BackgroundLight
    val surface       = if (isDark) SurfaceDark    else SurfaceLight
    val border        = if (isDark) BorderDark      else BorderLight
    val textPrimary   = if (isDark) Color.White     else TextPrimary
    val textSecondary = TextSecondary

    LaunchedEffect(Unit) { homeViewModel.dashboardData() }

    val uiState by homeViewModel.state.collectAsState()
    var toast by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is HomeUiState.Error  -> toast = ToastMessage(
                (uiState as HomeUiState.Error).message, ToastType.ERROR
            )
            else -> Unit
        }
    }

    Scaffold(
        containerColor = background,
        bottomBar = {
            BottomNavBar(
                selectedTab   = 0,
                navController = navController,
                surface       = surface,
                border        = border,
                textPrimary   = textPrimary,
                isDark        = isDark
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                HeaderSection(
                    surface       = surface,
                    textSecondary = textSecondary,
                    navController = navController,
                )
                // ─────────────────────────────────────────────────────────────

                Spacer(modifier = Modifier.height(UiScale.sectionTopGap))

                when (val state = uiState) {
                    is HomeUiState.Loading, HomeUiState.Idle -> {
                        HomeScreenSkeleton(
                            surface  = surface,
                            isDark   = isDark,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    is HomeUiState.Error -> {
                        ErrorStateContent(
                            errorMessage = state.message,
                            onRetry      = { homeViewModel.dashboardData() },
                            isDark       = isDark
                        )
                    }

                    is HomeUiState.Loaded -> {
                        val dashboard = state.dashboard

                        DailySummaryCard(
                            surface       = surface,
                            border        = border,
                            textPrimary   = textPrimary,
                            textSecondary = textSecondary,
                            dashboard     = dashboard,
                            navController = navController,
                            modifier      = Modifier.padding(horizontal = UiScale.screenPadding)
                        )

                        Spacer(modifier = Modifier.height(UiScale.sectionGap))

                        TodoSection(
                            todos         = dashboard.todos,
                            surface       = surface,
                            textPrimary   = textPrimary,
                            textSecondary = textSecondary,
                            isDark        = isDark,
                            navController = navController,
                            modifier      = Modifier.padding(horizontal = UiScale.screenPadding)
                        )

                        Spacer(modifier = Modifier.height(UiScale.sectionGap))

                        TodayExpensesSection(
                            expenses = dashboard.recentExpenses,
                            surface = surface,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark,
                            navController = navController,
                            modifier = Modifier.padding(horizontal = UiScale.screenPadding),
                            expenseViewModel = expenseViewModel
                        )

                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }
            }

            CustomToast(
                toast     = toast,
                onDismiss = { toast = null },
                modifier  = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = paddingValues.calculateTopPadding() + 8.dp)
            )
        }
    }
}

@Composable
fun HomeScreenSkeleton(
    surface  : Color,
    isDark   : Boolean,
    modifier : Modifier = Modifier
) {
    val shimmerAlpha by rememberInfiniteTransition(label = "home_sk")
        .animateFloat(
            initialValue  = 0.35f,
            targetValue   = 0.75f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label         = "alpha"
        )
    val skColor = (if (isDark) Color(0xFF2A3347) else Color(0xFFE2E8F0)).copy(alpha = shimmerAlpha)
    val surfaceColor = if (isDark) SurfaceDark else Color.White

    Column(modifier = modifier.padding(horizontal = UiScale.screenPadding)) {

        // ── 1. Daily Summary Card skeleton ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(UiScale.cardCorner))
                .background(surfaceColor)
                .padding(UiScale.cardPadding)
        ) {
            Column {
                // "DAILY SUMMARY" label + "View History" button row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(130.dp).height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(skColor)
                    )
                    Box(
                        Modifier
                            .width(90.dp).height(28.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(skColor)
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Ring + two stat columns
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Circular ring placeholder
                    Box(
                        Modifier
                            .size(UiScale.progressRingSize)
                            .clip(CircleShape)
                            .background(skColor)
                    )

                    // Stat column
                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // "Spent Today" label
                        Box(Modifier.width(70.dp).height(11.dp).clip(RoundedCornerShape(6.dp)).background(skColor))
                        Spacer(Modifier.height(6.dp))
                        // Amount
                        Box(Modifier.width(100.dp).height(26.dp).clip(RoundedCornerShape(8.dp)).background(skColor))

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = skColor, thickness = 1.dp)
                        Spacer(Modifier.height(14.dp))

                        // "Daily Remaining" label
                        Box(Modifier.width(90.dp).height(11.dp).clip(RoundedCornerShape(6.dp)).background(skColor))
                        Spacer(Modifier.height(6.dp))
                        // Amount
                        Box(Modifier.width(110.dp).height(26.dp).clip(RoundedCornerShape(8.dp)).background(skColor))
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = skColor, thickness = 1.dp)
                Spacer(Modifier.height(14.dp))

                // Monthly / Weekly / Daily row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(3) {
                        Column(
                            horizontalAlignment = when (it) {
                                0 -> Alignment.Start
                                1 -> Alignment.CenterHorizontally
                                else -> Alignment.End
                            },
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.width(70.dp).height(11.dp).clip(RoundedCornerShape(6.dp)).background(skColor))
                            Box(Modifier.width(80.dp).height(15.dp).clip(RoundedCornerShape(7.dp)).background(skColor))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(UiScale.sectionGap))

        // ── 2. Todo section skeleton ──────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(Modifier.width(90.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).background(skColor))
            Box(Modifier.width(55.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(skColor))
        }

        Spacer(Modifier.height(10.dp))

        repeat(3) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(UiScale.expenseIconBox).clip(RoundedCornerShape(14.dp)).background(skColor))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(skColor))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.35f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(skColor))
                }
                Box(Modifier.width(60.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(skColor))
            }
            if (it < 2) Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(UiScale.sectionGap))

        // ── 3. Today's Expenses section skeleton ──────────────────────────
        // Header row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(Modifier.width(150.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).background(skColor))
            Box(Modifier.width(55.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(skColor))
        }

        Spacer(Modifier.height(10.dp))

        // 3 expense row placeholders
        repeat(3) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(UiScale.expenseIconBox).clip(RoundedCornerShape(14.dp)).background(skColor))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(skColor))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.35f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(skColor))
                }
                Box(Modifier.width(60.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(skColor))
            }
            if (it < 2) Spacer(Modifier.height(4.dp))
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────
// NOTE: HomeDashboard no longer carries a userName, so the greeting is generic.

@Composable
fun HeaderSection(
    surface       : Color,
    textSecondary : Color,
    navController : NavController,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LogoIcon(navController = navController)
            Text(
                text       = "Welcome back!",
                fontSize   = 20.sp,
                fontWeight = FontWeight.W900,
                color      = if (isDark) Color.White else TextPrimary
            )
        }

        Surface(
            shape           = CircleShape,
            color           = surface,
            shadowElevation = 2.dp,
            tonalElevation  = 0.dp
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector        = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint               = if (isDark) Color.White else textSecondary,
                        modifier           = Modifier.size(30.dp)
                    )
                }
                if (Appstate.notificationEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White else Primary)
                            .border(width = 1.5.dp, color = surface, shape = CircleShape)
                    )
                }
            }
        }
    }
}

// ─── Daily Summary Card ───────────────────────────────────────────────────────

@Composable
fun DailySummaryCard(
    surface       : Color,
    border        : Color,
    textPrimary   : Color,
    textSecondary : Color,
    dashboard     : HomeDashboard,
    navController : NavController,
    modifier      : Modifier = Modifier
) {
    val spentToday   = dashboard.spentToday
    val dailyLimit   = dashboard.dailyLimit
    val weeklyBudget = dashboard.weeklyBudget
    val monthlyBudget = dashboard.monthlyBudget

    val remainingDaily = dailyLimit - spentToday
    val isOverLimit    = dailyLimit > 0.0 && spentToday > dailyLimit
    val overBy         = spentToday - dailyLimit

    val progressPct    = if (dailyLimit > 0)
        ((spentToday / dailyLimit) * 100f).toFloat().coerceIn(0f, 100f)
    else 0f

    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(UiScale.cardCorner),
        color           = surface,
        shadowElevation = 2.dp,
        tonalElevation  = 0.dp
    ) {
        Column(modifier = Modifier.padding(UiScale.cardPadding)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text          = "DAILY SUMMARY",
                    fontSize      = UiScale.body,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color         = textSecondary
                )
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = if (isDark) Color.White.copy(alpha = 0.1f) else PrimaryLight,
                    modifier = Modifier.clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { navController.navigate("history") { launchSingleTop = true } }
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = "View History",
                            fontSize   = UiScale.small,
                            fontWeight = FontWeight.Bold,
                            color      = Primary
                        )
                        Icon(
                            imageVector        = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint               = Primary,
                            modifier           = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                CircularProgress(
                    value       = progressPct,
                    size        = UiScale.progressRingSize,
                    strokeWidth = 12.dp
                )
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Column {
                        Text("Spent Today", fontSize = UiScale.small, fontWeight = FontWeight.Medium, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("₹${"%.2f".format(spentToday)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = border, thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))
                    Column {
                        Text("Daily Remaining", fontSize = UiScale.small, fontWeight = FontWeight.Medium, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text       = "₹${"%.2f".format(remainingDaily)}",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (remainingDaily < 0) Color(0xFFDC2626) else Primary
                        )
                    }
                }
            }

            // ── Inline over-limit note (no close button, always visible when breached) ──
            if (isOverLimit) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF1F0))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Text(
                        text       = "You're ₹${"%.2f".format(overBy)} over your daily limit today.",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color(0xFFB91C1C),
                        lineHeight = 17.sp
                    )
                }
            }
            // ─────────────────────────────────────────────────────────────────

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = border, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            // ── Monthly Budget / Weekly Budget / Daily Limit ──────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Monthly Budget", fontSize = 12.sp, color = textSecondary)
                    Text("₹${"%.2f".format(monthlyBudget)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Weekly Budget", fontSize = 12.sp, color = textSecondary)
                    Text("₹${"%.2f".format(weeklyBudget)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Daily Limit", fontSize = 12.sp, color = textSecondary)
                    Text("₹${"%.2f".format(dailyLimit)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                }
            }
        }
    }
}

// ─── Todo Section ──────────────────────────────────────────────────────────────
// Replaces the old Savings Goal banner + planner-based "Today's Budget" section.

@Composable
fun TodoSection(
    todos         : List<TodoEntity>,
    surface       : Color,
    textPrimary   : Color,
    textSecondary : Color,
    isDark        : Boolean,
    navController : NavController,
    modifier      : Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Todos",
                fontSize   = UiScale.title,
                fontWeight = FontWeight.Bold,
                color      = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (todos.isEmpty()) {
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("todos") { launchSingleTop = true } },
                shape           = RoundedCornerShape(UiScale.expenseCardCorner),
                color           = surface,
                shadowElevation = 1.dp,
                tonalElevation  = 0.dp
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Wallet,
                        contentDescription = null,
                        tint               = TextSecondary,
                        modifier           = Modifier.size(36.dp)
                    )
                    Text(text = "No todos yet", color = textSecondary, fontSize = 14.sp)
                    Text(
                        text       = "Tap to add a todo",
                        color      = Primary,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            val done        = todos.count { it.checkBox }
            val total       = todos.size
            val doneAmount  = todos.filter { it.checkBox }.sumOf { it.Amount }.toLong()
            val totalAmount = todos.sumOf { it.Amount }.toLong()

            Surface(
                modifier        = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape           = RoundedCornerShape(12.dp),
                color           = if (isDark) Color(0xFF1A2A4A) else Color(0xFFEEF2FF),
                shadowElevation = 0.dp,
                tonalElevation  = 0.dp
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("$done / $total done", fontSize = UiScale.small, fontWeight = FontWeight.SemiBold, color = Primary)
                    Text(
                        "₹$doneAmount / ₹$totalAmount",
                        fontSize   = UiScale.small,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isDark) Color.White else TextPrimary
                    )
                }
            }

            todos.forEach { todo ->
                TodoCard(
                    todo          = todo,
                    surface       = surface,
                    textPrimary   = textPrimary,
                    textSecondary = textSecondary,
                    isDark        = isDark,
                    onClick       = { navController.navigate("todo") { launchSingleTop = true } }
                )
                Spacer(modifier = Modifier.height(UiScale.itemGap))
            }
        }
    }
}

// ─── Todo Card ─────────────────────────────────────────────────────────────────

@Composable
private fun TodoCard(
    todo          : TodoEntity,
    surface       : Color,
    textPrimary   : Color,
    textSecondary : Color,
    isDark        : Boolean,
    onClick       : () -> Unit
) {
    val done = todo.checkBox

    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape           = RoundedCornerShape(UiScale.expenseCardCorner),
        color           = surface,
        shadowElevation = 2.dp,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiScale.expenseRowPadding, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(UiScale.expenseIconBox)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (done)
                                if (isDark) Color(0xFF0D3320) else Color(0xFFDCFCE7)
                            else
                                if (isDark) Color(0xFF1A2A4A) else Color(0xFFEEF2FF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = if (done) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint               = if (done) Color(0xFF16A34A) else Primary,
                        modifier           = Modifier.size(UiScale.expenseIcon)
                    )
                }

                Column {
                    Text(
                        text           = todo.Name,
                        fontSize       = 17.sp,
                        fontWeight     = FontWeight.Bold,
                        color          = if (done) Color(0xFF16A34A) else textPrimary,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text       = if (todo.quantity > 1) "Qty ${todo.quantity} • ${if (done) "Completed" else "Pending"}"
                        else if (done) "Completed" else "Pending",
                        fontSize   = UiScale.small,
                        fontWeight = FontWeight.Medium,
                        color      = if (done) Color(0xFF16A34A) else textSecondary
                    )
                }
            }

            Text(
                text           = "₹${"%.2f".format(todo.Amount)}",
                fontSize       = 17.sp,
                fontWeight     = FontWeight.Bold,
                color          = if (done) Color(0xFF16A34A) else Primary,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}

// ─── Today's Expenses Section ─────────────────────────────────────────────────

@Composable
fun TodayExpensesSection(
    expenses      : List<ExpenseEntity>,
    surface       : Color,
    textPrimary   : Color,
    textSecondary : Color,
    isDark        : Boolean,
    navController : NavController,
    modifier      : Modifier = Modifier,
    expenseViewModel: ExpenseViewModel
) {
    Column(modifier = modifier) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Today's Expenses",
                fontSize   = UiScale.title,
                fontWeight = FontWeight.Bold,
                color      = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (expenses.isEmpty()) {
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("history") { launchSingleTop = true } },
                shape           = RoundedCornerShape(UiScale.expenseCardCorner),
                color           = surface,
                shadowElevation = 1.dp,
                tonalElevation  = 0.dp
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Receipt,
                        contentDescription = null,
                        tint               = TextSecondary,
                        modifier           = Modifier.size(36.dp)
                    )
                    Text(text = "No expenses today",   color = textSecondary, fontSize = 14.sp)
                    Text(
                        text       = "Tap to view history →",
                        color      = Primary,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            expenses.forEach { expense ->
                ExpenseCard(
                    expense       = expense,
                    surface       = surface,
                    textPrimary   = textPrimary,
                    textSecondary = textSecondary,
                    isDark        = isDark,
                    onClick = {
                        expense.id?.let { id ->
                            expenseViewModel.loadExpenseById(id)
                            navController.navigate("detailScreen")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(UiScale.itemGap))
            }
        }
    }
}

// ─── Expense Card ─────────────────────────────────────────────────────────────

@Composable
private fun ExpenseCard(
    expense       : ExpenseEntity,
    surface       : Color,
    textPrimary   : Color,
    textSecondary : Color,
    isDark        : Boolean,
    onClick       : () -> Unit
) {
    val meta = categoryMeta(expense.category)

    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape           = RoundedCornerShape(UiScale.expenseCardCorner),
        color           = surface,
        shadowElevation = 2.dp,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(UiScale.expenseRowPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(UiScale.expenseIconBox)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) meta.bgDark else meta.bgLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = meta.icon,
                        contentDescription = null,
                        tint               = meta.tint,
                        modifier           = Modifier.size(UiScale.expenseIcon)
                    )
                }
                Column {
                    Row() {
                        Text(
                            text       = expense.category,
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color      = textPrimary
                        )
//                        Spacer(Modifier.padding(horizontal = 40.dp))
//                        Text(
//                            text = "-₹${"%.2f".format(expense.amount).take(3)}..",
//                            fontSize   = 17.sp,
//                            fontWeight = FontWeight.Bold,
//                            color      = Color(0xFFDC2626)
//                        )
                    }

                    Text(
                        text = "${expense.notes.orEmpty().ifBlank { "Notes Not Available" }.take(15)}... • ${expense.time}",
                        fontSize   = UiScale.small,
                        fontWeight = FontWeight.Medium,
                        color      = textSecondary
                    )
                }
            }


        }
    }
}