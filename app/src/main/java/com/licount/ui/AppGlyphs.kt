package com.licount.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal const val MAIN_STROKE = 1.8f
internal const val DETAIL_STROKE = 1.5f

internal fun glyph(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(build).build()

internal fun ImageVector.Builder.strokePath(
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

internal fun PathBuilder.box(l: Float, t: Float, r: Float, b: Float, rad: Float) {
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

internal fun PathBuilder.ellipse(cx: Float, cy: Float, rx: Float, ry: Float) {
    moveTo(cx - rx, cy)
    arcTo(rx, ry, 0f, true, true, cx + rx, cy)
    arcTo(rx, ry, 0f, true, true, cx - rx, cy)
    close()
}

internal fun PathBuilder.hLine(x1: Float, x2: Float, y: Float) {
    moveTo(x1, y)
    lineTo(x2, y)
}

internal fun PathBuilder.vLine(x: Float, y1: Float, y2: Float) {
    moveTo(x, y1)
    lineTo(x, y2)
}

/** 全 App 共用的线性图标：24x24、1.8dp 圆头描边，通过 Icon 的 tint 上色。 */
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

    /** 日历：带装订环的月历框。 */
    val calendar: ImageVector by lazy {
        glyph("Calendar") {
            strokePath {
                box(3.5f, 5f, 20.5f, 20f, 2f)
                hLine(4f, 20f, 9.5f)
                moveTo(8f, 3f)
                lineTo(8f, 6.5f)
                moveTo(16f, 3f)
                lineTo(16f, 6.5f)
            }
        }
    }

    /** 备份与恢复：归档盒（顶栏入口）。 */
    val archive: ImageVector by lazy {
        glyph("Archive") {
            strokePath {
                box(3f, 4f, 21f, 9.5f, 1.5f)
                moveTo(4.5f, 9.5f)
                lineTo(4.5f, 19.5f)
                quadTo(4.5f, 20.5f, 5.5f, 20.5f)
                lineTo(18.5f, 20.5f)
                quadTo(19.5f, 20.5f, 19.5f, 19.5f)
                lineTo(19.5f, 9.5f)
                hLine(9.5f, 14.5f, 14.5f)
            }
        }
    }

    /** 导出：托盘 + 向上箭头。 */
    val upload: ImageVector by lazy {
        glyph("Upload") {
            strokePath {
                box(3.5f, 9f, 20.5f, 20f, 2f)
                moveTo(12f, 16.5f)
                lineTo(12f, 4f)
                moveTo(8f, 8f)
                lineTo(12f, 4f)
                lineTo(16f, 8f)
            }
        }
    }

    /** 导入：托盘 + 向下箭头。 */
    val download: ImageVector by lazy {
        glyph("Download") {
            strokePath {
                box(3.5f, 9f, 20.5f, 20f, 2f)
                moveTo(12f, 4f)
                lineTo(12f, 16.5f)
                moveTo(8f, 12.5f)
                lineTo(12f, 16.5f)
                lineTo(16f, 12.5f)
            }
        }
    }

    /** 合并导入：两块归并到一处。 */
    val merge: ImageVector by lazy {
        glyph("Merge") {
            strokePath {
                box(3f, 4f, 10f, 11f, 1.5f)
                box(14f, 4f, 21f, 11f, 1.5f)
                moveTo(12f, 13f)
                lineTo(12f, 19.5f)
                moveTo(8.5f, 16f)
                lineTo(12f, 19.5f)
                lineTo(15.5f, 16f)
                hLine(5f, 19f, 21f)
            }
        }
    }

    /** 覆盖恢复：回转箭头。 */
    val replace: ImageVector by lazy {
        glyph("Replace") {
            strokePath {
                moveTo(5f, 11f)
                lineTo(5f, 6f)
                lineTo(18f, 6f)
                lineTo(18f, 13f)
                quadTo(18f, 18f, 13f, 18f)
                lineTo(9f, 18f)
                moveTo(12f, 15f)
                lineTo(9f, 18f)
                lineTo(12f, 21f)
            }
        }
    }

    /** 最近删除：垃圾桶。 */
    val trash: ImageVector by lazy {
        glyph("Trash") {
            strokePath {
                hLine(4f, 20f, 6f)
                hLine(9f, 15f, 3.5f)
                moveTo(6f, 6f)
                lineTo(7f, 20f)
                lineTo(17f, 20f)
                lineTo(18f, 6f)
                vLine(10f, 10f, 16.5f)
                vLine(14f, 10f, 16.5f)
            }
        }
    }

    /** 补记一笔：加号。 */
    val plus: ImageVector by lazy {
        glyph("Plus") {
            strokePath {
                hLine(5f, 19f, 12f)
                vLine(12f, 5f, 19f)
            }
        }
    }

    /** 空状态：空收纳盒。 */
    val empty: ImageVector by lazy {
        glyph("Empty") {
            strokePath {
                box(3f, 7f, 21f, 20f, 2f)
                hLine(3f, 21f, 12f)
                hLine(9f, 15f, 4f)
            }
        }
    }

    /**
     * 今日未记录提醒：铃铛，与分类/导航图标同一套 24 网格、1.8dp 圆头描边。
     * 三笔分离：钟身一笔闭合（圆顶收腰、底沿外扩，close 自带底沿横线）、
     * 顶部短柄与铃舌都留白，缩到 15dp 时不会像旧版那样把笔画粘成一团。
     */
    val notice: ImageVector by lazy {
        glyph("Notice") {
            strokePath {
                moveTo(5f, 17f)
                quadTo(6.5f, 9f, 12f, 5.5f)
                quadTo(17.5f, 9f, 19f, 17f)
                close()
                vLine(12f, 2.8f, 4f)
                moveTo(10f, 19f)
                quadTo(12f, 21.5f, 14f, 19f)
            }
        }
    }
}
