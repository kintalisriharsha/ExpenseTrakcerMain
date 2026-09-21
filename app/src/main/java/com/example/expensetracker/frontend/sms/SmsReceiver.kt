package com.example.expensetracker.frontend.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.annotation.RequiresApi

class SmsReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        var sender: String? = null
        var timestampMillis: Long = 0L
        val fullMessage = StringBuilder()

        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
            sender = sms.originatingAddress          // separate variable, not appended
            timestampMillis = sms.timestampMillis     // when the carrier actually sent this SMS,
            fullMessage.append(sms.displayMessageBody) // StringBuilder only reassembles multipart body
        }

        val serviceIntent = Intent(context, SmsProcessingService::class.java).apply {
            putExtra("sender", sender ?: "")
            putExtra("body", fullMessage.toString())
            putExtra("timestamp", timestampMillis)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        println("sender: $sender, body: $fullMessage")
    }
}