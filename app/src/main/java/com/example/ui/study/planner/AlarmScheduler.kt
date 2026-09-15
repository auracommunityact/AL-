package com.example.ui.study.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.StudySession

object AlarmScheduler {
    fun scheduleAlarm(context: Context, session: StudySession) {
        if (!session.alarmEnabled || session.completedStatus != "PENDING") return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Prepare intent, pendingIntent and alarmTime before permission checks so they are available everywhere
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ALARM_TRIGGERED"
            putExtra("SESSION_ID", session.id)
            putExtra("SUBJECT", session.subject)
            putExtra("TOPIC", session.topic)
            putExtra("TIME", session.dateMillis)
        }

        val requestCode = session.id.toInt() // ensure this conversion is safe for your IDs
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate alarm time: session.dateMillis - alarmOffsetMins * 60000
        val alarmTime = session.dateMillis - (session.alarmOffsetMins * 60 * 1000L)

        // Don't schedule if it's already in the past
        if (alarmTime < System.currentTimeMillis()) return

        // Check exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmScheduler", "Cannot schedule exact alarms - using inexact fallback")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
                return
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
            Log.d("AlarmScheduler", "Scheduled alarm for session ${session.id} at $alarmTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling alarm", e)
        }
    }

    fun cancelAlarm(context: Context, sessionId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ALARM_TRIGGERED"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm for session $sessionId")
    }
}
