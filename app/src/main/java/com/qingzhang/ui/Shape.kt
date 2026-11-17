package com.qingzhang.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * 超椭圆（连续）圆角，对应 FlClash prism_ui 的 AppShape/AppCorner。
 * RoundedCornerShape 的圆弧在 22dp 以上会露出明显的转折；超椭圆曲率连续，
 * 这是大圆角卡片不显得"方"的关键。
 */
private const val SUPERELLIPSE_EXPONENT = 5f

/** 胶囊用的指数。n=2 是正圆，两端收成真正的半圆。 */
const val CapsuleExponent = 2f

/** 每个角的采样段数。 */
private const val SEGMENTS_PER_CORNER = 24

private inline fun quadrant(
    radius: Float,
    exponent: Float,
    emit: (dx: Float, dy: Float) -> Unit
) {
    val power = 2f / exponent
    for (step in 0..SEGMENTS_PER_CORNER) {
        val t = (Math.PI / 2.0) * step / SEGMENTS_PER_CORNER
        val dx = radius * cos(t).toFloat().pow(power)
        val dy = radius * sin(t).toFloat().pow(power)
        emit(dx, dy)
    }
}

/** 构造超椭圆圆角矩形路径，供裁剪与投影共用同一轮廓。 */
fun squirclePath(
    size: Size,
    cornerRadius: Float,
    exponent: Float = SUPERELLIPSE_EXPONENT
): Path {
    val w = size.width
    val h = size.height
    val r = cornerRadius.coerceIn(0f, min(w, h) / 2f)
    val path = Path()
    if (r <= 0f) {
        path.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        return path
    }

    path.moveTo(r, 0f)
    path.lineTo(w - r, 0f)
    quadrant(r, exponent) { dx, dy -> path.lineTo(w - r + dy, r - dx) }
    path.lineTo(w, h - r)
    quadrant(r, exponent) { dx, dy -> path.lineTo(w - r + dx, h - r + dy) }
    path.lineTo(r, h)
    quadrant(r, exponent) { dx, dy -> path.lineTo(r - dy, h - r + dx) }
    path.lineTo(0f, r)
    quadrant(r, exponent) { dx, dy -> path.lineTo(r - dx, r - dy) }
    path.close()
    return path
}

/** 固定半径的超椭圆圆角。替代 RoundedCornerShape。 */
@Immutable
data class SquircleShape(val radius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(
        squirclePath(size, with(density) { radius.toPx() })
    )
}

/** 胶囊：半径取短边一半。 */
@Immutable
object SquircleCapsule : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(
        squirclePath(size, min(size.width, size.height) / 2f, CapsuleExponent)
    )
}

/** 卡片与常用控件的圆角档位。 */
val CardCorner = 32.dp
val BlockCorner = 22.dp
val ControlCorner = 14.dp
val IconCorner = 12.dp
