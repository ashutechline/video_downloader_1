package com.quickvideodownloader.app.presentation.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.net.URLDecoder

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    uriStr: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val decodedUri = remember(uriStr) { URLDecoder.decode(uriStr, "UTF-8") }
    
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    
    var skipFeedbackBy by remember { mutableLongStateOf(0L) }
    var showSkipFeedback by remember { mutableStateOf(false) }

    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var videoBrightness by rememberSaveable { mutableFloatStateOf(0.7f) }
    
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val configurationLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3000)
            isControlsVisible = false
        }
    }

    DisposableEffect(Unit) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(decodedUri)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val isLeft = offset.x < size.width / 2
                        if (isLeft) {
                            showBrightnessIndicator = true
                        } else {
                            showVolumeIndicator = true
                            volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                        }
                    },
                    onDragEnd = {
                        showBrightnessIndicator = false
                        showVolumeIndicator = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (change.position.x < size.width / 2) {
                            videoBrightness = (videoBrightness - (dragAmount / 300f)).coerceIn(0f, 1f)
                        } else {
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = if (dragAmount > 0) -1 else 1
                            val newV = (currentVol + delta).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newV, 0)
                            volumeLevel = newV.toFloat() / maxVolume
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isControlsVisible = !isControlsVisible },
                    onDoubleTap = { offset ->
                        val duration = exoPlayer.duration
                        val current = exoPlayer.currentPosition
                        val isLeft = offset.x < size.width / 2
                        
                        if (isLeft) {
                            exoPlayer.seekTo((current - 10000).coerceAtLeast(0))
                            skipFeedbackBy = -10
                        } else {
                            // Only skip forward if duration is known and valid
                            if (duration != C.TIME_UNSET) {
                                exoPlayer.seekTo((current + 10000).coerceAtMost(duration))
                            } else {
                                exoPlayer.seekTo(current + 10000)
                            }
                            skipFeedbackBy = 10
                        }
                        showSkipFeedback = true
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } },
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - videoBrightness) * 0.8f))
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GestureIndicator(visible = showBrightnessIndicator, icon = Icons.Default.Brightness6, value = videoBrightness, label = "Brightness")
            GestureIndicator(visible = showVolumeIndicator, icon = if (volumeLevel == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, value = volumeLevel, label = "Volume")
            
            LaunchedEffect(showSkipFeedback) {
                if (showSkipFeedback) {
                    delay(500)
                    showSkipFeedback = false
                }
            }
            AnimatedVisibility(visible = showSkipFeedback, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.Black.copy(0.6f), CircleShape).padding(20.dp)) {
                    Icon(
                        if (skipFeedbackBy > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                        null, tint = Color.White, modifier = Modifier.size(40.dp)
                    )
                    Text("${if (skipFeedbackBy > 0) "+" else ""}${skipFeedbackBy}s", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(visible = isControlsVisible, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(top = 24.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(1f) }) { Icon(Icons.Default.ZoomOut, null, tint = Color.White) }
                    IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(3f) }) { Icon(Icons.Default.ZoomIn, null, tint = Color.White) }
                    IconButton(onClick = {
                        activity?.requestedOrientation = if (configurationLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }) {
                        Icon(if (configurationLandscape) Icons.Default.ScreenLockPortrait else Icons.Default.ScreenLockLandscape, null, tint = Color.White)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControls(player = exoPlayer, isPlaying = isPlaying, onTogglePlay = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play(); isPlaying = !isPlaying })
        }
    }
}

@Composable
fun GestureIndicator(visible: Boolean, icon: ImageVector, value: Float, label: String) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(16.dp)).padding(24.dp).width(120.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary, trackColor = Color.White.copy(0.3f))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomControls(player: Player, isPlaying: Boolean, onTogglePlay: () -> Unit) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player) { while (true) { position = player.currentPosition; duration = player.duration.coerceAtLeast(0L); kotlinx.coroutines.delay(1000) } }
    Column(Modifier.fillMaxWidth().background(Color.Black.copy(0.4f)).padding(16.dp)) {
        Slider(value = if (duration > 0) position.toFloat() / duration else 0f, onValueChange = { player.seekTo((it * duration).toLong()) }, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(formatTime(position), color = Color.White, fontSize = 12.sp)
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms / 1000; return String.format("%02d:%02d", s / 60, s % 60) }
