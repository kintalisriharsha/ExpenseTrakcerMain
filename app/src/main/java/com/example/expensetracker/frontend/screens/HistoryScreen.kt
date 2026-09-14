package com.example.expensetracker.frontend.screens

import android.R.attr.maxHeight
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.text.SimpleDateFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    isDark: Boolean = false,
    expenseViewModel: ExpenseViewModel,
    todoViewModel: TodoViewModel
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
                                val grouped = expenses.groupBy { it.date }
                                val sortedGrouped = grouped.entries.sortedByDescending { (date, _) ->
                                    runCatching {
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(date)?.time ?: 0L
                                    }.getOrDefault(0L)
                                }
                                sortedGrouped.forEach { (date, txList) ->
                                    SectionHeader(text = date, textSize = 14)
                                    txList.forEach { expense ->
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