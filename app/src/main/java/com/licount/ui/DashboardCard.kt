package com.licount.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.licount.ui.theme.rememberIsDarkTheme

/** 卡片默认内边距。 */
val DashboardCardPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)

private data class ShadowSpec(val alpha: Float, val dy: Dp, val blur: Dp, val spread: Dp = 0.dp)

private val LightShadows = listOf(
    ShadowSpec(0x0D / 255f, 0.5.dp, 1.dp),
    ShadowSpec(0x0A / 255f, 2.dp, 6.dp),
    ShadowSpec(0x17 / 255f, 12.dp, 32.dp, (-6).dp)
)

private val DarkShadows = listOf(
    ShadowSpec(0x80 / 255f, 1.dp, 2.dp),
    ShadowSpec(0x8C / 255f, 10.dp, 28.dp, (-6).dp)
)

/** 沿超椭圆轮廓画多层投影。 */
internal fun Modifier.squircleShadow(radius: Dp, dark: Boolean): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return shadow(2.dp, SquircleShape(radius), clip = false)
    }
    val specs = if (dark) DarkShadows else LightShadows
    return drawWithCache {
        val radiusPx = radius.toPx()
        val layers = specs.mapNotNull { spec ->
            val inset = -spec.spread.toPx()
            val insetSize = Size(
                (size.width - inset * 2f).coerceAtLeast(0f),
                (size.height - inset * 2f).coerceAtLeast(0f)
            )
            if (insetSize.width <= 0f || insetSize.height <= 0f) return@mapNotNull null

            val path = squirclePath(insetSize, (radiusPx - inset).coerceAtLeast(0f))
            path.translate(Offset(inset, inset))

            val paint = Paint()
            val fw = paint.asFrameworkPaint()
            fw.isAntiAlias = true
            fw.setColor(android.graphics.Color.TRANSPARENT)
            fw.setShadowLayer(
                spec.blur.toPx(),
                0f,
                spec.dy.toPx(),
                Color.Black.copy(alpha = spec.alpha).toArgb()
            )
            path to paint
        }
        onDrawBehind {
            drawIntoCanvas { canvas ->
                for ((path, paint) in layers) canvas.drawPath(path, paint)
            }
        }
    }
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    radius: Dp = CardCorner,
    color: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues? = null,
    content: @Composable () -> Unit
) {
    val dark = rememberIsDarkTheme()
    val shape = SquircleShape(radius)
    val baseModifier = modifier.squircleShadow(radius, dark).clip(shape)

    val body: @Composable () -> Unit = if (contentPadding != null) {
        { Box(Modifier.padding(contentPadding)) { content() } }
    } else {
        content
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = baseModifier,
            shape = shape,
            color = color,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) { body() }
    } else {
        Surface(
            modifier = baseModifier,
            shape = shape,
            color = color,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) { body() }
    }
}
