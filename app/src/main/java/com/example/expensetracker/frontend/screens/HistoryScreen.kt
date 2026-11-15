package com.example.expensetracker.frontend.screens

import android.R.attr.maxHeight
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.components.BottomNavBar
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.LogoIcon
import com.example.expensetracker.frontend.components.SearchDialog
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.services.TodoService.TodoViewModel
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.services.entity.ExpenseEntity
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.BackgroundLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.Slate
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.SurfaceLight
import com.example.expensetracker.ui.theme.TextPrimary
import com.example.expensetracker.ui.theme.TextSecondary
import com.example.expensetracker.frontend.services.settingService.SettingViewModel
import com.example.expensetracker.frontend.services.settingService.SettingsEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ─── Month sections with day / week totals ────────────────────────────────────

// First day of the week. Change to DayOfWeek.SUNDAY if your weeks run Sun–Sat.
private val WEEK_START = DayOfWeek.MONDAY

// Credit-card spends are kept out of daily/weekly/monthly totals (see ExpenseEntity).
private const val CREDIT_CARD = "Credit Card"

@RequiresApi(Build.VERSION_CODES.O)
private data class WeekTotal(
    val spent: Double,
    val budgetMonth: YearMonth   // which month's settings row holds the weekly budget
)

@RequiresApi(Build.VERSION_CODES.O)
private data class DayGroup(
    val dateText: String,
    val items: List<ExpenseEntity>,
    val dayTotal: Double,
    val week: WeekTotal?         // set only on the last day of a finished week
)

@RequiresApi(Build.VERSION_CODES.O)
private data class MonthSection(
    val key: String,             // "2026-11" (or "other") – used to remember open/closed
    val month: YearMonth?,       // null = entries whose date text couldn't be read
    val title: String,
    val spent: Double,
    val days: List<DayGroup>
)

@RequiresApi(Build.VERSION_CODES.O)
private fun parseStoredDate(text: String): LocalDate? =
    runCatching { LocalDate.parse(text, DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)) }
        .recoverCatching { LocalDate.parse(text, DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())) }
        .getOrNull()

/**
 * Groups expenses by month (newest first), then by date. Each day gets its total.
 * A week summary is attached to the LAST date that has expenses in that week, once
 * the week is over — so it still shows if nothing was spent on the actual Sunday.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun buildMonthSections(expenses: List<ExpenseEntity>, today: LocalDate): List<MonthSection> {
    fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(WEEK_START))
    fun countable(items: List<ExpenseEntity>) = items.filter { it.paymentMode != CREDIT_CARD }

    val dated = expenses.groupBy { it.date }.map { (text, items) ->
        Triple(text, items, parseStoredDate(text))
    }

    val weekTotals = HashMap<LocalDate, Double>()
    val weekLast   = HashMap<LocalDate, LocalDate>()   // newest date with expenses per week

    dated.forEach { (_, items, date) ->
        if (date == null) return@forEach
        val ws = weekStart(date)
        weekTotals[ws] = (weekTotals[ws] ?: 0.0) + countable(items).sumOf { it.amount }
        if (weekLast[ws] == null || date.isAfter(weekLast[ws])) weekLast[ws] = date
    }

    val days: List<Pair<LocalDate?, DayGroup>> = dated
        .sortedByDescending { it.third ?: LocalDate.MIN }
        .map { (text, items, date) ->
            var week: WeekTotal? = null
            if (date != null) {
                val ws = weekStart(date)
                val we = ws.plusDays(6)
                if (weekLast[ws] == date && !we.isAfter(today)) {
                    week = WeekTotal(weekTotals[ws] ?: 0.0, YearMonth.from(we))
                }
            }
            date to DayGroup(text, items, countable(items).sumOf { it.amount }, week)
        }

    val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    return days
        .groupBy { (date, _) -> date?.let { YearMonth.from(it) } }
        .map { (ym, list) ->
            val groups = list.map { it.second }
            MonthSection(
                key   = ym?.toString() ?: "other",
                month = ym,
                title = ym?.format(monthTitle) ?: "Other dates",
                spent = groups.sumOf { it.dayTotal },
                days  = groups
            )
        }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    isDark: Boolean = false,
    expenseViewModel: ExpenseViewModel,
    todoViewModel: TodoViewModel,
    settingViewModel: SettingViewModel
) {
    val background = if (isDark) BackgroundDark else BackgroundLight
    val surface = if (isDark) SurfaceDark else SurfaceLight
    val border = if (isDark) BorderDark else BorderLight
    val textPrimary = if (isDark) Color.White else TextPrimary
    val textSecondary = TextSecondary

    var showSearchDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Expense History", "Todo History")
    var selectedTab by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    // ─── Refactored Unified UI State ──────────────────────────────────────────
    val expenseUiState by expenseViewModel.uiState.collectAsState()
    val expenses = expenseUiState.expenses
    val isLoading = expenseUiState.isLoading
    val errorMessage = expenseUiState.errorMessage

    var toast by remember { mutableStateOf<ToastMessage?>(null) }

    // Month sections (newest first) with day totals and week summaries
    val monthSections = remember(expenses) { buildMonthSections(expenses, LocalDate.now()) }

    // Budgets for every month a month header / week summary refers to
    val neededMonths = remember(monthSections) {
        (monthSections.mapNotNull { it.month } +
                monthSections.flatMap { sec -> sec.days.mapNotNull { it.week?.budgetMonth } }).distinct()
    }
    var budgets by remember { mutableStateOf<Map<YearMonth, SettingsEntity?>>(emptyMap()) }
    LaunchedEffect(neededMonths) {
        budgets = neededMonths.associateWith { settingViewModel.budgetFor(it.year, it.monthValue) }
    }

    // Collapsible months. `monthOpenOverride` only holds what the user has tapped;
    // months they haven't touched follow the default (current month + newest month open).
    // Saved in SharedPreferences so the choices survive leaving the screen and closing the app.
    val context = LocalContext.current
    val historyPrefs = remember { context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE) }
    val monthOpenOverride = remember {
        mutableStateMapOf<String, Boolean>().apply {
            historyPrefs.all.forEach { (k, v) ->
                if (k.startsWith("month_open_") && v is Boolean) put(k.removePrefix("month_open_"), v)
            }
        }
    }
    val defaultOpenKeys: Set<String> = remember(monthSections) {
        buildSet {
            add(YearMonth.now().toString())
            monthSections.firstOrNull()?.let { add(it.key) }
        }
    }
    fun isMonthOpen(key: String): Boolean = monthOpenOverride[key] ?: (key in defaultOpenKeys)

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            toast = ToastMessage(it, ToastType.ERROR)
            expenseViewModel.clearError()
        }
    }

    if (showSearchDialog) {
        SearchDialog(
            onDismiss = { showSearchDialog = false },
            isDark = isDark,
            expenseViewModel = expenseViewModel,
            todoViewModel = todoViewModel,
            selectedTab = selectedTab
        )
    }

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("History", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                },
                navigationIcon = {
                    Column(Modifier.padding(start = 10.dp)) {
                        LogoIcon(navController = navController)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, "Search", tint = textPrimary, modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
            )
        },
        bottomBar = {
            BottomNavBar(1, navController, surface, border, textPrimary, isDark)
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top    = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                // --- Tab bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background)
                        .padding(horizontal = 16.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                color = if (selectedTab == index) Color(0xFF1E40FF) else if (isDark) Color(0xFFADB5C7) else Color.Gray,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (selectedTab == index) Color(0xFF1E40FF) else Color.Transparent)
                            )
                        }
                    }
                }
                HorizontalDivider(color = if (isDark) BorderDark else Color.LightGray, thickness = 0.5.dp)

                // --- Tab content ---
                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedTab == 0) {
                        // --- EXPENSE HISTORY ---
                        if (isLoading && expenses.isEmpty()) {
                            ExpenseListSkeleton(isDark = isDark)
                        } else if (expenses.isEmpty()) {
                            EmptyHistoryState(isDark = isDark)
                        } else {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(scrollState)
                                    .padding(bottom = 16.dp)
                            ) {
                                monthSections.forEach { section ->
                                    val isOpen = isMonthOpen(section.key)
                                    MonthHeader(
                                        title       = section.title,
                                        spent       = section.spent,
                                        budget      = section.month?.let { budgets[it]?.monthlyBudget },
                                        expanded    = isOpen,
                                        isDark      = isDark,
                                        textPrimary = textPrimary,
                                        // read the CURRENT state when tapped, never a captured copy
                                        onToggle    = {
                                            val nowOpen = !isMonthOpen(section.key)
                                            monthOpenOverride[section.key] = nowOpen
                                            historyPrefs.edit().putBoolean("month_open_${section.key}", nowOpen).apply()
                                        }
                                    )
                                    if (isOpen) {
                                        Column {
                                            section.days.forEach { group ->
                                                DateHeader(
                                                    group       = group,
                                                    budgets     = budgets,
                                                    textPrimary = textPrimary
                                                )
                                                group.items.forEach { expense ->
                                                    ExpenseTransactionRow(
                                                        expense = expense,
                                                        background = background,
                                                        textPrimary = textPrimary,
                                                        textSecondary = textSecondary,
                                                        isDark = isDark,
                                                        onClick = {
                                                            expense.id?.let { id ->
                                                                expenseViewModel.loadExpenseById(id)
                                                                navController.navigate("detailScreen")
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // End-of-list chip
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(
                                            modifier  = Modifier.weight(1f),
                                            color     = if (isDark) Color(0xFF2A3347) else Color(0xFFE5E7EB),
                                            thickness = 1.dp
                                        )
                                        Text(
                                            text       = "All ${expenses.size} transactions loaded",
                                            fontSize   = 12.sp,
                                            color      = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        HorizontalDivider(
                                            modifier  = Modifier.weight(1f),
                                            color     = if (isDark) Color(0xFF2A3347) else Color(0xFFE5E7EB),
                                            thickness = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // --- TODO HISTORY ---
                        TodoHistoryContent(
                            background = background,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            viewModel = todoViewModel
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

// ─── Expense list skeleton ────────────────────────────────────────────────────
@Composable
fun ExpenseListSkeleton(isDark: Boolean) {
    val alpha by rememberInfiniteTransition(label = "exp_sk")
        .animateFloat(
            initialValue  = 0.35f,
            targetValue   = 0.75f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label         = "a"
        )
    val skColor = (if (isDark) Color(0xFF2A3347) else Color(0xFFE2E8F0)).copy(alpha = alpha)
    val cardSurface = if (isDark) SurfaceDark else Color.White

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val rowHeight = 77.dp
        val rowCount  = ((maxHeight / rowHeight).toInt() + 1).coerceAtLeast(6)

        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(rowCount) { idx ->
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDark) 0.dp else 2.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = cardSurface)
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(45.dp).clip(CircleShape).background(skColor))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Box(
                                    Modifier
                                        .width(if (idx % 3 == 0) 110.dp else if (idx % 3 == 1) 90.dp else 100.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(skColor)
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    Modifier
                                        .width(if (idx % 2 == 0) 80.dp else 68.dp)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(skColor)
                                )
                            }
                        }
                        Box(
                            Modifier
                                .width(if (idx % 2 == 0) 60.dp else 52.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(skColor)
                        )
                    }
                    HorizontalDivider(
                        color     = if (isDark) BorderDark else Color.LightGray,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyHistoryState(isDark: Boolean) {
    val iconBg = if (isDark) Color(0xFF1E2A3A) else Color(0xFFF0F4FF)
    val msgColor = if (isDark) Color(0xFFADB5C7) else TextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            text = "No transactions yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else TextPrimary
        )
        Text(
            text = "Your expenses will appear here after you add them.",
            fontSize = 14.sp,
            color = msgColor,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ─── Single transaction row ───────────────────────────────────────────────────
@Composable
private fun ExpenseTransactionRow(
    expense: ExpenseEntity,
    background: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val isDarkMode = background == BackgroundDark
    val cardSurface = if (isDarkMode) SurfaceDark else Color.White
    val iconBoxBg = if (isDarkMode) Color(0xFF2A2D3E) else Color(0xFFDEDEE0)
    val iconTint = if (isDarkMode) Color(0xFFADB5C7) else Color.DarkGray
    val subtitleColor = if (isDarkMode) Color(0xFFADB5C7).copy(alpha = 0.85f)
    else Color.Black.copy(alpha = 0.7f)

    val amountColor = Color(0xFFDC2626)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(iconBoxBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = expense.category.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = expense.category,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "• ${expense.contactName.ifBlank { "No contact" }}",
                        color = subtitleColor,
                        fontSize = 13.sp
                    )
                }
            }

            Text(
                text = "-₹${"%.2f".format(expense.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (isDarkMode) {
            HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
        }
    }
}

// ─── Amount helpers ───────────────────────────────────────────────────────────
/** 1250.0 -> "1250", 1250.5 -> "1250.50" */
private fun formatAmt(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v)

/** "1250/1300" = spent/budget; just "1250" when no budget is set. */
private fun spentOfBudget(spent: Double, budget: Double?): String =
    if (budget != null && budget > 0.0) "${formatAmt(spent)}/${formatAmt(budget)}" else formatAmt(spent)

/** Red when over budget, green when within it, neutral when there is no budget. */
private fun budgetColor(spent: Double, budget: Double?, neutral: Color): Color = when {
    budget == null || budget <= 0.0 -> neutral
    spent > budget                  -> Color(0xFFDC2626)
    else                            -> Color(0xFF16A34A)
}

// ─── Month header (tap to collapse / expand): month left, spent/budget right ──
@Composable
private fun MonthHeader(
    title: String,
    spent: Double,
    budget: Double?,
    expanded: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "month_chevron")
    val bg = if (isDark) Color(0xFF1A2233) else Color(0xFFEEF1F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = TextSecondary,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = spentOfBudget(spent, budget),
            color = budgetColor(spent, budget, textPrimary),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

// ─── Date header: date on the left, day total (and week spent/budget) on the right
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DateHeader(
    group: DayGroup,
    budgets: Map<YearMonth, SettingsEntity?>,
    textPrimary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = group.dateText.uppercase(),
            color = Slate,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${formatAmt(group.dayTotal)}",
                color = textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            // Only on the last day of a finished week
            group.week?.let { w ->
                val budget = budgets[w.budgetMonth]?.weeklyBudget
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Week ${spentOfBudget(w.spent, budget)}",
                    color = budgetColor(w.spent, budget, TextSecondary),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─── Section header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(text: String, textSize: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text.uppercase(),
            color = Slate,
            fontWeight = FontWeight.W500,
            fontSize = textSize.sp
        )
    }
}