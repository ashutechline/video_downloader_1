package com.quickvideodownloader.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.Manifest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.quickvideodownloader.app.R
import com.quickvideodownloader.app.presentation.ui.components.DownloadManagerCard
import com.quickvideodownloader.app.presentation.ui.components.FeatureCard
import com.quickvideodownloader.app.presentation.ui.components.RatingDialog
import com.quickvideodownloader.app.presentation.ui.components.openPlayStore
import com.quickvideodownloader.app.presentation.ui.theme.VideoDownloaderTheme
import com.quickvideodownloader.app.presentation.viewmodel.HomeViewModel
import com.quickvideodownloader.app.presentation.viewmodel.RatingViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    ratingViewModel: RatingViewModel = hiltViewModel(),
    onFromLinkClick: () -> Unit = {},
    onDirectChatClick: () -> Unit = {},
    onStatusSaverClick: () -> Unit = {},
    onDeviceVideosClick: () -> Unit = {},
    onLockedVideosClick: () -> Unit = {},
    onHiddenVideosClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lockedCount by viewModel.lockedVideosCount.collectAsState()
    val hiddenCount by viewModel.hiddenVideosCount.collectAsState()
    val ratingState by ratingViewModel.uiState.collectAsState()
    val notificationAsked by viewModel.notificationPermissionAsked.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(notificationAsked) {
        if (!notificationAsked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.setNotificationPermissionAsked()
        }
    }

    LaunchedEffect(Unit) {
        delay(60000) // 1 minute
        val currentTime = System.currentTimeMillis()
        if (!ratingState.hasRated && currentTime >= ratingState.showLaterTime) {
            ratingViewModel.onShowDialog()
        }
    }

    if (ratingState.showDialog) {
        RatingDialog(
            selectedRating = ratingState.selectedRating,
            onRatingSelected = { ratingViewModel.onRatingSelected(it) },
            onSubmit = { rating ->
                ratingViewModel.submitRating(rating) {
                    openPlayStore(context)
                }
            },
            onLater = { ratingViewModel.onShowLater() },
            onDismiss = { ratingViewModel.onDismissDialog() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item(span = { GridItemSpan(2) }) {
                HomeHeader()
            }

            // Feature Grid Section
            item {
                FeatureCard(
                    title = stringResource(R.string.from_link_title),
                    subtitle = stringResource(R.string.from_link_subtitle),
                    icon = Icons.Default.Link,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onFromLinkClick
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.direct_chat_title),
                    subtitle = stringResource(R.string.direct_chat_subtitle),
                    icon = Icons.Default.Chat,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onDirectChatClick
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.status_saver_title),
                    subtitle = stringResource(R.string.status_saver_subtitle),
                    icon = Icons.Default.DownloadForOffline,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onStatusSaverClick
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.device_videos_title),
                    subtitle = stringResource(R.string.device_videos_subtitle),
                    icon = Icons.Default.Smartphone,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onDeviceVideosClick
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.locked_videos),
                    subtitle = if (lockedCount > 0) stringResource(R.string.videos_secured, lockedCount) else stringResource(R.string.locked_videos_subtitle),
                    icon = Icons.Default.Lock,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onLockedVideosClick
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.hidden_videos),
                    subtitle = if (hiddenCount > 0) stringResource(R.string.videos_hidden, hiddenCount) else stringResource(R.string.hidden_videos_subtitle),
                    icon = Icons.Default.VisibilityOff,
                    iconColor = Color(0xFFF44336), // Red
                    onClick = onHiddenVideosClick
                )
            }

            // Downloads Manager Section
            item(span = { GridItemSpan(2) }) {
                DownloadManagerCard(
                    onClick = onDownloadsClick
                )
            }
            
            // Bottom spacing
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_header_greeting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.profile_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Profile Image Placeholder
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    VideoDownloaderTheme {
        HomeScreen()
    }
}
