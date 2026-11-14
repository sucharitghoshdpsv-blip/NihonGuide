package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object TravelNotificationHelper {
    const val CHANNEL_ID = "tabiguide_flight_itinerary_alerts"
    const val EXTRA_TITLE = "extra_alert_title"
    const val EXTRA_BODY = "extra_alert_body"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flight & Itinerary Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely alerts for flights, Shinkansen departures, and itinerary stops."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showImmediateAlert(context: Context, title: String, body: String, notificationId: Int = (System.currentTimeMillis() % 100000).toInt()) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun scheduleAlert(
        context: Context,
        title: String,
        body: String,
        delayMillis: Long,
        requestCode: Int
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TravelReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + delayMillis.coerceAtLeast(3_000L)
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}

class TravelReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(TravelNotificationHelper.EXTRA_TITLE) ?: "TabiGuide Travel Alert"
        val body = intent.getStringExtra(TravelNotificationHelper.EXTRA_BODY) ?: "Check your upcoming flight or itinerary stop."
        val id = intent.getIntExtra(TravelNotificationHelper.EXTRA_NOTIFICATION_ID, 1001)
        TravelNotificationHelper.showImmediateAlert(context, title, body, id)
    }
}
