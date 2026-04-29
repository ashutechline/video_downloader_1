package com.quickvideodownloader.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quickvideodownloader.app.R
import com.quickvideodownloader.app.presentation.ui.components.CommonTopBar
import com.quickvideodownloader.app.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onLanguageClick: () -> Unit = {},
    onLockScreenClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }
    var showResetAppDialog by remember { mutableStateOf(false) }
    var showDeleteAllDataDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.settings),
                showBackButton = false
            )
        },
        containerColor = Color(0xFFF7F8F9)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Profile Section
            item {
                ProfileHeader()
            }

            // General Section
            item {
                SettingsSection(title = "GENERAL") {
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "Change app language",
                        onClick = onLanguageClick,
                        iconContainerColor = Color(0xFFFFF3E0),
                        iconTint = Color(0xFFFF6A00)
                    )
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        trailing = {
                            Switch(
                                checked = state.darkMode,
                                onCheckedChange = { viewModel.toggleDarkMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF6A00),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        },
                        iconContainerColor = Color(0xFFFFF3E0),
                        iconTint = Color(0xFFFF6A00)
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        trailing = {
                            Switch(
                                checked = state.notifications,
                                onCheckedChange = { viewModel.toggleNotifications() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF6A00),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        },
                        iconContainerColor = Color(0xFFFFF3E0),
                        iconTint = Color(0xFFFF6A00)
                    )
                }
            }

            // Download Settings
            item {
                SettingsSection(title = "DOWNLOAD SETTINGS") {
                    SettingsItem(
                        icon = Icons.Default.FileDownload,
                        title = "Default Download Quality",
                        subtitle = state.downloadQuality,
                        onClick = { showQualityDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Folder,
                        title = "Download Location",
                        subtitle = "/Movies/MyApp",
                        onClick = { /* Navigate to folder picker if needed */ }
                    )
                }
            }

            // Privacy & Security
            item {
                SettingsSection(title = "PRIVACY & SECURITY") {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Locked Videos",
                        subtitle = "Change PIN",
                        onClick = onLockScreenClick
                    )
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Auto Lock",
                        trailing = {
                            Switch(
                                checked = state.autoLock,
                                onCheckedChange = { viewModel.toggleAutoLock() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF6A00),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    )
                }
            }

            // Data Management
            item {
                SettingsSection(title = "DATA MANAGEMENT") {
                    SettingsItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear Downloads",
                        onClick = { showClearDownloadsDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Clear Cache",
                        subtitle = "0.0 MB used",
                        onClick = { showClearCacheDialog = true }
                    )
                }
            }

            // About
            item {
                SettingsSection(title = "ABOUT") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "v1.0.1",
                        showArrow = false
                    )
                    SettingsItem(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        onClick = { /* Open URL */ }
                    )
                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        onClick = { /* Open URL */ }
                    )
                }
            }

            // Danger Zone
            item {
                SettingsSection(title = "DANGER ZONE", titleColor = Color.Red) {
                    SettingsItem(
                        icon = Icons.Default.RestartAlt,
                        title = "Reset App",
                        titleColor = Color.Red,
                        onClick = { showResetAppDialog = true },
                        iconTint = Color.Red,
                        iconContainerColor = Color(0xFFFFEBEE)
                    )
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Delete All Data",
                        titleColor = Color.Red,
                        onClick = { showDeleteAllDataDialog = true },
                        iconTint = Color.Red,
                        iconContainerColor = Color(0xFFFFEBEE)
                    )
                }
            }
        }
    }

    // Dialogs
    if (showClearCacheDialog) {
        ConfirmationDialog(
            title = "Clear Cache",
            message = "Are you sure you want to clear app cache?",
            onConfirm = {
                viewModel.clearCache()
                showClearCacheDialog = false
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    if (showClearDownloadsDialog) {
        ConfirmationDialog(
            title = "Clear Downloads",
            message = "This will delete all downloaded videos. Continue?",
            onConfirm = {
                viewModel.clearDownloads()
                showClearDownloadsDialog = false
            },
            onDismiss = { showClearDownloadsDialog = false }
        )
    }

    if (showResetAppDialog) {
        ConfirmationDialog(
            title = "Reset App",
            message = "This will reset all settings to default. Continue?",
            onConfirm = {
                viewModel.resetApp()
                showResetAppDialog = false
            },
            onDismiss = { showResetAppDialog = false }
        )
    }

    if (showDeleteAllDataDialog) {
        ConfirmationDialog(
            title = "Delete All Data",
            message = "This will delete all downloads and reset the app. This action cannot be undone.",
            onConfirm = {
                viewModel.resetApp()
                showDeleteAllDataDialog = false
            },
            onDismiss = { showDeleteAllDataDialog = false }
        )
    }

    if (showQualityDialog) {
        QualitySelectionDialog(
            currentQuality = state.downloadQuality,
            onQualitySelected = {
                viewModel.setDownloadQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }
}

@Composable
fun ProfileHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                tint = Color.LightGray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Alex",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Manage your preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    titleColor: Color = Color.Gray,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showArrow: Boolean = true,
    titleColor: Color = Color.Black,
    iconTint: Color = Color(0xFF424242),
    iconContainerColor: Color = Color(0xFFF5F5F5)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (showArrow && onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm", color = Color(0xFFFF6A00), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
fun QualitySelectionDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf("HD", "SD", "Auto")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Download Quality", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                qualities.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(quality) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = quality == currentQuality,
                            onClick = { onQualitySelected(quality) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF6A00))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = quality, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
