package com.example.expensetracker.frontend.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.ErrorStateContent
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.services.TodoService.TodoCategory
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import com.example.expensetracker.frontend.services.TodoService.TodoHistoryUiState
import com.example.expensetracker.frontend.services.TodoService.TodoViewModel
import com.example.expensetracker.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

// ─── UI model: one row in the list ───────────────────────────────────────────

data class TodoEntry(
    val day       : String,
    val month     : String,   // "OCT"
    val year      : String,   // "2024"
    val name      : String,
    val cost      : String,   // "₹1,200"
    val quantity  : Int,
    val isDone    : Boolean,
    val category  : TodoCategory? = null
)

// ─── UI model: one week block ─────────────────────────────────────────────────

data class TodoWeekSummary(
    val weekLabel      : String,
    val totalAmount    : Double,
    val totalItems     : Int,
    val completedItems : Int,
    val entries        : List<TodoEntry>
)

// ─── Main composable ──────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TodoHistoryContent(
    background    : Color,
    textPrimary   : Color,
    textSecondary : Color,
    viewModel     : TodoViewModel
) {
    val isDark      = background == BackgroundDark
    val cardSurface = if (isDark) SurfaceDark else Color.White
    val iconBoxBg   = if (isDark) Color(0xFF2A2D3E) else Color(0xFFF0F2F5)

    val historyState by viewModel.historyState.collectAsState()

    var allItems      by remember { mutableStateOf<List<TodoEntity>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore       by remember { mutableStateOf(true) }
    var currentOffset by remember { mutableStateOf(0) }
    var toast         by remember { mutableStateOf<ToastMessage?>(null) }
    var initialLoadFailed by remember { mutableStateOf(false) }
    val pageSize  = 20
    val listState = rememberLazyListState()

    // Recomputed every time the accumulated item list changes — cheap since it's local data.
    val weeks = remember(allItems) { allItems.toWeekSummaries() }

    // ── Initial load ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.loadHistoryPage(limit = pageSize, offset = 0)
    }

    // ── Infinite scroll ───────────────────────────────────────────────────────
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total       = listState.layoutInfo.totalItemsCount
            !isLoadingMore && hasMore && total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            isLoadingMore = true
            viewModel.loadHistoryPage(limit = pageSize, offset = currentOffset)
        }
    }

    // ── React to ViewModel ────────────────────────────────────────────────────
    LaunchedEffect(historyState) {
        when (val s = historyState) {
            is TodoHistoryUiState.Loaded -> {
                isLoading     = false
                isLoadingMore = false
                allItems      = if (s.requestedOffset == 0) s.items else allItems + s.items
                hasMore       = s.items.size >= pageSize
                currentOffset = allItems.size
            }
            is TodoHistoryUiState.Error -> {
                isLoading     = false
                isLoadingMore = false
                if (allItems.isEmpty()) {
                    initialLoadFailed = true  // first load failed, nothing to show
                } else {
                    toast = ToastMessage(s.message, ToastType.ERROR)  // pagination error, data still visible
                }
            }
            else -> Unit
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {

        when {
            // First-load skeleton
            isLoading && weeks.isEmpty() -> {
                TodoHistorySkeleton(isDark = isDark, cardSurface = cardSurface)
            }
            initialLoadFailed && weeks.isEmpty() -> {
                ErrorStateContent(
                    errorMessage = "Failed to load todo history.",
                    onRetry      = {
                        initialLoadFailed = false
                        isLoading         = true
                        currentOffset     = 0
                        allItems          = emptyList()
                        viewModel.loadHistoryPage(limit = pageSize, offset = 0)
                    },
                    isDark   = isDark,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Empty state
            !isLoading && weeks.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🗂", fontSize = 40.sp)
                        Text("No todo history yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text("Your past todos will appear here.", fontSize = 13.sp, color = textSecondary)
                    }
                }
            }

            // List
            else -> {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    weeks.forEach { week ->

                        // Week summary card
                        item(key = "header_${week.weekLabel}") {
                            TodoWeekHeaderCard(
                                week        = week,
                                isDark      = isDark,
                                cardSurface = cardSurface,
                                textPrimary = textPrimary
                            )
                        }

                        // Group tasks by day inside the week
                        val byDay = week.entries
                            .groupBy { "${it.day} ${it.month} ${it.year}" }

                        byDay.forEach { (dayLabel, dayEntries) ->
                            item(key = "day_${week.weekLabel}_$dayLabel") {
                                TodoSectionLabel(text = dayLabel, textSecondary = textSecondary)
                            }
                            items(
                                items = dayEntries,
                                key   = { "${week.weekLabel}_${it.day}_${it.name}_${it.cost}" }
                            ) { entry ->
                                TodoHistoryRow(
                                    entry       = entry,
                                    cardSurface = cardSurface,
                                    iconBoxBg   = iconBoxBg,
                                    textPrimary = textPrimary,
                                    isDark      = isDark
                                )
                            }
                        }

                        // Week separator
                        item(key = "sep_${week.weekLabel}") {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(
                                color     = if (isDark) BorderDark else Color(0xFFE5E7EB),
                                thickness = 1.dp,
                                modifier  = Modifier.padding(horizontal = 18.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // Pagination spinner
                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), Alignment.Center) {
                                CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }

        // Toast
        CustomToast(
            toast     = toast,
            onDismiss = { toast = null },
            modifier  = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        )
    }
}

// ─── Todo history skeleton ─────────────────────────────────────────────────────
@Composable
fun TodoHistorySkeleton(isDark: Boolean, cardSurface: Color) {
    val alpha by rememberInfiniteTransition(label = "th_sk")
        .animateFloat(
            initialValue  = 0.35f,
            targetValue   = 0.75f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label         = "a"
        )
    val skColor = (if (isDark) Color(0xFF2A3347) else Color(0xFFE2E8F0)).copy(alpha = alpha)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val groupHeight = 342.dp
        val groupCount  = ((maxHeight / groupHeight).toInt() + 1).coerceAtLeast(2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            repeat(groupCount) { groupIdx ->

                Surface(
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    shape           = RoundedCornerShape(16.dp),
                    color           = cardSurface,
                    shadowElevation = 2.dp,
                    tonalElevation  = 0.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .width(if (groupIdx % 2 == 0) 130.dp else 118.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(skColor)
                            )
                            Box(
                                Modifier
                                    .width(72.dp).height(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(skColor)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .fillMaxWidth().height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(skColor)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column {
                                Box(Modifier.width(36.dp).height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)).background(skColor))
                                Spacer(Modifier.height(5.dp))
                                Box(Modifier.width(60.dp).height(14.dp)
                                    .clip(RoundedCornerShape(7.dp)).background(skColor))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Box(Modifier.width(36.dp).height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)).background(skColor))
                                Spacer(Modifier.height(5.dp))
                                Box(Modifier.width(60.dp).height(14.dp)
                                    .clip(RoundedCornerShape(7.dp)).background(skColor))
                            }
                        }
                    }
                }

                repeat(3) { rowIdx ->
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(0.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDark) 0.dp else 1.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardSurface)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp, horizontal = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(skColor)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Box(
                                        Modifier
                                            .width(if (rowIdx % 3 == 0) 110.dp else if (rowIdx % 3 == 1) 90.dp else 100.dp)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(skColor)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        Modifier.width(52.dp).height(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(skColor)
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .width(if (rowIdx % 2 == 0) 52.dp else 44.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(skColor)
                            )
                        }
                        HorizontalDivider(
                            color     = if (isDark) BorderDark else Color(0xFFF0F0F5),
                            thickness = 1.dp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color     = if (isDark) BorderDark else Color(0xFFE5E7EB),
                    thickness = 1.dp,
                    modifier  = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ─── Week header card ─────────────────────────────────────────────────────────

@Composable
fun TodoWeekHeaderCard(
    week        : TodoWeekSummary,
    isDark      : Boolean,
    cardSurface : Color,
    textPrimary : Color
) {
    val progress   = if (week.totalItems > 0)
        (week.completedItems.toFloat() / week.totalItems).coerceIn(0f, 1f) else 0f
    val allDone    = week.totalItems > 0 && week.completedItems == week.totalItems
    val badgeBg    = if (allDone) Color(0xFFDCFCE7) else Color(0xFFEEF2FF)
    val badgeColor = if (allDone) Color(0xFF16A34A) else Primary
    val badgeLabel = "${week.completedItems}/${week.totalItems} done"
    val barColor   = if (allDone) Color(0xFF16A34A) else Primary

    Surface(
        modifier        = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        shape           = RoundedCornerShape(16.dp),
        color           = cardSurface,
        shadowElevation = 2.dp,
        tonalElevation  = 0.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(week.weekLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(badgeBg).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(badgeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color      = barColor,
                trackColor = if (isDark) Color(0xFF2A3347) else Color(0xFFE5E7EB)
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Items",  fontSize = 11.sp, color = TextSecondary)
                    Text("${week.totalItems}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total value", fontSize = 11.sp, color = TextSecondary)
                    Text("₹${"%,.0f".format(week.totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            }
        }
    }
}

// ─── Day section label ────────────────────────────────────────────────────────

@Composable
fun TodoSectionLabel(text: String, textSecondary: Color) {
    Text(
        text          = text,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.Medium,
        color         = textSecondary,
        letterSpacing = 0.5.sp,
        modifier      = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 6.dp)
    )
}

// ─── Todo row ─────────────────────────────────────────────────────────────────

@Composable
fun TodoHistoryRow(
    entry       : TodoEntry,
    cardSurface : Color,
    iconBoxBg   : Color,
    textPrimary : Color,
    isDark      : Boolean
) {
    val amountColor = if (entry.isDone) Color(0xFF1D9E75) else TextSecondary

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        colors    = CardDefaults.cardColors(containerColor = cardSurface)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier         = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(iconBoxBg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(entry.day,   fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary, lineHeight = 16.sp)
                        Text(entry.month, fontSize = 10.sp, color = TextSecondary, lineHeight = 12.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.name,
                        fontSize       = 15.sp,
                        fontWeight     = FontWeight.SemiBold,
                        color          = if (entry.isDone) TextSecondary else textPrimary,
                        textDecoration = if (entry.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.category?.let { cat ->
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(cat.bg).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${cat.emoji} ${cat.label}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cat.color)
                            }
                        }
                        if (entry.quantity > 1) {
                            Text("×${entry.quantity}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(entry.cost, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = amountColor)
        }
        if (isDark) HorizontalDivider(color = BorderDark, thickness = 1.dp)
    }
}

// ─── Mapper ───────────────────────────────────────────────────────────────────
// Groups the flat, locally-accumulated TodoEntity list into Mon–Sun week buckets,
// the same shape the old server-driven BudgetHistory produced.

@RequiresApi(Build.VERSION_CODES.O)
private fun List<TodoEntity>.toWeekSummaries(): List<TodoWeekSummary> {
    val displayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val yearFmt    = DateTimeFormatter.ofPattern("yyyy", Locale.getDefault())
    val weekFields = WeekFields.of(Locale.getDefault())

    val byWeekStart = this.groupBy { todo ->
        val date = todo.toLocalDate()
        date.minusDays((date.dayOfWeek.value - 1).toLong())   // Monday of that todo's week
    }

    return byWeekStart.entries
        .sortedByDescending { it.key }   // newest week first
        .map { (weekStart, todosInWeek) ->
            val weekEnd = weekStart.plusDays(6)
            val weekLabel = "${weekStart.format(displayFmt)} – ${weekEnd.format(displayFmt)} ${weekEnd.format(yearFmt)}"

            val entries = todosInWeek
                .map { todo ->
                    val date = todo.toLocalDate()
                    TodoEntry(
                        day      = date.dayOfMonth.toString(),
                        month    = date.month.name.take(3),
                        year     = date.year.toString(),
                        name     = todo.Name,
                        cost     = "₹${"%,.0f".format(todo.Amount)}",
                        quantity = todo.quantity,
                        isDone   = todo.checkBox,
                        category = todo.categoryEnum
                    )
                }
                .sortedWith(compareBy { "${it.year}${it.month}${it.day.padStart(2, '0')}" })

            TodoWeekSummary(
                weekLabel      = weekLabel,
                totalAmount    = todosInWeek.sumOf { it.Amount },
                totalItems     = todosInWeek.size,
                completedItems = todosInWeek.count { it.checkBox },
                entries        = entries
            )
        }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun TodoEntity.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()