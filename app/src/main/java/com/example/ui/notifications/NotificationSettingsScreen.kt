package com.example.ui.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.data.repository.notifications.NotificationRepository
import com.example.data.repository.notifications.NotificationSettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settings = remember { NotificationSettingsRepository(context) }
    val repo = remember { NotificationRepository(context) }
    
    var allEnabled by remember { mutableStateOf(settings.allNotificationsEnabled) }
    var studyRemindersEnabled by remember { mutableStateOf(settings.studyRemindersEnabled) }
    var booksEnabled by remember { mutableStateOf(settings.newBooksEnabled) }
    var videosEnabled by remember { mutableStateOf(settings.newVideosEnabled) }
    var updatesEnabled by remember { mutableStateOf(settings.updatesEnabled) }
    var announcementsEnabled by remember { mutableStateOf(settings.announcementsEnabled) }
    var soundEnabled by remember { mutableStateOf(settings.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(settings.vibrationEnabled) }

    // Track system permission status
    var systemPermissionGranted by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                systemPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationManager.areNotificationsEnabled()
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // SYSTEM PERMISSION BANNER/STATUS CARD
            SettingsSectionHeader(title = "System Status")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (systemPermissionGranted) Color(0xFF4CAF50) else Color(0xFFF44336))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (systemPermissionGranted) "System Notifications Allowed" else "System Notifications Blocked",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (systemPermissionGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (systemPermissionGranted) "Notifications are fully operational on this device." else "Grant permission in system settings to receive any notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            } else {
                                Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                                    putExtra("app_package", context.packageName)
                                    putExtra("app_uid", context.applicationInfo.uid)
                                }
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Open Device Settings",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // GENERAL MASTER SWITCH
            SettingsSectionHeader(title = "General Preferences")
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Enable Notifications",
                    subtitle = "Toggle all in-app and push updates",
                    checked = allEnabled,
                    onCheckedChange = {
                        allEnabled = it
                        settings.allNotificationsEnabled = it
                    }
                )
            }

            // INDIVIDUAL CATEGORIES & SOUND/VIBRATION
            AnimatedVisibility(
                visible = allEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSectionHeader(title = "Notification Categories")
                    SettingsCard {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Alarm,
                            title = "Study Reminders",
                            subtitle = "Alerts for scheduled study events and classes",
                            checked = studyRemindersEnabled,
                            onCheckedChange = {
                                studyRemindersEnabled = it
                                settings.studyRemindersEnabled = it
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsSwitchRow(
                            icon = Icons.Filled.Book,
                            title = "New Books",
                            subtitle = "Notify when new reading materials are released",
                            checked = booksEnabled,
                            onCheckedChange = {
                                booksEnabled = it
                                settings.newBooksEnabled = it
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsSwitchRow(
                            icon = Icons.Filled.PlayCircle,
                            title = "New Videos",
                            subtitle = "Notify when video lessons are uploaded",
                            checked = videosEnabled,
                            onCheckedChange = {
                                videosEnabled = it
                                settings.newVideosEnabled = it
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsSwitchRow(
                            icon = Icons.Filled.SystemUpdate,
                            title = "App Updates",
                            subtitle = "Notify about features, optimizations and versions",
                            checked = updatesEnabled,
                            onCheckedChange = {
                                updatesEnabled = it
                                settings.updatesEnabled = it
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsSwitchRow(
                            icon = Icons.Filled.Campaign,
                            title = "Announcements",
                            subtitle = "Important notices, events and learning news",
                            checked = announcementsEnabled,
                            onCheckedChange = {
                                announcementsEnabled = it
                                settings.announcementsEnabled = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSectionHeader(title = "System Alerts")
                    SettingsCard {
                        SettingsSwitchRow(
                            icon = Icons.Filled.VolumeUp,
                            title = "Sound",
                            subtitle = "Play alert tone when a notification arrives",
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                settings.soundEnabled = it
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsSwitchRow(
                            icon = Icons.Filled.Vibration,
                            title = "Vibration",
                            subtitle = "Vibrate device when a notification is delivered",
                            checked = vibrationEnabled,
                            onCheckedChange = {
                                vibrationEnabled = it
                                settings.vibrationEnabled = it
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DIAGNOSTICS & TESTING
            SettingsSectionHeader(title = "Diagnostics")
            SettingsCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!systemPermissionGranted) {
                                Toast.makeText(context, "Please enable system permissions first!", Toast.LENGTH_LONG).show()
                            } else if (!allEnabled) {
                                Toast.makeText(context, "Please enable notifications inside the app first!", Toast.LENGTH_LONG).show()
                            } else {
                                repo.sendTestNotification(
                                    title = "🔔 Aura Learning Test Notification",
                                    message = "Your notification settings are configured successfully! Happy learning!"
                                )
                                Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Done, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Send Test Notification",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
