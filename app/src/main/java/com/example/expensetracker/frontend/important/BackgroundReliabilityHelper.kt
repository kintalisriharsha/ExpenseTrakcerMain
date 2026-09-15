package com.example.expensetracker.frontend.important

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Two OEM/OS background-restriction prompts that came up while debugging why SMS
 * detection + widget refresh worked on an emulator but not on a real device:
 *
 *  1. Battery optimization exemption — a real, requestable Android permission/intent.
 *     Shows a one-tap system dialog letting the user whitelist the app.
 *
 *  2. "Pause app activity if unused" / permission auto-revoke — Android does NOT let
 *     any third-party app silently turn this off for itself (by design, for privacy).
 *     The only thing an app can do is deep-link the user straight to that toggle
 *     screen so they don't have to hunt for it manually in Settings.
 *
 * Neither of these can be done invisibly — both require the user to actually see a
 * system screen and make the choice themselves.
 */
object BackgroundReliabilityHelper {

    /** True if the app is already exempt from battery optimizations. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Fires the system "Allow this app to ignore battery optimizations?" dialog.
     * No-op if the app is already exempt, or on devices too old to support it.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (isIgnoringBatteryOptimizations(context)) return

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // A handful of OEM builds strip this screen out — nothing more we can do.
        }
    }

    /**
     * Opens the per-app "Pause app activity if unused" / auto-revoke-permissions
     * screen so the user can toggle it off themselves. Falls back to the generic
     * App Info screen on very old devices that predate this specific screen.
     */
    fun openAutoRevokePermissionsSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }
    }
}
