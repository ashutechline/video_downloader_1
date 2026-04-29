package com.example.video_downloder.presentation.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.video_downloder.presentation.state.StatusTab
import com.example.video_downloder.presentation.ui.components.StatusItemView
import com.example.video_downloder.presentation.viewmodel.StatusViewModel
import com.example.video_downloder.presentation.ui.components.CommonTopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.video_downloder.presentation.ui.components.ShimmerGridItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StatusSaverScreen(
    onBackClick: () -> Unit,
    onPreviewClick: (String) -> Unit,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // SAF Folder Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Take persistent permission
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onFolderPermissionGranted(it.toString())
        }
    }

    // Initial URI for WhatsApp Statuses on Android 11+
    val initialUri = remember {
        val path = "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        Uri.parse("content://com.android.externalstorage.documents/document/${path.replace("/", "%2F")}")
    }

    // Standard Permissions State
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    // Automatically launch folder picker if permission is missing (Android 11+)
    LaunchedEffect(uiState.isFolderPermissionGranted, permissionState.allPermissionsGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
            permissionState.allPermissionsGranted && 
            !uiState.isFolderPermissionGranted) {
            folderPickerLauncher.launch(initialUri)
        }
    }

    // Automatically load statuses when standard permission is granted
    // (For Android 11+, we still need folder permission)
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted && uiState.isFolderPermissionGranted) {
            viewModel.loadStatuses()
        }
    }

    // Handle Back Button when selection is active
    BackHandler(enabled = uiState.isMultiSelectEnabled) {
        viewModel.clearSelection()
    }

    // Success/Error Messages
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    val shareStatus: (Uri) -> Unit = { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Status"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CommonTopBar(
                title = "Status Saver",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = viewModel::loadStatuses) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.Black
                        )
                    }
                }
            )

            // Subtitle Section
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "View and save statuses from your device",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )

                StatusTabs(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::selectTab
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!permissionState.allPermissionsGranted) {
                PermissionRequestContent(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !uiState.isFolderPermissionGranted) {
                FolderPermissionContent(
                    onGrantClick = { folderPickerLauncher.launch(initialUri) }
                )
            } else {
                if (uiState.isLoading) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(6) {
                            ShimmerGridItem()
                        }
                    }
                } else if (uiState.filteredStatuses.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyStateContent()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.filteredStatuses, key = { it.id }) { status ->
                            StatusItemView(
                                status = status,
                                onClick = {
                                    if (uiState.isMultiSelectEnabled) {
                                        viewModel.toggleSelection(status)
                                    } else {
                                        onPreviewClick(status.id)
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.isMultiSelectEnabled) {
                                        viewModel.toggleSelection(status)
                                    }
                                },
                                onDownloadClick = {
                                    viewModel.saveStatus(status)
                                },
                                onShareClick = {
                                    shareStatus(status.uri)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button Overlay
        if (uiState.filteredStatuses.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.saveSelected() }, 
                icon = { Icon(Icons.Default.DownloadDone, contentDescription = null) },
                text = { Text("Save All", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun FolderPermissionContent(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Almost there!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Android 11+ requires you to manually select the WhatsApp status folder to grant access.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Instructions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("1. Click the button below", style = MaterialTheme.typography.bodySmall)
                Text("2. Navigate to Android > media > com.whatsapp > WhatsApp > Media > .Statuses", style = MaterialTheme.typography.bodySmall)
                Text("3. Click 'USE THIS FOLDER'", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Folder & Grant Access")
        }
    }
}

@Composable
fun StatusTabs(
    selectedTab: StatusTab,
    onTabSelected: (StatusTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE9E9F0))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            StatusTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.Gray,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Storage Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "We need storage access to fetch WhatsApp status files from your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun EmptyStateContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.Gray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No statuses found",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Text(
            "Try viewing some statuses on WhatsApp first",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
