package com.example.mednot.Utils // Assuming a 'Utils' package for helper classes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.mednot.AlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules a single, non-repeating alarm.
     * @param medicineName The name of the medicine for the notification.
     * @param dosage The dosage information for the notification.
     * @param time The time string (e.g., "08:00") when the medicine should be taken.
     * @param requestCode A unique ID for the alarm (e.g., hash of reminder ID).
     */
    fun scheduleSingleAlarm(medicineName: String, dosage: String, time: String, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MEDICINE_NAME", medicineName)
            putExtra("DOSAGE", dosage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse time string (e.g., "08:00")
        val (hour, minute) = time.split(":").map { it.toInt() }

        // Set the alarm time
        val calendar: Calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // If the time has already passed today, set it for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Use AlarmManager.RTC_WAKEUP for an exact, system-waking alarm
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d("AlarmScheduler", "Alarm set for $medicineName at ${calendar.time} with request code $requestCode")
        Toast.makeText(context, "Reminder set for $medicineName!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Cancels an existing alarm using its unique request code.
     */
    fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("AlarmScheduler", "Alarm cancelled for request code $requestCode")
        }
    }
}