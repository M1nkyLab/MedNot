package com.example.mednot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "mednot_channel"
    private val CHANNEL_NAME = "Medication Reminders"

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        val dosage = intent.getStringExtra("DOSAGE") ?: "Dosage not specified"
        val notificationId = intent.getIntExtra("REQUEST_CODE", 0) // Use request code as ID

        // 1. Create a notification channel (needed for Android 8.0+)
        createNotificationChannel(context)

        // 2. Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .setContentTitle("💊 Time for your $medicineName")
            .setContentText("Take: $dosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // 3. Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    // Helper function to create the notification channel
    private fun createNotificationChannel(context: Context) {
        // The channel object is required for Android 8.0 (Oreo) and above.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Reminders for taking medication"
            }
            // Register the channel with the system
            val notificationManager: android.app.NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}