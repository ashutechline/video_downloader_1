package com.example.video_downloder.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import android.widget.Toast
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.video_downloder.presentation.ui.theme.VideoDownloaderTheme
import com.example.video_downloder.presentation.ui.components.CommonTopBar
import com.example.video_downloder.presentation.ui.components.AnimatedDownloadDialog

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.video_downloder.R
import com.example.video_downloder.presentation.viewmodel.DownloadLinkViewModel
import com.example.video_downloder.presentation.viewmodel.DownloadLinkState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import android.os.Build
import android.Manifest
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DownloadLinkScreen(
    onBackClick: () -> Unit = {},
    onPreviewClick: (String) -> Unit = {},
    viewModel: DownloadLinkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    val clipboardManager = LocalClipboardManager.current
    var urlText by rememberSaveable { mutableStateOf("") }
    var selectedFormatId by rememberSaveable { mutableStateOf<String?>(null) }
    
    val state by viewModel.state.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val notificationAsked by viewModel.notificationPermissionAsked.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Reset state if URL changes significantly
    LaunchedEffect(urlText) {
        if (urlText.isBlank() && state !is DownloadLinkState.Idle) {
            viewModel.resetState()
            selectedFormatId = null
        }
    }

    // Auto-select first format on success
    LaunchedEffect(state) {
        if (state is DownloadLinkState.Success) {
            val response = (state as DownloadLinkState.Success).data
            if (selectedFormatId == null && !response.formats.isNullOrEmpty()) {
                selectedFormatId = response.formats?.firstOrNull()?.formatId
            }
        }
    }

    // Feedback effect
    LaunchedEffect(state) {
        when (state) {
            is DownloadLinkState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (state as DownloadLinkState.Error).message,
                    duration = SnackbarDuration.Short
                )
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CommonTopBar(
                title = stringResource(R.string.download_from_link),
                onBackClick = onBackClick
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Subtitle/Intro Section
                item {
                    Text(
                        text = stringResource(R.string.paste_video_link),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // URL Input Section
                item {
                    UrlInputField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        onPasteClick = {
                            clipboardManager.getText()?.let {
                                urlText = it.text
                            }
                        }
                    )
                }

                // Video Preview Section (Only shown when successful)
                if (state is DownloadLinkState.Success) {
                    val response = (state as DownloadLinkState.Success).data
                    item {
                        SectionTitle(stringResource(R.string.video_preview))
                        Spacer(modifier = Modifier.height(12.dp))
                        VideoPreviewCard(
                            thumbnailUrl = response.thumbnail ?: "",
                            duration = "${response.duration?.roundToInt() ?: 0}s",
                            title = response.title ?: stringResource(R.string.downloaded_video),
                            subtitle = stringResource(R.string.select_quality)
                        )
                    }

                    // Quality Selection Section
                    item {
                        SectionTitle(stringResource(R.string.select_quality))
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            response.formats?.forEach { format ->
                                QualityOptionItem(
                                    title = format.quality ?: "Unknown Quality",
                                    subtitle = if (format.hasAudio == true) stringResource(R.string.with_audio) else stringResource(R.string.no_audio),
                                    isSelected = selectedFormatId == format.formatId,
                                    onClick = { selectedFormatId = format.formatId }
                                )
                            }
                        }
                    }
                } else if (state is DownloadLinkState.Error) {
                    item {
                        Text(
                            text = (state as DownloadLinkState.Error).message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Download/Fetch Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state is DownloadLinkState.Success) {
                        val response = (state as DownloadLinkState.Success).data
                        DownloadButton(
                            text = stringResource(R.string.download_video),
                            onClick = {
                                if (selectedFormatId != null) {
                                    if (notificationPermissionState?.status?.isGranted == false) {
                                        notificationPermissionState?.launchPermissionRequest()
                                    }
                                    selectedFormatId?.let { formatId ->
                                        val format = response.formats?.find { it.formatId == formatId }
                                        viewModel.downloadVideo(
                                            url = urlText,
                                            formatId = formatId,
                                            quality = format?.quality ?: ""
                                        )
                                    }
                                } else {
                                    Toast.makeText(context, context.getString(R.string.select_quality_first), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else if (state is DownloadLinkState.Idle || state is DownloadLinkState.Error) {
                        DownloadButton(
                            text = stringResource(R.string.fetch_video_info),
                            onClick = {
                                viewModel.fetchVideoInfo(urlText)
                            }
                        )
                    }
                }
            }
        }

        // Snackbar Host
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val backgroundColor = if (state is DownloadLinkState.Error) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    containerColor = backgroundColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(data.visuals.message)
                }
            }
        }

        // --- Overlays and Dialogs ---

        // Loading Bottom Sheet for Fetching Info
        if (state is DownloadLinkState.Loading) {
            ModalBottomSheet(
                onDismissRequest = { },
                dragHandle = null,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            strokeWidth = 8.dp
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 8.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Text(
                        text = stringResource(R.string.fetch_video_info),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1C1E)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.analyzing_link),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Animated Download Progress Dialog
        AnimatedDownloadDialog(
            isVisible = downloadState.isDownloading,
            progress = downloadState.progress,
            downloadedMB = downloadState.downloadedMB,
            totalMB = downloadState.totalMB,
            videoTitle = downloadState.videoTitle,
            downloadSpeed = downloadState.downloadSpeed,
            onCancel = { viewModel.cancelDownload() }
        )

        // Full Screen Success Overlay
        if (state is DownloadLinkState.DownloadSuccess) {
            val successState = state as DownloadLinkState.DownloadSuccess
            DownloadSuccessView(
                videoInfo = successState.videoInfo,
                filePath = successState.filePath,
                onPreviewClick = onPreviewClick,
                onDownloadAnotherClick = {
                    viewModel.resetState()
                    urlText = ""
                }
            )
        }
    }
}

// Removed HeaderSection as it is replaced by CommonTopBar

@Composable
fun UrlInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onPasteClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "https://video.example.com...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        },
        trailingIcon = {
            Button(
                onClick = onPasteClick,
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(36.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true
    )
}

@Composable
fun VideoPreviewCard(
    thumbnailUrl: String,
    duration: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun QualityOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 1.5.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Screenshot shows very subtle shadow
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, Color.LightGray, CircleShape)
                )
            }
        }
    }
}

@Composable
fun DownloadButton(
    text: String, 
    icon: ImageVector = Icons.Default.Download,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = if (containerColor == Color.White) 1.dp else 0.dp,
                color = if (containerColor == Color.White) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1A1C1E)
    )
}

// DownloadProgressBottomSheet removed and replaced by Dialog

@Composable
fun DownloadSuccessView(
    videoInfo: com.example.video_downloder.data.remote.dto.VideoInfoResponse?,
    filePath: String,
    onPreviewClick: (String) -> Unit,
    onDownloadAnotherClick: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CommonTopBar(
                title = stringResource(R.string.download_complete),
                onBackClick = onDownloadAnotherClick,
                actions = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp)
                    )
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Video Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Thumbnail with Play Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.LightGray)
                    ) {
                        AsyncImage(
                            model = videoInfo?.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(60.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                .clickable { onPreviewClick(filePath) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = videoInfo?.title ?: stringResource(R.string.downloaded_video),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "MP4  •  45.2 MB  •  1080p", // Example metadata, should be dynamic if possible
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Primary Action: Save to Gallery
            Button(
                onClick = { 
                    Toast.makeText(context, "Video saved to Gallery!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Save to Gallery", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Secondary Actions: Share and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Share Button
                Button(
                    onClick = { /* Share Logic */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE9E9F0),
                        contentColor = Color.Black
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Delete Button
                Button(
                    onClick = { /* Delete Logic */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFC62828)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Options List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column {
                    OptionListItem(
                        icon = Icons.Default.Edit,
                        title = "Rename File",
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = { /* Rename logic */ }
                    )
                    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    OptionListItem(
                        icon = Icons.Default.PlayCircleOutline,
                        title = "Open in External Player",
                        trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = { onPreviewClick(filePath) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom "Download Another" Text Button (Optional based on design, but good for UX)
            TextButton(
                onClick = onDownloadAnotherClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home", color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}
}

@Composable
fun OptionListItem(
    icon: ImageVector,
    title: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF7F8F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1C1E),
            modifier = Modifier.weight(1f)
        )
        Icon(imageVector = trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadLinkScreenPreview() {
    VideoDownloaderTheme {
        DownloadLinkScreen()
    }
}
