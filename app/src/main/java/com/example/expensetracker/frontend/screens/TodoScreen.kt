package com.example.expensetracker.frontend.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.example.expensetracker.frontend.components.BottomNavBar
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.LogoIcon
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.services.TodoService.TodoCategory
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import com.example.expensetracker.frontend.services.TodoService.TodoViewModel
import com.example.expensetracker.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ─── Helpers ──────────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
private fun TodoEntity.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

// ─── Main Screen ──────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoPlanner(
    onDismiss    : () -> Unit,
    navController: NavController,
    isDark       : Boolean = false,
    viewModel    : TodoViewModel
) {
    val background  = if (isDark) BackgroundDark else BackgroundLight
    val surface     = if (isDark) SurfaceDark    else SurfaceLight
    val border      = if (isDark) BorderDark     else BorderLight
    val textPrimary = if (isDark) Color.White    else TextPrimary

    // ── Live, fully reactive data — no manual re-fetching needed ───────────────
    val allTodos by viewModel.todos.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var toast by remember { mutableStateOf<ToastMessage?>(null) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { toast = ToastMessage(it, ToastType.ERROR); viewModel.clearError() }
    }

    var activeCategories by remember { mutableStateOf(setOf<TodoCategory>()) }

    val filteredTodos = remember(allTodos, activeCategories) {
        allTodos
            .filter { activeCategories.isEmpty() || it.categoryEnum in activeCategories }
    }

    val today = LocalDate.now()

    // All todos that fall in the CURRENT month, newest first — replaces the old 7-day week bucketing
    val monthTasks = remember(filteredTodos, today) {
        filteredTodos
            .filter { it.toLocalDate().year == today.year && it.toLocalDate().monthValue == today.monthValue }
            .sortedByDescending { it.toLocalDate() }
    }

    val totalItems     = monthTasks.size
    val completedItems = monthTasks.count { it.checkBox }
    val totalValue      = monthTasks.sumOf { it.Amount }

    // ── Sheets ──────────────────────────────────────────────────────────────────
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showEditSheet by remember { mutableStateOf(false) }
    var editingTask   by remember { mutableStateOf<TodoEntity?>(null) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val monthYear = today.format(DateTimeFormatter.ofPattern("MMMM", LocalLocale.current.platformLocale))

    if (showAddSheet) {
        AddTodoBottomSheet(
            isDark     = isDark,
            sheetState = sheetState,
            monthDate  = today,
            onDismiss  = { showAddSheet = false },
            onAdd      = { entity ->
                viewModel.insertTodo(entity) {
                    toast = ToastMessage("Todo added ✓", ToastType.SUCCESS)
                }
                showAddSheet = false
            }
        )
    }

    if (showEditSheet && editingTask != null) {
        EditTodoBottomSheet(
            isDark     = isDark,
            sheetState = editSheetState,
            task       = editingTask!!,
            onDismiss  = { showEditSheet = false; editingTask = null },
            onSave     = { updated ->
                viewModel.updateTodo(updated)
                toast = ToastMessage("Todo updated ✓", ToastType.SUCCESS)
                showEditSheet = false
                editingTask   = null
            }
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Todo", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(28.dp))
                    }
                },
                navigationIcon = {
                    Column(Modifier.padding(start = 16.dp)) { LogoIcon(navController = navController) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab  = -1, navController = navController,
                surface      = surface, border = border,
                textPrimary  = textPrimary, isDark = isDark
            )
        }
    ) { paddingValues ->

        Box(Modifier.fillMaxSize()) {

            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Never Forgot Anything",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible // or Ellipsis/Clip
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    ) {
                        Box(Modifier.width(4.dp).height(16.dp).clip(CircleShape).background(Primary))
                        Spacer(Modifier.width(8.dp))
                        Text("with todo.", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    }

                    TodoOverviewCard(
                        isDark         = isDark,
                        textPrimary    = textPrimary,
                        totalItems     = totalItems,
                        completedItems = completedItems,
                        totalValue     = totalValue
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "FILTER BY CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                        items(TodoCategory.values().size) { i ->
                            val cat = TodoCategory.values()[i]
                            CategoryChip(cat = cat, selected = cat in activeCategories) {
                                activeCategories = if (cat in activeCategories) activeCategories - cat else activeCategories + cat
                            }
                        }
                    }
                }

                // ── Single month block, replaces the old Mon → Sun loop ─────────
                item {
                    TodoMonthBlock(
                        monthDate    = today,
                        tasks        = monthTasks,
                        isDark       = isDark,
                        surface      = surface,
                        textPrimary  = textPrimary,
                        onAddClick   = { showAddSheet = true },
                        onToggleDone = { task -> viewModel.toggleDone(task) },
                        onDelete     = { task -> viewModel.deleteTodo(task); toast = ToastMessage("Todo deleted", ToastType.INFO) },
                        onEdit       = { task -> editingTask = task; showEditSheet = true }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                item { Spacer(Modifier.height(12.dp)) }
            }

            CustomToast(
                toast     = toast,
                onDismiss = { toast = null },
                modifier  = Modifier.align(Alignment.TopCenter).padding(top = paddingValues.calculateTopPadding() + 8.dp)
            )
        }
    }
}

// ─── Overview card — gradient hero + completion ring, now scoped to the month ──

@Composable
fun TodoOverviewCard(
    isDark        : Boolean,
    textPrimary   : Color,
    totalItems    : Int,
    completedItems: Int,
    totalValue    : Double
) {
    val progress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f
    val animatedProgress by animateFloatAsState(progress, tween(700), label = "progress")
    val pct = (progress * 100).roundToInt()

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
        tonalElevation  = 0.dp,
        color           = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = if (isDark)
                            listOf(Color(0xFF1E2A55), Color(0xFF13182E))
                        else
                            listOf(Primary, Color(0xFF3B4FD9))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f)),
                            Alignment.Center
                        ) {
                            Icon(Icons.Default.Checklist, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("This Month", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                            Text("$completedItems of $totalItems done", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(50.dp)).background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("$pct%", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(
                    progress   = { animatedProgress },
                    modifier   = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color      = Color.White,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    strokeCap  = StrokeCap.Round
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Total value ₹${"%,.0f".format(totalValue)}",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ─── Month block — one circle (today), one + button, flat task list ───────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TodoMonthBlock(
    monthDate   : LocalDate,
    tasks       : List<TodoEntity>,
    isDark      : Boolean,
    surface     : Color,
    textPrimary : Color,
    onAddClick  : () -> Unit,
    onToggleDone: (TodoEntity) -> Unit,
    onDelete    : (TodoEntity) -> Unit,
    onEdit      : (TodoEntity) -> Unit
) {
    val monthFormatter    = DateTimeFormatter.ofPattern("MMMM", LocalLocale.current.platformLocale)
    val shortDayFormatter = DateTimeFormatter.ofPattern("EEE", LocalLocale.current.platformLocale)

    val doneCount = tasks.count { it.checkBox }
    val total     = tasks.sumOf { it.Amount }

    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ── single circle: today's date number + weekday abbrev ──
                Box(Modifier.size(62.dp).clip(CircleShape).background(Primary), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(monthDate.dayOfMonth.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(monthDate.format(shortDayFormatter).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(monthDate.format(monthFormatter), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        "$doneCount/${tasks.size} done · ₹${"%,.0f".format(total)}",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary, letterSpacing = 0.3.sp
                    )
                }
            }
            // ── single + button for the whole month ──
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(if (isDark) Color(0xFF1A2A4A) else Color(0xFFC8CBD9))
                    .clickable { onAddClick() },
                Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Add todo", tint = Primary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        if (tasks.isNotEmpty()) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = surface, shadowElevation = 2.dp, tonalElevation = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    tasks.forEachIndexed { idx, task ->
                        TodoRow(
                            task        = task,
                            isDark      = isDark,
                            textPrimary = textPrimary,
                            onToggle    = { onToggleDone(task) },
                            onDelete    = { onDelete(task) },
                            onEdit      = { onEdit(task) }
                        )
                        if (idx < tasks.lastIndex) {
                            HorizontalDivider(color = if (isDark) BorderDark else Color(0xFFF0F0F5), thickness = 0.5.dp)
                        }
                    }
                }
            }
        } else {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = surface, shadowElevation = 1.dp, tonalElevation = 0.dp) {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), Alignment.Center) {
                    Text("No todos yet this month — tap + to add one", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ─── Todo row — cart-icon toggle (glows when checked), name (price/quantity), edit/delete ──

@Composable
fun TodoRow(
    task       : TodoEntity,
    isDark     : Boolean,
    textPrimary: Color,
    onToggle   : () -> Unit,
    onDelete   : () -> Unit,
    onEdit     : () -> Unit
) {
    val toggleTint by animateColorAsState(
        targetValue   = if (task.checkBox) Primary else TextSecondary.copy(alpha = 0.35f),
        animationSpec = tween(250),
        label         = "toggleTint"
    )
    val glowBg by animateColorAsState(
        targetValue   = if (task.checkBox) Primary.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(250),
        label         = "glowBg"
    )

    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {

            // ── toggle icon — shows the category emoji (or cart as fallback), glows when checked ──
            val categoryColor = task.categoryEnum?.color ?: Primary

            val glowBg by animateColorAsState(
                targetValue   = if (task.checkBox) categoryColor.copy(alpha = 0.8f) else categoryColor.copy(alpha = 0.10f),
                animationSpec = tween(250),
                label         = "glow"
            )

            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(glowBg)
                    .clickable { onToggle() },
                Alignment.Center
            ) {
                if (task.categoryEnum != null) {
                    Text(task.categoryEnum!!.emoji, fontSize = 16.sp)
                } else {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = if (task.checkBox) "Done" else "Not done",
                        tint     = toggleTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            // ── name (price/quantity) ──
            Text(
                buildString {
                    append(task.Name)
                    append("  (₹${"%,.0f".format(task.Amount)}")
                    append(" × ${task.quantity})")
                },
                fontSize       = 15.sp,
                fontWeight     = FontWeight.SemiBold,
                color          = if (task.checkBox) TextSecondary else textPrimary,
                textDecoration = if (task.checkBox) TextDecoration.LineThrough else TextDecoration.None,
                modifier       = Modifier.weight(1f, fill = false)
            )
        }

        // ── edit / delete ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Edit", tint = Primary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CategoryChip(cat: TodoCategory, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50.dp))
            .background(if (selected) cat.color else cat.bg)
            .border(1.dp, if (selected) cat.color else cat.bg.copy(alpha = 0f), RoundedCornerShape(50.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text("${cat.emoji} ${cat.label}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else cat.color)
    }
}

@Composable
fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit, isDark: Boolean) {
    val chipBg = if (isDark) BackgroundDark else Color(0xFFF4F5F7)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(chipBg).padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = { if (quantity > 1) onChange(quantity - 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, "Decrease", tint = Primary, modifier = Modifier.size(16.dp))
        }
        Text(quantity.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
        IconButton(onClick = { onChange(quantity + 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, "Increase", tint = Primary, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Day-of-month stepper — lets you pick which day in the current month a todo lands on ──

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayOfMonthStepper(day: Int, monthDate: LocalDate, onChange: (Int) -> Unit, isDark: Boolean) {
    val maxDay = monthDate.lengthOfMonth()
    val chipBg = if (isDark) BackgroundDark else Color(0xFFF4F5F7)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(chipBg).padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = { if (day > 1) onChange(day - 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, "Earlier day", tint = Primary, modifier = Modifier.size(16.dp))
        }
        Text(
            monthDate.withDayOfMonth(day).format(DateTimeFormatter.ofPattern("d MMM", LocalLocale.current.platformLocale)),
            fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp)
        )
        IconButton(onClick = { if (day < maxDay) onChange(day + 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, "Later day", tint = Primary, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Add Todo bottom sheet — no more dayIndex/weekDates, just a day-of-month picker ────

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoBottomSheet(
    isDark     : Boolean,
    sheetState : SheetState,
    monthDate  : LocalDate,
    onDismiss  : () -> Unit,
    onAdd      : (TodoEntity) -> Unit
) {
    val background  = if (isDark) SurfaceDark else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimary
    val fieldBg     = if (isDark) BackgroundDark else Color(0xFFF4F5F7)

    var taskName    by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }
    var quantity    by remember { mutableStateOf(1) }
    var selectedDay by remember { mutableStateOf(monthDate.dayOfMonth) }
    var nameError   by remember { mutableStateOf(false) }
    var budgetError by remember { mutableStateOf<String?>(null) }
    var selectedCat by remember { mutableStateOf<TodoCategory?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor   = background,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).width(40.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color(0xFF3A4A6A) else Color(0xFFDDDDE5)))
        },
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 36.dp)) {
            Text("Add Todo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary, modifier = Modifier.padding(bottom = 16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Date", fontSize = 13.sp, color = TextSecondary)
                DayOfMonthStepper(day = selectedDay, monthDate = monthDate, onChange = { selectedDay = it }, isDark = isDark)
            }

            Spacer(Modifier.height(16.dp))

            Text("CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(TodoCategory.values().size) { i ->
                    val cat = TodoCategory.values()[i]
                    CategoryChip(cat = cat, selected = selectedCat == cat) { selectedCat = if (selectedCat == cat) null else cat }
                }
            }

            Text("Todo Name", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fieldBg)
                    .then(if (nameError) Modifier.border(1.dp, Orange, RoundedCornerShape(12.dp)) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (taskName.isEmpty()) Text("e.g. Groceries, Uber ride...", color = TextSecondary, fontSize = 15.sp)
                BasicTextField(taskName, { taskName = it; nameError = false }, singleLine = true, textStyle = TextStyle(color = textPrimary, fontSize = 15.sp), modifier = Modifier.fillMaxWidth())
            }
            if (nameError) Text("Please enter a todo name", fontSize = 12.sp, color = Orange, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Quantity", fontSize = 13.sp, color = TextSecondary)
                QuantityStepper(quantity = quantity, onChange = { quantity = it }, isDark = isDark)
            }

            Spacer(Modifier.height(16.dp))

            Text("Amount (₹)", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fieldBg)
                    .then(if (budgetError != null) Modifier.border(1.dp, Color(0xFFDC2626), RoundedCornerShape(12.dp)) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (budgetInput.isEmpty()) Text("0.00", color = TextSecondary, fontSize = 15.sp)
                BasicTextField(budgetInput, { budgetInput = it; budgetError = null }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = TextStyle(color = textPrimary, fontSize = 15.sp), modifier = Modifier.fillMaxWidth())
            }
            if (budgetError != null) Text(budgetError!!, fontSize = 12.sp, color = Color(0xFFDC2626), modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        var valid = true
                        if (taskName.isBlank()) { nameError = true; valid = false }
                        val amount = budgetInput.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) { budgetError = "Please enter a valid amount"; valid = false }

                        if (valid && amount != null) {
                            onAdd(
                                TodoEntity(
                                    Name      = taskName.trim(),
                                    Amount    = amount,
                                    quantity  = quantity,
                                    category  = selectedCat?.name,
                                    createdAt = monthDate.withDayOfMonth(selectedDay)
                                        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Add Todo", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTodoBottomSheet(
    isDark     : Boolean,
    sheetState : SheetState,
    task       : TodoEntity,
    onDismiss  : () -> Unit,
    onSave     : (TodoEntity) -> Unit
) {
    val background  = if (isDark) SurfaceDark else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimary
    val fieldBg     = if (isDark) BackgroundDark else Color(0xFFF4F5F7)

    var taskName    by remember { mutableStateOf(task.Name) }
    var budgetInput by remember { mutableStateOf(task.Amount.toLong().toString()) }
    var quantity    by remember { mutableStateOf(task.quantity) }
    var nameError   by remember { mutableStateOf(false) }
    var budgetError by remember { mutableStateOf<String?>(null) }
    var selectedCat by remember { mutableStateOf(task.categoryEnum) }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor   = background,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).width(40.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color(0xFF3A4A6A) else Color(0xFFDDDDE5)))
        },
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 36.dp)) {
            Text("Edit Todo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary, modifier = Modifier.padding(bottom = 16.dp))

            Text("CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(TodoCategory.values().size) { i ->
                    val cat = TodoCategory.values()[i]
                    CategoryChip(cat = cat, selected = selectedCat == cat) { selectedCat = if (selectedCat == cat) null else cat }
                }
            }

            Text("Todo Name", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fieldBg)
                    .then(if (nameError) Modifier.border(1.dp, Orange, RoundedCornerShape(12.dp)) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (taskName.isEmpty()) Text("e.g. Groceries, Uber ride...", color = TextSecondary, fontSize = 15.sp)
                BasicTextField(taskName, { taskName = it; nameError = false }, singleLine = true, textStyle = TextStyle(color = textPrimary, fontSize = 15.sp), modifier = Modifier.fillMaxWidth())
            }
            if (nameError) Text("Please enter a todo name", fontSize = 12.sp, color = Orange, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Quantity", fontSize = 13.sp, color = TextSecondary)
                QuantityStepper(quantity = quantity, onChange = { quantity = it }, isDark = isDark)
            }

            Spacer(Modifier.height(16.dp))

            Text("Amount (₹)", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fieldBg)
                    .then(if (budgetError != null) Modifier.border(1.dp, Color(0xFFDC2626), RoundedCornerShape(12.dp)) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (budgetInput.isEmpty()) Text("0.00", color = TextSecondary, fontSize = 15.sp)
                BasicTextField(budgetInput, { budgetInput = it; budgetError = null }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = TextStyle(color = textPrimary, fontSize = 15.sp), modifier = Modifier.fillMaxWidth())
            }
            if (budgetError != null) {
                Text(text = budgetError!!, fontSize = 12.sp, color = Color(0xFFDC2626), modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        var valid = true
                        if (taskName.isBlank()) { nameError = true; valid = false }
                        val amount = budgetInput.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) { budgetError = "Please enter a valid amount"; valid = false }
                        if (valid && amount != null) {
                            onSave(
                                task.copy(
                                    Name     = taskName.trim(),
                                    Amount   = amount,
                                    quantity = quantity,
                                    category = selectedCat?.name
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}