package com.example.expensetracker.frontend.important

import android.content.Context
import android.content.SharedPreferences

// ─────────────────────────────────────────────────────────────────────────────
//  AppPreferences  –  thin wrapper around SharedPreferences
//
//  New fields for notification-permission flow:
//    notificationDecisionMade  – true once user grants OR skips MAX_SKIPS times
//    notificationSkipCount     – how many times user tapped "Not Now"
//
//  MAX_SKIPS = 3  →  screen shows on launches 1, 2, 3 then never again.
// ─────────────────────────────────────────────────────────────────────────────

class AppPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME               = "app_prefs"
        private const val KEY_IS_DARK              = "is_dark"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        const  val MAX_SKIPS                       = 3
        private const val KEY_NOTIF_DECISION_MADE  = "notif_decision_made"
        private const val KEY_NOTIF_SKIP_COUNT     = "notif_skip_count"
        private const val KEY_BG_RELIABILITY_SHOWN = "bg_reliability_prompt_shown"
        private const val KEY_AUTO_REVOKE_SHOWN    = "auto_revoke_prompt_shown"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Existing ──────────────────────────────────────────────────────────────

    var isDark: Boolean
        get()      = prefs.getBoolean(KEY_IS_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK, value).apply()

    var notificationEnabled: Boolean
        get()      = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()

    // ── Notification screen control ───────────────────────────────────────────

    /** Never show the permission screen again after this is true. */
    var notificationDecisionMade: Boolean
        get()      = prefs.getBoolean(KEY_NOTIF_DECISION_MADE, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_DECISION_MADE, value).apply()

    /** True once the user has seen the battery-optimization/auto-revoke prompt. */
    var backgroundReliabilityPromptShown: Boolean
        get()      = prefs.getBoolean(KEY_BG_RELIABILITY_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_BG_RELIABILITY_SHOWN, value).apply()

    /** True once the user has seen the "Pause app activity if unused" settings screen. */
    var autoRevokePromptShown: Boolean
        get()      = prefs.getBoolean(KEY_AUTO_REVOKE_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_REVOKE_SHOWN, value).apply()
}