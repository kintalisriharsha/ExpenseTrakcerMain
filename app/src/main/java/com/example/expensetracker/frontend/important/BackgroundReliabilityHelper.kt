package com.example.expensetracker.frontend.important

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Battery optimization exemption — a real, requestable Android permission/intent
 * that came up while debugging why SMS detection + widget refresh worked on an
 * emulator but not on a real device. Shows a one-tap system dialog letting the
 * user whitelist the app from OEM battery managers that would otherwise kill
 * background work SMS detection depends on.
 *
 * MainActivity requests this once automatically on cold start, sequenced AFTER
 * the notification and SMS/phone permission dialogs fully resolve (not launched
 * from a separate, concurrently-running coroutine) — that race is what used to
 * let this cancel the notification dialog outright. Settings can also offer a
 * manual re-ask button using [requestIgnoreBatteryOptimizations] for anyone who
 * declined the first time.
 *
 * The app hibernation / permission auto-revoke prompt ("Pause app activity if
 * unused" etc.) that used to live here has been dropped. Android doesn't let an
 * app silently turn that off for itself — the only thing possible was deep-linking
 * to a settings screen that had the same forced-navigation problem as the battery
 * prompt did, and it only matters after months of total inactivity, so it wasn't
 * worth keeping. See https://developer.android.com/topic/performance/app-hibernation
 * if this ever needs to come back.
 */
object BackgroundReliabilityHelper {

    /** True if the app is already exempt from battery optimizations. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Builds the "Allow this app to ignore battery optimizations?" intent for this
     * app's package. Callers that need to know when the user has finished with the
     * resulting screen (e.g. to sequence something after it) should launch this
     * via an ActivityResultLauncher (StartActivityForResult) rather than
     * context.startActivity.
     */
    fun createRequestIgnoreBatteryOptimizationsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }

    /**
     * Fire-and-forget convenience for a simple Settings-screen button: builds and
     * launches the intent directly. No-op if the app is already exempt, or on
     * devices too old to support it.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (isIgnoringBatteryOptimizations(context)) return

        try {
            context.startActivity(createRequestIgnoreBatteryOptimizationsIntent(context))
        } catch (e: android.content.ActivityNotFoundException) {
            // A handful of OEM builds strip this screen out — nothing more we can do.
        }
    }
}