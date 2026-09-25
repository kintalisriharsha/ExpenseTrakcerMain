package com.example.expensetracker

import android.Manifest
import android.content.ActivityNotFoundException
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.frontend.components.ActionCardPickerScreen
import com.example.expensetracker.frontend.important.AppPreferences
import com.example.expensetracker.frontend.important.Appstate
import com.example.expensetracker.frontend.important.Appstate.isDark
import com.example.expensetracker.frontend.important.BackgroundReliabilityHelper
import com.example.expensetracker.frontend.screens.NotificationHistoryScreen
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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
        val analyticsRepo      = AnalyticsRepository(
            db.analyticsDao(),
            settingRepository = settingRepository
        )
        val todoRepository     = TodoRepository(db.todoDao())

        setContent {
            ExpenseTrackerTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
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

                // ── Continuation slots used to serialize the cold-start dialogs ────
                // Each launcher below resumes its slot when the OS calls back, so the
                // single LaunchedEffect further down can suspend until a dialog has
                // FULLY resolved before launching the next one — no more racing
                // coroutines, no more delay(N) guesses.
                var notificationResult by remember { mutableStateOf<CancellableContinuation<Unit>?>(null) }
                var smsPhoneResult     by remember { mutableStateOf<CancellableContinuation<Unit>?>(null) }
                var batteryOptResult   by remember { mutableStateOf<CancellableContinuation<Unit>?>(null) }

                // ── Generic POST_NOTIFICATIONS request (API 33+) ──────────────
                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    Appstate.notificationEnabled = granted
                    notificationResult?.resume(Unit)
                    notificationResult = null
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
                    smsPhoneResult?.resume(Unit)
                    smsPhoneResult = null
                }

                // ── Battery optimization exemption launcher ────────────────────
                // Uses StartActivityForResult (not a fire-and-forget startActivity)
                // so the cold-start flow can await the real moment the user backs
                // out of this screen, the same way it awaits the permission dialogs.
                val batteryOptLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    batteryOptResult?.resume(Unit)
                    batteryOptResult = null
                }

                // ── Cold-start permission & system-dialog flow ─────────────────────
                // ONE sequential coroutine, not several LaunchedEffect(Unit) blocks.
                // Multiple LaunchedEffect(Unit) blocks each start their own coroutine
                // and run concurrently — that's what let the battery-optimization
                // Activity launch mid-flight and cancel the notification permission
                // dialog while it was still resolving. Each step below now suspends
                // on the launcher's real callback (via suspendCancellableCoroutine),
                // not a fixed delay(), so nothing can start until the previous dialog
                // has actually finished.
                LaunchedEffect(Unit) {

                    // -- Notification permission (API 33+) --
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        Appstate.notificationEnabled = hasNotificationPermission

                        if (!hasNotificationPermission && !appPreferences.notificationDecisionMade) {
                            appPreferences.notificationDecisionMade = true
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            suspendCancellableCoroutine<Unit> { cont -> notificationResult = cont }
                        }
                    } else {
                        Appstate.notificationEnabled = true
                        appPreferences.notificationDecisionMade = true
                    }

                    // -- Sync Appstate permission flags --
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

                    // -- SMS + phone permissions, only if not already granted --
                    if (!hasSms || !hasPhone) {
                        val permissionsToRequest = buildList {
                            add(Manifest.permission.RECEIVE_SMS)
                            add(Manifest.permission.READ_SMS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                add(Manifest.permission.READ_PHONE_NUMBERS)
                            add(Manifest.permission.READ_PHONE_STATE)
                        }
                        smsPhoneLauncher.launch(permissionsToRequest.toTypedArray())
                        suspendCancellableCoroutine<Unit> { cont -> smsPhoneResult = cont }
                    }

                    // -- Battery optimization exemption (once) --
                    // Protects background SMS detection + widget refresh from being
                    // silently killed by OEM battery managers — SMS detection depends
                    // on this, so it's still requested automatically rather than left
                    // as an easy-to-miss opt-in. Not gated on the SMS grant result
                    // above — that reflects state from before the async SMS request
                    // resolves. It only runs here, AFTER the two dialogs above have
                    // fully finished, so it can no longer race or cancel them.
                    if (!appPreferences.backgroundReliabilityPromptShown &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !BackgroundReliabilityHelper.isIgnoringBatteryOptimizations(context)
                    ) {
                        appPreferences.backgroundReliabilityPromptShown = true
                        val intent = BackgroundReliabilityHelper
                            .createRequestIgnoreBatteryOptimizationsIntent(context)
                        try {
                            batteryOptLauncher.launch(intent)
                            suspendCancellableCoroutine<Unit> { cont -> batteryOptResult = cont }
                        } catch (e: ActivityNotFoundException) {
                            // A handful of OEM builds strip this screen out — nothing more we can do.
                        }
                    }

                    // The app hibernation / auto-revoke prompt stays dropped: Android
                    // never lets an app silently disable it, the deep-link screen had
                    // the same forced-navigation risk, and it only matters after
                    // months of total inactivity — not worth it. See
                    // https://developer.android.com/topic/performance/app-hibernation
                    // if this ever needs to come back.
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
                    composable("notifications") {
                        NotificationHistoryScreen(
                            navController    = navController,
                            isDark           = isDark,
                            expenseViewModel = expenseViewModel
                        )
                    }
                }
            }
        }
    }
}