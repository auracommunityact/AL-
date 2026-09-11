package com.example.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.AppUpdateConfig
import com.example.data.repository.AuraRepository
import com.example.ui.ViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(private val repository: AuraRepository) : ViewModel() {
    private val _updateConfig = MutableStateFlow<AppUpdateConfig?>(null)
    val updateConfig: StateFlow<AppUpdateConfig?> = _updateConfig.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _isForceUpdate = MutableStateFlow(false)
    val isForceUpdate: StateFlow<Boolean> = _isForceUpdate.asStateFlow()

    private val _checkStatus = MutableStateFlow<String?>(null) // Used for manual check UI
    val checkStatus: StateFlow<String?> = _checkStatus.asStateFlow()

    fun checkForUpdates(context: Context, isManualCheck: Boolean = false) {
        viewModelScope.launch {
            if (isManualCheck) _checkStatus.value = "Checking for updates..."
            val config = repository.getLatestAppUpdateConfig()
            if (config != null) {
                val currentVersionCode = getVersionCode(context)
                if (currentVersionCode < config.latestVersionCode) {
                    _updateConfig.value = config
                    _showUpdateDialog.value = true
                    _isForceUpdate.value = config.forceUpdate || (currentVersionCode < config.minimumSupportedVersionCode)
                    if (isManualCheck) _checkStatus.value = null
                } else {
                    if (isManualCheck) _checkStatus.value = "You're up to date"
                }
            } else {
                if (isManualCheck) _checkStatus.value = "Failed to check for updates. Try again later."
            }
        }
    }

    fun dismissUpdateDialog() {
        if (!_isForceUpdate.value) {
            _showUpdateDialog.value = false
        }
    }

    fun resetStatus() {
        _checkStatus.value = null
    }

    private fun getVersionCode(context: Context): Long {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }
}

@Composable
fun AppUpdateDialog(
    viewModel: UpdateViewModel = viewModel(factory = ViewModelFactory)
) {
    val showDialog by viewModel.showUpdateDialog.collectAsState()
    val config by viewModel.updateConfig.collectAsState()
    val isForceUpdate by viewModel.isForceUpdate.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Only run automatic check once per composition (e.g. at app start)
        viewModel.checkForUpdates(context, isManualCheck = false)
    }

    if (showDialog && config != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isForceUpdate) viewModel.dismissUpdateDialog()
            },
            icon = {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (isForceUpdate) "Update Required" else config!!.updateTitle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isForceUpdate) "This version of Aura Learning is no longer supported. Please update to continue." else config!!.updateMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!config!!.changelog.isNullOrBlank() && !isForceUpdate) {
                        Text(
                            text = "Changelog:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = config!!.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version: ${config!!.latestVersionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config!!.downloadUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if browser is missing
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                if (!isForceUpdate) {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Later")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun UpdateSettingsItem(
    viewModel: UpdateViewModel = viewModel(factory = ViewModelFactory)
) {
    val checkStatus by viewModel.checkStatus.collectAsState()
    val context = LocalContext.current
    var currentVersionName by remember { mutableStateOf("1.0.0") }

    LaunchedEffect(Unit) {
        try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            currentVersionName = packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            // Ignored
        }
    }

    com.example.ui.profile.settings.SettingsItem(
        icon = Icons.Default.SystemUpdate,
        title = "Check for Updates",
        subtitle = checkStatus ?: "Current version: $currentVersionName",
        onClick = {
            viewModel.checkForUpdates(context, isManualCheck = true)
        }
    )
}
