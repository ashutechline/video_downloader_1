package com.quickvideodownloader.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.quickvideodownloader.app.data.local.entity.LockedVideoEntity
import com.quickvideodownloader.app.presentation.viewmodel.LockedVideosViewModel
import com.quickvideodownloader.app.presentation.ui.components.CommonTopBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedVideosScreen(
    viewModel: LockedVideosViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onVideoClick: (String) -> Unit
) {
    val context = LocalContext.current
    val lockedVideos by viewModel.lockedVideos.collectAsState()
    var selectedVideoForActions by remember { mutableStateOf<LockedVideoEntity?>(null) }
    
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    Scaffold(
        topBar = {
            CommonTopBar(title = "Locked Vault", onBackClick = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (lockedVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your vault is empty", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(lockedVideos, key = { it.id }) { video ->
                    LockedVideoItem(
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
            onUnlock = { viewModel.unlockVideo(video) },
            onDelete = { viewModel.deletePermanently(video) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoActionsBottomSheet(
    video: LockedVideoEntity,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
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
                headlineContent = { Text("Unlock & Remove") },
                leadingContent = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                modifier = Modifier.clickable { onUnlock(); onDismiss() }
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
fun LockedVideoItem(
    video: LockedVideoEntity,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
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
            
            // Lock icon overlay
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(16.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeMb = video.size / (1024 * 1024).toDouble()
                Text(
                    text = String.format("%.1f MB", sizeMb),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Box {
                IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                }
            }
        }
    }
}
