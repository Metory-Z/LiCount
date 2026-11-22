package com.licount.ui

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 分类图标：与底栏 / 顶栏同一套 24x24、1.8dp 圆头线性描边，靠 Icon 的 tint 随主题上色。
 * 原先用的是彩色 emoji，和全 App 的单色线稿不是一种语言，这里统一重画。
 *
 * 图标只按 key 映射，不写进 [com.licount.data.Category]：数据层保持纯数据，
 * 老备份里存的 category key 不受影响。
 */
object CategoryGlyphs {

    /** 餐饮：碗 + 两缕热气。 */
    val food: ImageVector by lazy {
        glyph("CatFood") {
            strokePath {
                hLine(4f, 20f, 11f)
                moveTo(5f, 11f)
                quadTo(5.5f, 19f, 12f, 19f)
                quadTo(18.5f, 19f, 19f, 11f)
                moveTo(9f, 4f)
                quadTo(7.8f, 6f, 9f, 8f)
                moveTo(15f, 4f)
                quadTo(13.8f, 6f, 15f, 8f)
            }
        }
    }

    /** 交通：公交车正面 + 两轮。 */
    val transport: ImageVector by lazy {
        glyph("CatTransport") {
            strokePath {
                box(5f, 3.5f, 19f, 17f, 2f)
                hLine(5f, 19f, 10f)
                hLine(8f, 16f, 13.5f)
                ellipse(8.5f, 18.5f, 1.2f, 1.2f)
                ellipse(15.5f, 18.5f, 1.2f, 1.2f)
            }
        }
    }

    /** 购物：手提袋。 */
    val shopping: ImageVector by lazy {
        glyph("CatShopping") {
            strokePath {
                moveTo(5f, 8f)
                lineTo(19f, 8f)
                lineTo(18f, 20.5f)
                lineTo(6f, 20.5f)
                close()
                moveTo(9f, 8f)
                lineTo(9f, 6f)
                quadTo(9f, 3f, 12f, 3f)
                quadTo(15f, 3f, 15f, 6f)
                lineTo(15f, 8f)
            }
        }
    }

    /** 娱乐：屏幕 + 播放三角。 */
    val entertainment: ImageVector by lazy {
        glyph("CatEntertainment") {
            strokePath {
                box(3f, 5f, 21f, 19f, 2f)
                moveTo(10f, 9f)
                lineTo(16f, 12f)
                lineTo(10f, 15f)
                close()
            }
        }
    }

    /** 居住：房子 + 窗。 */
    val housing: ImageVector by lazy {
        glyph("CatHousing") {
            strokePath {
                moveTo(4f, 11f)
                lineTo(12f, 4f)
                lineTo(20f, 11f)
                moveTo(6f, 10f)
                lineTo(6f, 20f)
                lineTo(18f, 20f)
                lineTo(18f, 10f)
                box(10f, 14.5f, 14f, 20f, 0.8f)
            }
        }
    }

    /** 医疗：圆形十字。 */
    val medical: ImageVector by lazy {
        glyph("CatMedical") {
            strokePath {
                ellipse(12f, 12f, 8.5f, 8.5f)
                hLine(8f, 16f, 12f)
                vLine(12f, 8f, 16f)
            }
        }
    }

    /** 学习：摊开的书。 */
    val education: ImageVector by lazy {
        glyph("CatEducation") {
            strokePath {
                moveTo(12f, 6.5f)
                quadTo(8f, 4.5f, 4f, 5.5f)
                lineTo(4f, 18.5f)
                quadTo(8f, 17.5f, 12f, 19.5f)
                quadTo(16f, 17.5f, 20f, 18.5f)
                lineTo(20f, 5.5f)
                quadTo(16f, 4.5f, 12f, 6.5f)
                close()
                vLine(12f, 6.5f, 19.5f)
            }
        }
    }

    /** 基金：坐标轴 + 上升折线 + 箭头。 */
    val fund: ImageVector by lazy {
        glyph("CatFund") {
            strokePath {
                vLine(4f, 4f, 21f)
                hLine(4f, 21f, 21f)
                moveTo(6f, 17f)
                lineTo(10f, 13f)
                lineTo(13f, 15f)
                lineTo(19f, 7f)
                hLine(14f, 19f, 7f)
                vLine(19f, 7f, 12f)
            }
        }
    }

    /** 红包：封套 + 弧形袋盖 + 圆形封口。 */
    val redpacket: ImageVector by lazy {
        glyph("CatRedpacket") {
            strokePath {
                box(6f, 3f, 18f, 21f, 2f)
                moveTo(6f, 8.5f)
                quadTo(12f, 13.5f, 18f, 8.5f)
                ellipse(12f, 10.5f, 2f, 2f)
            }
        }
    }

    /** 转账：上下两条方向相反的箭头。 */
    val transfer: ImageVector by lazy {
        glyph("CatTransfer") {
            strokePath {
                hLine(4f, 19f, 9.5f)
                moveTo(16f, 6.5f)
                lineTo(19f, 9.5f)
                lineTo(16f, 12.5f)
                hLine(5f, 20f, 15.5f)
                moveTo(8f, 12.5f)
                lineTo(5f, 15.5f)
                lineTo(8f, 18.5f)
            }
        }
    }

    /** 理发：剪刀（双环 + 交叉刀刃 + 铆钉）。 */
    val haircut: ImageVector by lazy {
        glyph("CatHaircut") {
            strokePath {
                ellipse(7f, 18.5f, 2.6f, 2.6f)
                ellipse(17f, 18.5f, 2.6f, 2.6f)
                moveTo(8.6f, 16.4f)
                lineTo(18.5f, 4f)
                moveTo(15.4f, 16.4f)
                lineTo(5.5f, 4f)
                ellipse(12f, 12f, 0.9f, 0.9f)
            }
        }
    }

    /** 其他：方框内三条待归类横线。 */
    val other: ImageVector by lazy {
        glyph("CatOther") {
            strokePath {
                box(4f, 4f, 20f, 20f, 3f)
                hLine(8f, 16f, 9f)
                hLine(8f, 16f, 12f)
                hLine(8f, 16f, 15f)
            }
        }
    }

    fun byKey(key: String): ImageVector = when (key) {
        "food" -> food
        "transport" -> transport
        "shopping" -> shopping
        "entertainment" -> entertainment
        "housing" -> housing
        "medical" -> medical
        "education" -> education
        "fund" -> fund
        "redpacket" -> redpacket
        "transfer" -> transfer
        "haircut" -> haircut
        else -> other
    }
}
