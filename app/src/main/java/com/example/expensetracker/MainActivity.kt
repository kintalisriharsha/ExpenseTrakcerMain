package com.example.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.frontend.components.ActionCardPickerScreen
import com.example.expensetracker.frontend.important.AppPreferences
import com.example.expensetracker.frontend.important.Appstate
import com.example.expensetracker.frontend.important.Appstate.isDark
import com.example.expensetracker.frontend.screens.*
import com.example.expensetracker.frontend.services.analyticsService.AnalyticsRepository
import com.example.expensetracker.frontend.services.analyticsService.AnalyticsViewModel
import com.example.expensetracker.frontend.services.analyticsService.AnalyticsViewModelFactory
import com.example.expensetracker.frontend.services.expenseService.ExpenseRepository
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.frontend.services.TodoService.TodoRepository
import com.example.expensetracker.frontend.services.TodoService.TodoViewModel
import com.example.expensetracker.frontend.services.homeService.HomeRepository
import com.example.expensetracker.frontend.services.homeService.HomeViewModel
import com.example.expensetracker.frontend.services.homeService.HomeViewModelFactory
import com.example.expensetracker.frontend.services.important.AppDatabase
import com.example.expensetracker.frontend.services.settingService.SettingRepository
import com.example.expensetracker.frontend.services.settingService.SettingViewModel
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.expensetracker.frontend.services.TodoService.TodoViewModelFactory
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModelFactory
import com.example.expensetracker.frontend.services.settingService.SettingViewModelFactory

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Restore persisted preferences BEFORE setContent ───────────────────
        val appPreferences = AppPreferences(applicationContext)
        isDark                       = appPreferences.isDark
        Appstate.notificationEnabled = appPreferences.notificationEnabled

        // ── Room-backed repositories ──────────────────────────────────────────
        val db = AppDatabase.getInstance(applicationContext)

        val expenseRepository  = ExpenseRepository(db.expenseDao())
        val settingRepository  = SettingRepository(db.settingDao())
        val homeRepository     = HomeRepository(db.homeDao())
        val analyticsRepo      = AnalyticsRepository(db.analyticsDao())
        val todoRepository     = TodoRepository(db.todoDao())

        setContent {
            ExpenseTrackerTheme {
                val navController = rememberNavController()
                val context = androidx.compose.ui.platform.LocalContext.current

                val todoViewModel: TodoViewModel           = viewModel(factory = TodoViewModelFactory(todoRepository))
                val settingViewModel: SettingViewModel     = viewModel(factory = SettingViewModelFactory(settingRepository))
                val expenseViewModel: ExpenseViewModel     = viewModel(factory = ExpenseViewModelFactory(expenseRepository))
                val homeViewModel: HomeViewModel           = viewModel(factory = HomeViewModelFactory(homeRepository))
                val analyticsViewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModelFactory(analyticsRepo))

                // ── Persist dark mode + notifications on every change ─────────
                LaunchedEffect(isDark, Appstate.notificationEnabled) {
                    appPreferences.isDark              = isDark
                    appPreferences.notificationEnabled = Appstate.notificationEnabled
                }

                // ── Generic POST_NOTIFICATIONS request (API 33+) ──────────────
                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    Appstate.notificationEnabled = granted
                }

                // ── SMS + Phone OS launcher ───────────────────────────────────
                val smsPhoneLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    Appstate.smsGranted =
                        results[Manifest.permission.RECEIVE_SMS] == true &&
                                results[Manifest.permission.READ_SMS]    == true
                    val phoneNumbersGranted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            results[Manifest.permission.READ_PHONE_NUMBERS] == true
                        else true
                    Appstate.phoneGranted =
                        phoneNumbersGranted &&
                                results[Manifest.permission.READ_PHONE_STATE] == true
                }

                // ── Sync Appstate permission flags on cold start ──────────────
                LaunchedEffect(Unit) {
                    Appstate.contactsGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED

                    val hasSms = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECEIVE_SMS
                    ) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED

                    Appstate.smsGranted = hasSms

                    val phoneNumbersOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.READ_PHONE_NUMBERS
                        ) == PackageManager.PERMISSION_GRANTED
                    else true
                    val hasPhone = phoneNumbersOk &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED
                    Appstate.phoneGranted = hasPhone

                    // ── Actually request SMS/phone permissions if not yet granted ─────
                    if (!hasSms || !hasPhone) {
                        val permissionsToRequest = buildList {
                            add(Manifest.permission.RECEIVE_SMS)
                            add(Manifest.permission.READ_SMS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                add(Manifest.permission.READ_PHONE_NUMBERS)
                            add(Manifest.permission.READ_PHONE_STATE)
                        }
                        smsPhoneLauncher.launch(permissionsToRequest.toTypedArray())
                    }

                    // ── Notification permission (existing) ─────────────────────────────
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        Appstate.notificationEnabled = hasNotificationPermission

                        if (!hasNotificationPermission && !appPreferences.notificationDecisionMade) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            appPreferences.notificationDecisionMade = true
                        }
                    } else {
                        Appstate.notificationEnabled = true
                        appPreferences.notificationDecisionMade = true
                    }
                }

                // ── Always start at home ───────────────────────────────────────
                val startDest = "home"

                // ── NavHost ───────────────────────────────────────────────────
                NavHost(navController, startDestination = startDest) {

                    composable("home") {
                        ExpenseTrackerScreen(
                            navController = navController,
                            isDark        = isDark,
                            homeViewModel = homeViewModel,
                            expenseViewModel = expenseViewModel
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            navController = navController,
                            isDark = isDark,
                            expenseViewModel = expenseViewModel,
                            todoViewModel = todoViewModel,
                        )
                    }
                    composable("add_expense") {
                        AddExpense(
                            navController    = navController,
                            isDark           = isDark,
                            expenseViewModel = expenseViewModel
                        )
                    }
                    composable("edit_expense") {
                        EditExpenseScreen(
                            navController    = navController,
                            isDark           = isDark,
                            expenseViewModel = expenseViewModel
                        )
                    }
                    composable("show_screens") {
                        ActionCardPickerScreen(
                            navController = navController,
                            isDark        = isDark
                        )
                    }
                    composable("settings") {
                        Settings(
                            navController = navController,
                            isDark        = isDark,
                            viewModel     = settingViewModel
                        )
                    }
                    composable("detailScreen") {
                        TransactionDetailScreen(
                            navController    = navController,
                            isDark           = isDark,
                            expenseViewModel = expenseViewModel
                        )
                    }
                    composable("analytics") {
                        AnalyticsScreen(
                            navController      = navController,
                            isDark             = isDark,
                            analyticsViewModel = analyticsViewModel
                        )
                    }
                    composable("todo") {
                        TodoPlanner(
                            onDismiss = { navController.popBackStack() },
                            navController = navController,
                            isDark = isDark,
                            viewModel = todoViewModel
                        )
                    }
                }
            }
        }
    }
}