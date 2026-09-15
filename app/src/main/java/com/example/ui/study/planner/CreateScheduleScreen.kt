package com.example.ui.study.planner

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.local.PlannerDatabase
import com.example.data.local.StudySession
import com.example.data.repository.PlannerRepository
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { PlannerDatabase.getDatabase(context) }
    val repo = remember { PlannerRepository(db.studySessionDao()) }
    val coroutineScope = rememberCoroutineScope()

    val defaultOffset = remember { context.getSharedPreferences("planner_settings", android.content.Context.MODE_PRIVATE).getInt("default_reminder_time", 5) }

    var subject by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var alarmEnabled by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(Color(0xFF2196F3)) }
    
    val timeState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE)
    )

    val colors = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Schedule") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Start Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            // Simple time picker inline or dialog, using inline for simplicity here
            TimeInput(state = timeState, modifier = Modifier.align(Alignment.CenterHorizontally))

            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter { char -> char.isDigit() } },
                label = { Text("Duration (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Subject Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color, CircleShape)
                            .clickable { selectedColor = color },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable Alarm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ring when study time starts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (subject.isBlank()) {
                        Toast.makeText(context, "Subject is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    calendar.set(Calendar.MINUTE, timeState.minute)
                    calendar.set(Calendar.SECOND, 0)
                    
                    // If time is in the past, set for tomorrow
                    if (calendar.timeInMillis < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    val session = StudySession(
                        subject = subject,
                        topic = topic,
                        dateMillis = calendar.timeInMillis,
                        durationMins = duration.toIntOrNull() ?: 60,
                        color = selectedColor.toArgb().toLong(),
                        notes = "",
                        repeatType = "ONE_TIME",
                        alarmEnabled = alarmEnabled,
                        alarmOffsetMins = defaultOffset,
                        alarmDurationMins = 5,
                        alarmSoundUri = "",
                        completedStatus = "PENDING"
                    )

                    coroutineScope.launch {
                        val id = repo.insertSession(session)
                        val savedSession = session.copy(id = id)
                        if (alarmEnabled) {
                            AlarmScheduler.scheduleAlarm(context, savedSession)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Schedule", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
