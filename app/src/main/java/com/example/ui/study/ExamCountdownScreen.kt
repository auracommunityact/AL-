package com.example.ui.study

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.ExamDateSheetEntity
import com.example.data.local.PlannerDatabase
import com.example.ui.study.planner.AlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCountdownScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { PlannerDatabase.getDatabase(context) }
    val examDao = db.examDateSheetDao()

    var examList by remember { mutableStateOf<List<ExamDateSheetEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Live tick for countdown calculation
    var currentTick by remember { mutableStateOf(System.currentTimeMillis()) }

    // Dialog state for add/edit
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<ExamDateSheetEntity?>(null) }

    // Form inputs
    var subjectInput by remember { mutableStateOf("") }
    var gradeInput by remember { mutableStateOf("10th") }
    var selectedCalendar by remember { mutableStateOf<Calendar>(Calendar.getInstance()) }
    var dateString by remember { mutableStateOf("") }
    var timeString by remember { mutableStateOf("") }

    // Fetch exams
    fun refreshExams() {
        coroutineScope.launch {
            isLoading = true
            try {
                examList = examDao.getAllExams()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshExams()
        // Live countdown clock ticking every second
        while (true) {
            currentTick = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val upcomingExams = remember(examList, currentTick) {
        examList.filter { it.timestamp > currentTick }
    }

    val nextExam = upcomingExams.firstOrNull()

    // Setup schedule function
    fun scheduleExamNotifications() {
        // Trigger a background schedule for daily notification
        scheduleDailyExamNotification(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam Date Sheet & Countdown", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E3A8A) // Premium Navy Blue
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingExam = null
                    subjectInput = ""
                    gradeInput = "10th"
                    selectedCalendar = Calendar.getInstance()
                    dateString = ""
                    timeString = ""
                    showAddDialog = true
                },
                containerColor = Color(0xFF1E3A8A),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exam")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC)) // Soft, beautiful slate white background
        ) {
            // Top Section: Live Countdown Header
            if (nextExam != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E3A8A), Color(0xFF152A60)) // Premium Navy Blue Gradient
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "NEXT EXAM IS APPROACHING",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = nextExam.subject,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${nextExam.examDate} (${nextExam.examDay}) at ${nextExam.examTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Countdown boxes
                        val diff = nextExam.timestamp - currentTick
                        val days = diff / (24 * 60 * 60 * 1000L)
                        val hours = (diff / (60 * 60 * 1000L)) % 24
                        val minutes = (diff / (60 * 1000L)) % 60
                        val seconds = (diff / 1000L) % 60

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountdownUnitCard(value = days.toString().padStart(2, '0'), label = "Days")
                            Text(":", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            CountdownUnitCard(value = hours.toString().padStart(2, '0'), label = "Hours")
                            Text(":", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            CountdownUnitCard(value = minutes.toString().padStart(2, '0'), label = "Mins")
                            Text(":", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            CountdownUnitCard(value = seconds.toString().padStart(2, '0'), label = "Secs")
                        }
                    }
                }
            } else {
                // No exams scheduled banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.1f), Color(0xFF1E3A8A).copy(alpha = 0.05f))
                            )
                        )
                        .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventNote,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Exams Scheduled yet",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create your custom Date Sheet & preparation planner below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Middle Section: List of Custom Date Sheet Entries
            Text(
                text = "📚 My Exam Date Sheet",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1E3A8A),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1E3A8A))
                }
            } else if (examList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Your Date Sheet is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your school/board exam calendar here to track time and customize resources.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(examList, key = { it.id }) { exam ->
                        val isPassed = exam.timestamp <= currentTick
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Subject icon circle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPassed) Color.LightGray.copy(alpha = 0.3f)
                                            else Color(0xFF1E3A8A).copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = if (isPassed) Color.Gray else Color(0xFF1E3A8A)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Exam Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = exam.subject,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPassed) Color.Gray else Color(0xFF1E3A8A)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = if (isPassed) Color.LightGray else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            Text(
                                                text = exam.grade,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${exam.examDate} (${exam.examDay})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "Time: ${exam.examTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                // Status / Actions
                                Column(horizontalAlignment = Alignment.End) {
                                    if (isPassed) {
                                        Text(
                                            text = "Completed",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Upcoming",
                                            color = Color(0xFF10B981),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingExam = exam
                                                subjectInput = exam.subject
                                                gradeInput = exam.grade
                                                dateString = exam.examDate
                                                timeString = exam.examTime
                                                val splitDate = exam.examDate.split("-")
                                                if (splitDate.size == 3) {
                                                    selectedCalendar.set(Calendar.YEAR, splitDate[0].toInt())
                                                    selectedCalendar.set(Calendar.MONTH, splitDate[1].toInt() - 1)
                                                    selectedCalendar.set(Calendar.DAY_OF_MONTH, splitDate[2].toInt())
                                                }
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF1E3A8A),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        examDao.deleteExamById(exam.id)
                                                        Toast.makeText(context, "Deleted from Date Sheet", Toast.LENGTH_SHORT).show()
                                                        refreshExams()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (editingExam == null) "Add Subject Exam" else "Edit Subject Exam",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("Subject (e.g. Hindi, Mathematics)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.MenuBook, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = gradeInput,
                        onValueChange = { gradeInput = it },
                        label = { Text("Grade / Class (e.g. 10th, 12th)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) }
                    )

                    // Date Picker trigger button
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedCalendar.set(Calendar.YEAR, year)
                                    selectedCalendar.set(Calendar.MONTH, month)
                                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    val formattedMonth = (month + 1).toString().padStart(2, '0')
                                    val formattedDay = dayOfMonth.toString().padStart(2, '0')
                                    dateString = "$year-$formattedMonth-$formattedDay"
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (dateString.isEmpty()) "Select Exam Date" else "Date: $dateString")
                    }

                    // Time Picker trigger button
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    selectedCalendar.set(Calendar.MINUTE, minute)
                                    selectedCalendar.set(Calendar.SECOND, 0)
                                    val isPm = hourOfDay >= 12
                                    val hr = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                    val minStr = minute.toString().padStart(2, '0')
                                    val amPm = if (isPm) "PM" else "AM"
                                    timeString = "${hr.toString().padStart(2, '0')}:$minStr $amPm"
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (timeString.isEmpty()) "Select Exam Time" else "Time: $timeString")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectInput.isBlank() || dateString.isBlank() || timeString.isBlank()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            try {
                                val dayFormat = SimpleDateFormat("EEEE", Locale.US)
                                val computedDay = dayFormat.format(selectedCalendar.time)
                                val finalTimestamp = selectedCalendar.timeInMillis

                                val item = ExamDateSheetEntity(
                                    id = editingExam?.id ?: UUID.randomUUID().toString(),
                                    subject = subjectInput.trim(),
                                    examDate = dateString,
                                    examDay = computedDay,
                                    examTime = timeString,
                                    grade = gradeInput.trim(),
                                    timestamp = finalTimestamp
                                )

                                examDao.insertExam(item)
                                Toast.makeText(context, "Saved to Date Sheet successfully!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                refreshExams()
                                
                                // Schedule alarm/notification
                                scheduleExamNotifications()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CountdownUnitCard(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

// Function to schedule a daily/one-shot background notification checking the next exam countdown
fun scheduleDailyExamNotification(context: Context) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(context, com.example.ui.study.planner.AlarmReceiver::class.java).apply {
            action = "com.example.EXAM_COUNTDOWN_TRIGGERED"
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            8888, // unique request code for exam countdown alerts
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule to trigger every morning or 1 hour from now for testing and background liveness
        val alarmTime = System.currentTimeMillis() + 60 * 60 * 1000L // 1 hour from now

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
                return
            }
        }
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            alarmTime,
            pendingIntent
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
