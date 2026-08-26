package com.example.ui.study.planner

import android.app.KeyguardManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlannerDatabase
import com.example.data.repository.PlannerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure this activity shows over the lock screen and turns the screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val subject = intent.getStringExtra("SUBJECT") ?: "Study Time"
        val topic = intent.getStringExtra("TOPIC") ?: "Let's focus"
        val sessionId = intent.getLongExtra("SESSION_ID", -1)

        startAlarmSound()

        setContent {
            MaterialTheme {
                AlarmScreenContent(
                    subject = subject,
                    topic = topic,
                    onDismiss = {
                        stopAlarmSound()
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = PlannerDatabase.getDatabase(this@AlarmActivity)
                            val repo = PlannerRepository(db.studySessionDao())
                            val session = repo.getSessionById(sessionId)
                            if (session != null) {
                                repo.updateSession(session.copy(completedStatus = "SKIPPED"))
                            }
                        }
                        finish()
                    },
                    onSnooze = {
                        stopAlarmSound()
                        // Reschedule alarm for 5 mins later
                        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                        val snoozeIntent = android.content.Intent(this, AlarmReceiver::class.java).apply {
                            action = "com.example.ALARM_TRIGGERED"
                            putExtra("SESSION_ID", sessionId)
                            putExtra("SUBJECT", subject)
                            putExtra("TOPIC", topic)
                        }
                        val pendingIntent = android.app.PendingIntent.getBroadcast(
                            this,
                            sessionId.toInt(),
                            snoozeIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            snoozeTime,
                            pendingIntent
                        )
                        finish()
                    },
                    onStartStudy = {
                        stopAlarmSound()
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = PlannerDatabase.getDatabase(this@AlarmActivity)
                            val repo = PlannerRepository(db.studySessionDao())
                            val session = repo.getSessionById(sessionId)
                            if (session != null) {
                                repo.updateSession(session.copy(completedStatus = "COMPLETED"))
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun startAlarmSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()
        
        val sharedPrefs = getSharedPreferences("planner_settings", Context.MODE_PRIVATE)
        val vibrationEnabled = sharedPrefs.getBoolean("vibration_enabled", true)
        
        if (vibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            val timings = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, 0)
            }
        }
    }

    private fun stopAlarmSound() {
        ringtone?.stop()
        vibrator?.cancel()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
    }
}

@Composable
fun AlarmScreenContent(
    subject: String,
    topic: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onStartStudy: () -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(currentTime))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedTime,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "\"Success starts with today's study.\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onStartStudy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Studying", fontSize = 18.sp)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Filled.Snooze, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Snooze")
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Filled.AlarmOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dismiss")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
