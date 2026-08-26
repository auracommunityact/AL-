package com.example.ui.study.planner

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("planner_settings", Context.MODE_PRIVATE) }
    
    var defaultReminderTime by remember { mutableStateOf(sharedPrefs.getInt("default_reminder_time", 5)) }
    var vibrationEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("vibration_enabled", true)) }
    var timeFormat24 by remember { mutableStateOf(sharedPrefs.getBoolean("time_format_24", false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planner Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Default Reminder Time (mins before)", style = MaterialTheme.typography.titleMedium)
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = if (defaultReminderTime == 0) "At time of event" else "$defaultReminderTime minutes before",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(0, 5, 10, 15, 30, 60).forEach { mins ->
                        DropdownMenuItem(
                            text = { Text(if (mins == 0) "At time of event" else "$mins minutes before") },
                            onClick = {
                                defaultReminderTime = mins
                                sharedPrefs.edit().putInt("default_reminder_time", mins).apply()
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { 
                        vibrationEnabled = it
                        sharedPrefs.edit().putBoolean("vibration_enabled", it).apply()
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24-Hour Time Format", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = timeFormat24,
                    onCheckedChange = { 
                        timeFormat24 = it
                        sharedPrefs.edit().putBoolean("time_format_24", it).apply()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { testNotification(context) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Test Notification")
            }
        }
    }
}

fun testNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "study_alarms",
            "Study Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarms for scheduled study sessions"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    val timeStr = timeFormat.format(java.util.Date())

    val notification = NotificationCompat.Builder(context, "study_alarms")
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("📚 Time to Study! (Test)")
        .setContentText("Test Subject - Test Topic starts at \$timeStr.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(999, notification)
}
