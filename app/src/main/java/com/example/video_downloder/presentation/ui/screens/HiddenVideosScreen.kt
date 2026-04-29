package com.example.video_downloder.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.video_downloder.data.local.entity.HiddenVideoEntity
import com.example.video_downloder.presentation.viewmodel.HiddenVideosViewModel
import com.example.video_downloder.presentation.ui.components.CommonTopBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenVideosScreen(
    viewModel: HiddenVideosViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onVideoClick: (String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val hiddenVideos by viewModel.hiddenVideos.collectAsState()
    var selectedVideoForActions by remember { mutableStateOf<HiddenVideoEntity?>(null) }
    val uiEvent by viewModel.uiEvent.collectAsState()

    val imageLoader = remember {
        ImageLoader.Builder(context).components { add(VideoFrameDecoder.Factory()) }.build()
    }

    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            if (event is HiddenVideosViewModel.UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
                viewModel.clearUiEvent()
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Hidden Videos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E)
                        )
                        Text(
                            text = "Your hidden media files",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(
                        onClick = { /* Search */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }

                // Info Banner
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF1F0))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Hidden videos are not visible in your device gallery",
                            fontSize = 13.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (hiddenVideos.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.unhideAll() },
                    icon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                    text = { Text("Unhide All") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        if (hiddenVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hidden videos", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(hiddenVideos, key = { it.id }) { video ->
                    HiddenVideoItem(
                        video = video,
                        imageLoader = imageLoader,
                        onClick = { onVideoClick(video.path) },
                        onMoreClick = { selectedVideoForActions = video }
                    )
                }
            }
        }
    }

    selectedVideoForActions?.let { video ->
        VideoActionsBottomSheet(
            video = video,
            onDismiss = { selectedVideoForActions = null },
            onUnhide = { viewModel.unhideVideo(video) },
            onDelete = { viewModel.deletePermanently(video) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoActionsBottomSheet(
    video: HiddenVideoEntity,
    onDismiss: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = video.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            ListItem(
                headlineContent = { Text("Unhide Video") },
                leadingContent = { Icon(Icons.Default.Visibility, contentDescription = null) },
                modifier = Modifier.clickable { onUnhide(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text("Delete Permanently", color = Color.Red) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                modifier = Modifier.clickable { onDelete(); onDismiss() }
            )
        }
    }
}

@Composable
fun HiddenVideoItem(
    video: HiddenVideoEntity,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Square thumbnails for better grid look
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEEEEEE))
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(video.path)
                    .videoFrameMillis(1000)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = video.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
            )
            
            // Play icon overlay
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )

            // Duration pill
            val durationText = remember(video.duration) {
                val seconds = (video.duration / 1000) % 60
                val minutes = (video.duration / (1000 * 60)) % 60
                val hours = video.duration / (1000 * 60 * 60)
                if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
                else String.format("%02d:%02d", minutes, seconds)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = durationText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Menu button overlay
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = video.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1C1E),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
