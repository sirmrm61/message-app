package com.sirmrm.messageapp.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Central motion spec: soft springs with slight bounce, short durations,
 * no energetic movement. Every animation in the app should come from here.
 */
object AppMotion {
    val SpringSoft = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    val SpringGentle = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
    val Fast = tween<Float>(durationMillis = 200)
    val Medium = tween<Float>(durationMillis = 300)
    val Slow = tween<Float>(durationMillis = 350)
}

/**
 * Press feedback without ripple: subtle shrink (0.97) + slight fade.
 * Drop-in replacement for clickable in places where the iOS-like look wants
 * no ink ripple.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, AppMotion.SpringSoft, label = "pressScale")
    val alpha by animateFloatAsState(if (pressed) 0.7f else 1f, AppMotion.Fast, label = "pressAlpha")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}
