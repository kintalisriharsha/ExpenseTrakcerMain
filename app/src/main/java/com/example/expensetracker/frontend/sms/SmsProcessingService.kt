package com.example.expensetracker.frontend.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.expensetracker.frontend.services.expenseService.ExpenseRepository
import com.example.expensetracker.frontend.services.important.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsProcessingService : Service() {

    private val serviceJob   = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var repository: ExpenseRepository

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, buildSilentNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra("sender") ?: return START_NOT_STICKY
        val body   = intent.getStringExtra("body")   ?: return START_NOT_STICKY

        serviceScope.launch {
            processAndStoreSms(sender, body, startId)
        }
        return START_NOT_STICKY
    }

    private suspend fun processAndStoreSms(sender: String, body: String, startId: Int) {
        val transaction = SmsParser.parse(sender, body)

        if (transaction != null) {
            repository.add(transaction)
            showTransactionNotification(this@SmsProcessingService, transaction)
        }

        stopSelf(startId)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildSilentNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Processing",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            setSound(null, null)
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Processing SMS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 99
        const val CHANNEL_ID      = "sms_processing_silent"
    }
}