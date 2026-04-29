package com.quickvideodownloader.app.presentation.lock

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.quickvideodownloader.app.core.utils.BiometricHelper

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    onNavigateBack: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val biometricHelper = remember { BiometricHelper(context) }

    // Navigation trigger
    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) {
            onUnlockSuccess()
        }
    }

    // Shake animation and vibration for error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.isError) {
        if (state.isError) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            repeat(6) {
                shakeOffset.animateTo(
                    targetValue = if (it % 2 == 0) 10f else -10f,
                    animationSpec = tween(50)
                )
            }
            shakeOffset.animateTo(0f)
        }
    }

    val backgroundColor = Color(0xFF151B25)
    val cardColor = Color(0xFF1E2633)
    val accentColor = Color(0xFFFF6A00)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Lock Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = accentColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title & Subtitle
        Text(
            text = if (state.isFirstTime) "Set PIN Code" else "Locked Videos",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (state.isFirstTime) "Create a 4-digit security code" else "Enter your security code",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Dots
        Row(
            modifier = Modifier.offset(x = shakeOffset.value.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) { index ->
                val isFilled = index < state.enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) accentColor else Color.Transparent
                        )
                        .then(
                            if (!isFilled) Modifier.background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) else Modifier
                        )
                        .then(
                            if (!isFilled) Modifier.padding(2.dp).clip(CircleShape).background(backgroundColor) else Modifier
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Biometric Button
        if (!state.isFirstTime && biometricHelper.isBiometricAvailable()) {
            Button(
                onClick = {
                    biometricHelper.showBiometricPrompt(
                        activity = context as FragmentActivity,
                        onSuccess = { viewModel.onBiometricSuccess() },
                        onError = { /* Handle error if needed */ }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Use Fingerprint", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Number Keypad
        NumberKeypad(
            onNumberClick = viewModel::onNumberClick,
            onDelete = viewModel::onDelete
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Removed manual Unlock button as it's now automatic
        Spacer(modifier = Modifier.height(24.dp))

        // Auto lock toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto lock after exit",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = state.isAutoLockEnabled,
                onCheckedChange = viewModel::toggleAutoLock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Forgot PIN?",
            color = accentColor,
            modifier = Modifier.clickable { /* Handle forgot PIN */ },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NumberKeypad(
    onNumberClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "delete")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { item ->
                    KeypadButton(
                        text = item,
                        onClick = {
                            when (item) {
                                "delete" -> onDelete()
                                "" -> {}
                                else -> onNumberClick(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text == "delete") {
            Icon(
                imageVector = Icons.Outlined.Backspace,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        } else if (text.isNotEmpty()) {
            Text(
                text = text,
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
