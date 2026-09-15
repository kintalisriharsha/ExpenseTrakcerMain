package com.example.expensetracker.frontend.sms

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.services.entity.ExpenseEntity
import java.util.Locale

private const val RESULT_CHANNEL_ID = "success_notification"

fun createNotification(
    context: Context,
    title: String,
    amount: Double,
    category: String
): NotificationCompat.Builder {
    val notifTitle = context.getString(R.string.app_name)
    val notifBody  = "Added $title \u2014 $category \u00B7 \u20B9${formatAmount(amount)} to your records"
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Builder(context, RESULT_CHANNEL_ID)
        .setContentTitle(notifTitle)
        .setContentText(notifBody)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
        .setSmallIcon(R.drawable.logo)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
}

fun createTransactionChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            RESULT_CHANNEL_ID,
            "Transaction Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies you when a bank SMS is parsed into a transaction"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}

@SuppressLint("MissingPermission", "NotificationPermission")
fun showTransactionNotification(context: Context, transaction: ExpenseEntity) {
    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        createTransactionChannel(context)
        val builder = createNotification(
            context,
            transaction.title,
            transaction.amount,
            transaction.category
        )

        NotificationManagerCompat.from(context)
            .notify(transaction.hashCode(), builder.build())
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) amount.toInt().toString()
    else String.format(Locale.getDefault(), "%.2f", amount)
}