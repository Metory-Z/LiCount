package com.qingzhang.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val MAIN_STROKE = 1.8f
private const val DETAIL_STROKE = 1.5f

private fun glyph(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(build).build()

private fun ImageVector.Builder.strokePath(
    width: Float = MAIN_STROKE,
    draw: PathBuilder.() -> Unit
) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = draw
    )
}

private fun PathBuilder.box(l: Float, t: Float, r: Float, b: Float, rad: Float) {
    moveTo(l + rad, t)
    lineTo(r - rad, t)
    quadTo(r, t, r, t + rad)
    lineTo(r, b - rad)
    quadTo(r, b, r - rad, b)
    lineTo(l + rad, b)
    quadTo(l, b, l, b - rad)
    lineTo(l, t + rad)
    quadTo(l, t, l + rad, t)
    close()
}

private fun PathBuilder.ellipse(cx: Float, cy: Float, rx: Float, ry: Float) {
    moveTo(cx - rx, cy)
    arcTo(rx, ry, 0f, true, true, cx + rx, cy)
    arcTo(rx, ry, 0f, true, true, cx - rx, cy)
    close()
}

private fun PathBuilder.hLine(x1: Float, x2: Float, y: Float) {
    moveTo(x1, y)
    lineTo(x2, y)
}

/** 底栏图标。三个 tab：首页(记账)、流水、统计。 */
object AppGlyphs {

    /** 首页：房子（账本归家）。 */
    val home: ImageVector by lazy {
        glyph("Home") {
            strokePath {
                moveTo(4f, 11f)
                lineTo(12f, 4f)
                lineTo(20f, 11f)
                lineTo(20f, 20f)
                quadTo(20f, 21f, 19f, 21f)
                lineTo(5f, 21f)
                quadTo(4f, 21f, 4f, 20f)
                close()
                moveTo(9f, 21f)
                lineTo(9f, 14f)
                lineTo(15f, 14f)
                lineTo(15f, 21f)
            }
        }
    }

    /** 流水：列表三条横线。 */
    val records: ImageVector by lazy {
        glyph("Records") {
            strokePath {
                hLine(4f, 20f, 6f)
                hLine(4f, 20f, 12f)
                hLine(4f, 20f, 18f)
            }
        }
    }

    /** 统计：柱状图。 */
    val stats: ImageVector by lazy {
        glyph("Stats") {
            strokePath {
                moveTo(5f, 20f)
                lineTo(19f, 20f)
                moveTo(7f, 20f)
                lineTo(7f, 14f)
                moveTo(12f, 20f)
                lineTo(12f, 8f)
                moveTo(17f, 20f)
                lineTo(17f, 11f)
            }
        }
    }
}
