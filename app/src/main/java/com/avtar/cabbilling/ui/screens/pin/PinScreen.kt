package com.avtar.cabbilling.ui.screens.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.ui.containerViewModel

@Composable
fun PinScreen(onUnlocked: () -> Unit) {
    val vm = containerViewModel { PinViewModel(it.configRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    // Auto-verify as soon as the maximum length is reached.
    LaunchedEffect(state.entered) {
        if (state.entered.length == PinViewModel.MAX_LEN) vm.verify(onUnlocked)
    }
    // Buzz on an incorrect attempt.
    LaunchedEffect(state.error) {
        if (state.error) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Cab Billing", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.locked) "Too many attempts" else "Enter your PIN to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            PinDots(length = state.entered.length, error = state.error)

            Spacer(Modifier.height(14.dp))
            Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                when {
                    state.locked -> Text(
                        "Try again in ${state.cooldownRemaining}s",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    state.error -> Text(
                        "Incorrect PIN",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Keypad(
                enabled = !state.checking && !state.locked,
                checking = state.checking,
                onDigit = { d ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.append(d)
                },
                onBackspace = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.backspace()
                },
                onUnlock = { vm.verify(onUnlocked) }
            )
        }
    }
}

@Composable
private fun PinDots(length: Int, error: Boolean) {
    val filledColor = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(PinViewModel.MAX_LEN) { index ->
            val filled = index < length
            val dotColor by animateColorAsState(
                targetValue = if (filled) filledColor else emptyColor,
                label = "pinDotColor"
            )
            val dotSize by animateDpAsState(
                targetValue = if (filled) 16.dp else 12.dp,
                label = "pinDotSize"
            )
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
private fun Keypad(
    enabled: Boolean,
    checking: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onUnlock: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("123", "456", "789").forEach { rowDigits ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                rowDigits.forEach { d ->
                    KeyButton(enabled = enabled, onClick = { onDigit(d) }) {
                        Text(d.toString(), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            KeyButton(enabled = enabled, filled = false, onClick = onBackspace) {
                Icon(Icons.Filled.Backspace, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            KeyButton(enabled = enabled, onClick = { onDigit('0') }) {
                Text("0", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            KeyButton(enabled = enabled, filled = true, accent = true, onClick = onUnlock) {
                if (checking) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Check, contentDescription = "Unlock", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    enabled: Boolean,
    onClick: () -> Unit,
    filled: Boolean = true,
    accent: Boolean = false,
    content: @Composable () -> Unit
) {
    val bg: Color = when {
        accent -> MaterialTheme.colorScheme.primary
        filled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    Surface(
        shape = CircleShape,
        color = bg,
        modifier = Modifier.size(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}
