package com.qingzhang.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

/** 对应 FlClash 的 PressSurface：按下时整块表面短促底色淡入，不是水波纹。 */
@Composable
fun PressSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = SquircleShape(ControlCorner),
    enabled: Boolean = true,
    role: Role? = null,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "press-fill"
    )
    val fill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f * alpha)

    Box(
        modifier
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = onClick
            )
            .background(fill)
    ) { content() }
}
