package com.example.expensetracker.frontend.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.components.BottomNavBar
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.LogoIcon
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import com.example.expensetracker.frontend.important.Appstate
import com.example.expensetracker.frontend.services.settingService.SettingsEntity
import com.example.expensetracker.frontend.services.settingService.SettingViewModel
import com.example.expensetracker.frontend.services.settingService.SettingsUiState
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.BackgroundLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.SurfaceLight
import com.example.expensetracker.ui.theme.TextPrimary
import com.example.expensetracker.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navController : NavController,
    isDark        : Boolean         = false,
    viewModel     : SettingViewModel
) {
    val background  = if (isDark) BackgroundDark else BackgroundLight
    val surface     = if (isDark) SurfaceDark    else SurfaceLight
    val border      = if (isDark) BorderDark      else BorderLight
    val textPrimary = if (isDark) Color.White     else TextPrimary

    // ── Local editable fields ─────────────────────────────────────────────────
    var dailyLimit    by remember { mutableStateOf("") }
    var weeklyBudget  by remember { mutableStateOf("") }
    var monthlyBudget by remember { mutableStateOf("") }
    var notifications by remember { mutableStateOf(Appstate.notificationEnabled) }
    var darkMode      by remember { mutableStateOf(Appstate.isDark) }

    // Derived progress values (updated when server data loads)
    var spentToday by remember { mutableStateOf(0.0) }
    var spentMonth by remember { mutableStateOf(0.0) }

    // ── Toast ─────────────────────────────────────────────────────────────────
    var toast by remember { mutableStateOf<ToastMessage?>(null) }

    val today  = LocalDate.now()
    val locale = LocalLocale.current.platformLocale

    fun buildSettingsEntity(
        monthlyBudget: Double,
        weeklyBudget: Double,
        dailyLimit: Double,
        notificationEnabled: Boolean,
        isDarkMode: Boolean
    ): SettingsEntity = SettingsEntity(
        year                = today.year,
        month               = today.monthValue,
        monthlyBudget       = monthlyBudget,
        weeklyBudget        = weeklyBudget,
        dailyLimit          = dailyLimit,
        notificationEnabled = notificationEnabled,
        isDarkMode          = isDarkMode
    )

    // ── Backing data from the ViewModel ─────────────────────────────────────────
    val uiState by viewModel.uiState.collectAsState()
    val settingsState = (uiState as? SettingsUiState.Success)?.settings
    val errorMessage   by viewModel.errorMessage.collectAsState()

    // True only while the very first snapshot hasn't arrived yet.
    // (settingsState == null is also true for a *loaded* new user with no row,
    // so we can't infer "still loading" from that — we ask uiState directly.)
    val isLoading = uiState is SettingsUiState.Loading
    val isUninitialized = dailyLimit.isEmpty() && weeklyBudget.isEmpty() && monthlyBudget.isEmpty()


    // ── React to newly loaded settings ─────────────────────────────────────────
    // (Sample-data seeding for first-time users now lives in SettingViewModel's init block.)
    LaunchedEffect(settingsState) {
        val resp = settingsState ?: return@LaunchedEffect
        dailyLimit    = resp.dailyLimit.toLong().toString()
        weeklyBudget  = resp.weeklyBudget.toLong().toString()
        monthlyBudget = resp.monthlyBudget.toLong().toString()
        notifications              = resp.notificationEnabled
        darkMode                   = resp.isDarkMode
        Appstate.isDark            = resp.isDarkMode
        Appstate.notificationEnabled = resp.notificationEnabled
    }

    // ── React to ViewModel errors ────────────────────────────────────────────
    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        toast = ToastMessage(message, ToastType.ERROR)
        viewModel.clearError()
    }

    val days  = (-2..2).map { today.plusDays(it.toLong()) }

    val dailyLimitVal    = dailyLimit.toDoubleOrNull()    ?: 1.0
    val monthlyBudgetVal = monthlyBudget.toDoubleOrNull() ?: 1.0
    val dailyProgress    = (spentToday / dailyLimitVal).toFloat().coerceIn(0f, 1f)
    val monthlyProgress  = (spentMonth / monthlyBudgetVal).toFloat().coerceIn(0f, 1f)

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Budget Settings",
                            color      = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 24.sp
                        )
                    }
                },
                navigationIcon = {
                    Column(Modifier.padding(start = 20.dp, end = 10.dp)) {
                        LogoIcon(navController = navController)
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color       = Primary,
                            modifier    = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Filled.ArrowBack, null, tint = Color.Transparent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab   = 3,
                navController = navController,
                surface       = surface,
                border        = border,
                textPrimary   = textPrimary,
                isDark        = isDark
            )
        }
    ) { paddingValues ->

        Box(Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                Text(
                    "Financial Targets",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = textPrimary,
                    modifier   = Modifier.padding(start = 10.dp, top = 20.dp)
                )
                Text(
                    "Manage your limits and savings goals",
                    fontSize = 16.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(start = 10.dp)
                )

                Spacer(Modifier.height(20.dp))

                // ── Target Date Card ──────────────────────────────────────────
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(20.dp),
                    color           = surface,
                    shadowElevation = 3.dp,
                    tonalElevation  = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                null,
                                tint     = TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Target Date",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 18.sp,
                                color      = textPrimary
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            days.forEach { date ->
                                val isToday = date == today
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier            = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isToday) Primary else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text       = date.dayOfWeek
                                            .getDisplayName(JavaTextStyle.SHORT, LocalLocale.current.platformLocale)
                                            .uppercase(),
                                        fontSize   = 12.sp,
                                        color      = if (isToday) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text       = date.dayOfMonth.toString(),
                                        fontSize   = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (isToday) Color.White else textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Daily Spending Card ───────────────────────────────────────
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(20.dp),
                    color           = surface,
                    shadowElevation = 3.dp,
                    tonalElevation  = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier         = Modifier
                                        .size(50.dp)
                                        .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        null,
                                        tint     = Primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Daily Spending",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 20.sp,
                                    color      = textPrimary
                                )
                            }
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFEEF2FF)) {
                                Text(
                                    "Active",
                                    color      = Primary,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Daily Expense Limit", fontSize = 15.sp, color = TextSecondary)
                        Spacer(Modifier.height(10.dp))

                        Surface(
                            modifier        = Modifier.fillMaxWidth(),
                            shape           = RoundedCornerShape(12.dp),
                            color           = background,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.AttachMoney,
                                    null,
                                    tint     = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    // Show "0" if we are uninitialized and fetching data
                                    value           = if (isUninitialized && isLoading) "0" else dailyLimit,
                                    onValueChange   = { if (!(isUninitialized && isLoading)) dailyLimit = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle       = TextStyle(
                                        fontSize   = 18.sp,
                                        color      = textPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Weekly Budget Card ────────────────────────────────────────────────────
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(20.dp),
                    color           = surface,
                    shadowElevation = 3.dp,
                    tonalElevation  = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier         = Modifier
                                    .size(50.dp)
                                    .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DateRange,
                                    null,
                                    tint     = Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Weekly Budget",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 20.sp,
                                color      = textPrimary
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Weekly Allowance (≤ Monthly Budget)", fontSize = 15.sp, color = TextSecondary)
                        Spacer(Modifier.height(10.dp))

                        Surface(
                            modifier        = Modifier.fillMaxWidth(),
                            shape           = RoundedCornerShape(12.dp),
                            color           = background,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Wallet,
                                    null,
                                    tint     = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    // Show "0" if we are uninitialized and fetching data
                                    value           = if (isUninitialized && isLoading) "0" else weeklyBudget,
                                    onValueChange   = { if (!(isUninitialized && isLoading)) weeklyBudget = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle       = TextStyle(
                                        fontSize   = 18.sp,
                                        color      = textPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        val wb = weeklyBudget.toDoubleOrNull() ?: 0.0
                        val mb = monthlyBudget.toDoubleOrNull() ?: 0.0
                        if (wb > 0.0 && mb > 0.0 && wb > mb) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "⚠ Weekly budget exceeds monthly budget",
                                fontSize = 13.sp,
                                color    = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Monthly Budget Card ───────────────────────────────────────
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(20.dp),
                    color           = surface,
                    shadowElevation = 3.dp,
                    tonalElevation  = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier         = Modifier
                                    .size(50.dp)
                                    .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CalendarMonth,
                                    null,
                                    tint     = Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Monthly Budget",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 20.sp,
                                color      = textPrimary
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Total Monthly Allowance", fontSize = 15.sp, color = TextSecondary)
                        Spacer(Modifier.height(10.dp))

                        Surface(
                            modifier        = Modifier.fillMaxWidth(),
                            shape           = RoundedCornerShape(12.dp),
                            color           = background,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Wallet,
                                    null,
                                    tint     = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    // Show "0" if we are uninitialized and fetching data
                                    value           = if (isUninitialized && isLoading) "0" else monthlyBudget,
                                    onValueChange   = { if (!(isUninitialized && isLoading)) monthlyBudget = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle       = TextStyle(
                                        fontSize   = 18.sp,
                                        color      = textPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Push Notifications Toggle ─────────────────────────────────
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(20.dp),
                    color           = surface,
                    shadowElevation = 3.dp,
                    tonalElevation  = 0.dp
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier         = Modifier
                                    .size(50.dp)
                                    .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    null,
                                    tint     = Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "Push Notifications",
                                fontSize   = 20.sp,
                                color      = textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // ── Push Notifications Toggle ─────────────────────────────────
                        Switch(
                            checked = notifications,
                            enabled = !(isUninitialized && isLoading),
                            onCheckedChange = { newValue ->
                                notifications = newValue
                                Appstate.notificationEnabled = newValue

                                // Determine values: if empty (first time), send 0.0
                                val dl = dailyLimit.toDoubleOrNull() ?: 0.0
                                val wb = weeklyBudget.toDoubleOrNull() ?: 0.0
                                val mb = monthlyBudget.toDoubleOrNull() ?: 0.0

                                // Push to the local DB immediately
                                val entity = buildSettingsEntity(
                                    monthlyBudget       = mb,
                                    weeklyBudget        = wb,
                                    dailyLimit          = dl,
                                    notificationEnabled = newValue,
                                    isDarkMode          = darkMode
                                )
                                if (settingsState == null) {
                                    viewModel.initSettings(entity)
                                } else {
                                    viewModel.updateSettings(entity)
                                }

                                toast = ToastMessage(
                                    if (newValue) "Notifications enabled ✓" else "Notifications disabled",
                                    if (newValue) ToastType.SUCCESS else ToastType.INFO
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(20.dp),
                    color           = surface,
                    shadowElevation = 3.dp,
                    tonalElevation  = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DarkMode,
                                    null,
                                    tint = Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "Dark Mode",
                                fontSize = 20.sp,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = darkMode,
                            enabled = !(isUninitialized && isLoading),
                            onCheckedChange = { newValue ->
                                darkMode = newValue
                                Appstate.isDark = newValue

                                // Get current values or 0.0 if user hasn't typed anything yet
                                val dl = dailyLimit.toDoubleOrNull() ?: 0.0
                                val wb = weeklyBudget.toDoubleOrNull() ?: 0.0
                                val mb = monthlyBudget.toDoubleOrNull() ?: 0.0

                                // Push to the local DB immediately
                                val entity = buildSettingsEntity(
                                    monthlyBudget       = mb,
                                    weeklyBudget        = wb,
                                    dailyLimit          = dl,
                                    notificationEnabled = notifications,
                                    isDarkMode          = newValue
                                )
                                if (settingsState == null) {
                                    viewModel.initSettings(entity)
                                } else {
                                    viewModel.updateSettings(entity)
                                }

                                toast = ToastMessage(
                                    if (newValue) "Dark mode enabled ✓" else "Dark mode disabled",
                                    if (newValue) ToastType.SUCCESS else ToastType.INFO
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Save Button ─────────────────────────────────────────
                Button(
                    onClick = {
                        val dl = dailyLimit.toDoubleOrNull() ?: 0.0
                        val wb = weeklyBudget.toDoubleOrNull() ?: 0.0
                        val mb = monthlyBudget.toDoubleOrNull() ?: 0.0

                        when {
                            dl <= 0.0 -> toast = ToastMessage("Please set a daily limit", ToastType.ERROR)
                            wb <= 0.0 -> toast = ToastMessage("Please set a weekly budget", ToastType.ERROR)
                            mb <= 0.0 -> toast = ToastMessage("Please set a monthly budget", ToastType.ERROR)
                            wb > mb   -> toast = ToastMessage("Weekly budget cannot exceed monthly", ToastType.ERROR)
                            else -> {
                                val entity = buildSettingsEntity(
                                    monthlyBudget       = mb,
                                    weeklyBudget        = wb,
                                    dailyLimit          = dl,
                                    notificationEnabled = notifications,
                                    isDarkMode          = darkMode
                                )
                                if (settingsState == null) {
                                    viewModel.initSettings(entity)
                                } else {
                                    viewModel.updateSettings(entity)
                                }
                                toast = ToastMessage("Settings updated successfully ✓", ToastType.SUCCESS)
                            }
                        }
                    },
                    modifier       = Modifier.fillMaxWidth(),
                    shape          = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(20.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled        = !(isUninitialized && isLoading)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            strokeWidth = 2.dp,
                            modifier    = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            "Save All Settings",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // ── Toast overlay ─────────────────────────────────────────────────
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