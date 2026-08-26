package com.example.ui.study.planner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.PlannerDatabase
import com.example.data.repository.PlannerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.d("BootReceiver", "Boot completed, rescheduling alarms...")
            val db = PlannerDatabase.getDatabase(context)
            val repo = PlannerRepository(db.studySessionDao())
            
            CoroutineScope(Dispatchers.IO).launch {
                val upcoming = repo.getUpcomingAlarmSessions(System.currentTimeMillis())
                for (session in upcoming) {
                    AlarmScheduler.scheduleAlarm(context, session)
                }
            }
        }
    }
}
