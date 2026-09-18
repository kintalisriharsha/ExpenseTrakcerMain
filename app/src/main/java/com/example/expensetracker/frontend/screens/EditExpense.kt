package com.example.expensetracker.frontend.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.components.Contact
import com.example.expensetracker.frontend.components.ContactSheet
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.DatePickerModal
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.services.entity.ExpenseEntity
import com.example.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    navController: NavController,
    isDark: Boolean = false,
    expenseViewModel: ExpenseViewModel
) {
    val background  = if (isDark) BackgroundDark else BackgroundLight
    val surface     = if (isDark) SurfaceDark    else SurfaceLight
    val border      = if (isDark) BorderDark     else BorderLight
    val textPrimary = if (isDark) Color.White    else TextPrimary

    val uiState by expenseViewModel.uiState.collectAsState()
    val expense = uiState.selectedExpense ?: run {
        LaunchedEffect(Unit) {
            navController.navigateUp()
        }
        return
    }

    var amount           by remember { mutableStateOf(formatAmountForInput(expense.amount)) }
    // Tracks whether the user has actually pressed a keypad key yet. Without this,
    // the pre-filled amount string (e.g. "500") gets appended to on the very first
    // digit tap instead of being replaced — tapping "3" on a ₹500 expense produced
    // "5003" instead of "3". The first tap after opening this screen now replaces
    // the whole value; every tap after that behaves normally.
    var hasEditedAmount  by remember { mutableStateOf(false) }
    var showLimitError   by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(expense.category) }
    var notes            by remember { mutableStateOf(expense.notes ?: "") }
    var selectedDate     by remember { mutableStateOf(expense.date) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker   by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }
    var selectedContact  by remember {
        mutableStateOf(
            if (!expense.contactName.isNullOrBlank())
                Contact(
                    id       = "",
                    name     = expense.contactName,
                    phone    = expense.contactNumber ?: "",
                    initials = expense.contactName
                        .split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                )
            else null
        )
    }

    var toast by remember { mutableStateOf<ToastMessage?>(null) }
    var updatedSuccessfully by remember { mutableStateOf(false) }

    // --- Custom keypad handlers (mirrors AddExpense.kt) --------------------------
    fun handleDigit(digit: String) {
        val base = if (!hasEditedAmount) "" else amount
        hasEditedAmount = true
        val candidate = if (base.isEmpty() || base == "0") digit else base + digit
        if (!candidate.matches(Regex("^\\d*\\.?\\d{0,2}$"))) return
        val numeric = candidate.toDoubleOrNull() ?: 0.0
        if (numeric <= 200000.0) {
            amount = candidate
            showLimitError = false
        } else {
            showLimitError = true
        }
    }

    fun handleDecimalPoint() {
        val base = if (!hasEditedAmount) "" else amount
        hasEditedAmount = true
        if (base.contains(".")) {
            amount = base
            return
        }
        amount = if (base.isEmpty()) "0." else "$base."
        showLimitError = false
    }

    fun handleBackspace() {
        hasEditedAmount = true
        if (amount.isNotEmpty()) {
            amount = amount.dropLast(1)
            showLimitError = false
        }
    }

    // --- Save ---------------------------------------------------------------------
    fun saveChanges() {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        if (amountDouble <= 0.0 || selectedCategory.isBlank()) {
            toast = ToastMessage("Please enter a valid amount and category.", ToastType.ERROR)
            return
        }

        // Store date as "dd MMM yyyy" (e.g. "13 Sep 2026") — this must match
        // the format written by AddExpense and parsed by AnalyticsDao's
        // substr()/CASE date logic. Locale.ENGLISH is forced so the month
        // abbreviation is always Jan/Feb/.../Dec regardless of device locale.
        val storedDate = Instant.ofEpochMilli(selectedDateMillis)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

        // Time isn't editable on this screen (shown read-only on
        // the detail screen instead), so keep it unchanged.
        val updatedExpense = expense.copy(
            amount        = amountDouble,
            category      = selectedCategory,
            notes         = notes.ifBlank { null },
            date          = storedDate,
            contactName   = selectedContact?.name ?: "",
            contactNumber = selectedContact?.phone ?: "",
            icon          = selectedCategory,
            title         = notes.ifBlank { selectedCategory }
        )

        expenseViewModel.updateExpense(updatedExpense) {
            updatedSuccessfully = true
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toast = ToastMessage(it, ToastType.ERROR)
            expenseViewModel.clearError()
        }
    }

    LaunchedEffect(updatedSuccessfully) {
        if (updatedSuccessfully) {
            toast = ToastMessage("Expense updated successfully!", ToastType.SUCCESS)
            kotlinx.coroutines.delay(1_500)
            navController.navigate("detailScreen") {
                popUpTo("edit_expense") { inclusive = true }
            }
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { date ->
                selectedDate = date
                runCatching {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    selectedDateMillis = sdf.parse(date)?.time ?: System.currentTimeMillis()
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showContactSheet) {
        ContactSheet(
            onDismiss              = { showContactSheet = false },
            onContactSelected      = { contact ->
                selectedContact = contact
                showContactSheet = false
            },
            isDark                 = isDark,
            initialSelectedContact = selectedContact
        )
    }

    Scaffold(
        containerColor   = background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
                title  = {
                    Text(
                        "Edit Expense",
                        color      = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 24.sp
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Close",
                            tint               = textPrimary,
                            modifier           = Modifier.size(32.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NumericKeypad(
                onDigitClick     = ::handleDigit,
                onDecimalClick   = ::handleDecimalPoint,
                onBackspaceClick = ::handleBackspace,
                isDark           = isDark,
                surface          = surface,
                border           = border
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
                AmountInputField(
                    amount         = amount.toDoubleOrNull() ?: 0.0,
                    isEmpty        = amount.isEmpty(),
                    showLimitError = showLimitError,
                    isDark         = isDark
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = if (isDark) Color(0xFF2A3347) else Color(0xFFE8E8EE),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CategoryGrid(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        textPrimary = textPrimary
                    )

                    ShowDetails(
                        notes = notes,
                        onNotesChange = { notes = it },
                        selectedDate = selectedDate,
                        onDateClick = { showDatePicker = true },
                        selectedContact = selectedContact,
                        onContactClick = { showContactSheet = true },
                        onContactClear = { selectedContact = null },
                        surface = surface,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        showTimeField = false,
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { saveChanges() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                "Save Changes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
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

/** Avoids Double.toString()'s unwanted trailing ".0" on whole-number amounts
 *  (e.g. shows "500" instead of "500.0" when the edit screen first opens). */
private fun formatAmountForInput(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()