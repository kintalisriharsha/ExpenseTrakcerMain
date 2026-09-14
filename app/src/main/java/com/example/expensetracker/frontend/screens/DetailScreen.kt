package com.example.expensetracker.frontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.components.BottomNavBar
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.BackgroundLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.SurfaceLight
import com.example.expensetracker.ui.theme.TextSecondary
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    navController: NavController,
    isDark: Boolean = false,
    expenseViewModel: ExpenseViewModel
) {
    val uiState by expenseViewModel.uiState.collectAsState()
    val expense = uiState.selectedExpense ?: return

    val background  = if (isDark) BackgroundDark  else BackgroundLight
    val surface     = if (isDark) SurfaceDark     else SurfaceLight
    val border      = if (isDark) BorderDark      else BorderLight
    val textPrimary = if (isDark) Color.White     else Color(0xFF0F172A)

    val initials = (expense.contactName ?: "")
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    var showDeleteDialog  by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toast = ToastMessage(it, ToastType.ERROR)
            expenseViewModel.clearError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text    = { Text("Are you sure you want to delete this transaction? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    expenseViewModel.deleteExpense(expense.id) {
                        navController.navigate("history") {
                            popUpTo("detailScreen") { inclusive = true }
                        }
                    }
                }) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor   = background,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Expense Detail",
                            color      = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 24.sp
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        Icons.Filled.ArrowBack,
                        null,
                        tint     = textPrimary,
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .clickable { navController.popBackStack()}
                    )
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("edit_expense") { launchSingleTop = true }
                    }) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit",
                            tint     = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab   = 1,
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(Color(0x15A8D8F0))
                            .align(Alignment.TopStart)
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color(0x10B0E8D0))
                            .align(Alignment.BottomEnd)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(
                                    8.dp,
                                    RoundedCornerShape(22.dp),
                                    ambientColor = Color(0x201152D4),
                                    spotColor    = Color(0x301152D4)
                                )
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFFDEEBFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Restaurant,
                                null,
                                tint     = Primary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Text(
                            expense.category.uppercase(),
                            fontSize      = 13.sp,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color         = Color(0xFF64748B)
                        )
                        Text(
                            "-₹${"%.2f".format(expense.amount)}",
                            fontSize      = 48.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = if (isDark) Color.White else Color(0xFF0F172A),
                            letterSpacing = (-1).sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        Modifier.weight(1f), RoundedCornerShape(18.dp),
                        color = surface, shadowElevation = 2.dp, tonalElevation = 0.dp
                    ) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = Primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("DATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(expense.date, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }
                    Surface(
                        Modifier.weight(1f), RoundedCornerShape(18.dp),
                        color = surface, shadowElevation = 2.dp, tonalElevation = 0.dp
                    ) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                            Icon(Icons.Outlined.AccessTime, null, tint = Primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("TIME", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            expense.time?.let { Text(it, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary) }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    RoundedCornerShape(18.dp),
                    color = surface, shadowElevation = 2.dp, tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF0E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("CONTACT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = TextSecondary)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                expense.contactName?.ifBlank { "No contact" } ?: "No contact",
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textPrimary
                            )
                            if (!expense.contactNumber.isNullOrBlank()) {
                                Spacer(Modifier.height(3.dp))
                                val masked = "******" + expense.contactNumber.takeLast(4)
                                Text(masked, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    RoundedCornerShape(18.dp),
                    color = surface, shadowElevation = 2.dp, tonalElevation = 0.dp
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                        Text("NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = TextSecondary)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            expense.notes?.takeIf { it.isNotBlank() } ?: "No notes",
                            fontSize   = 15.sp,
                            color      = if (expense.notes.isNullOrBlank()) TextSecondary else textPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFEE2E2))
                        .clickable(enabled = !uiState.isLoading) { showDeleteDialog = true }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color    = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                            Text("Delete Transaction", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
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