package com.licount.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.MoneyInput
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 首页：一整块记账面板，整页不滚动。
 *
 * 顶部一条紧凑的本月收支信息条；下方一张完整输入卡，内部依次是
 * 提醒槽（固定高度预留，有无提醒均占位）、金额展示区（居中吸收剩余高度）、
 * 收支切换、单行横滑分类、数字键盘与保存。
 * 键盘固定行高，始终位于卡片底部、悬浮导航栏上方，任何屏幕高度下都无需上滑。
 * 矮屏通过 BoxWithConstraints 自动收紧间距与按键高度，不引入滚动。
 */
@Composable
fun HomeScreen(
    monthExpense: Long,
    monthIncome: Long,
    todayRecorded: Boolean,
    onSave: (amountFen: Long, category: String) -> Unit,
    categoryCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    // 使用次数多的分类排前面；次数相同（含 0 次）保持预设顺序
    val orderedCategories = remember(categoryCounts) {
        Category.presets.sortedByDescending { categoryCounts[it.key] ?: 0 }
    }
    // 用 rememberSaveable：旋转屏幕、切到别的 App 再回来时，输了一半的金额不会丢
    var amountText by rememberSaveable { mutableStateOf("") }
    var isIncome by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf(orderedCategories.first().key) }

    // 按字符串解析，不经过 Double，避免 0.29 → 28 分这类浮点截断
    val amountFen = MoneyInput.parseYuanToFen(amountText)
    val canSave = amountFen > 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = bottomInset)
    ) {
        // 可用高度偏小时整体降一档：优先保证键盘与保存完整可见
        val compact = maxHeight < 600.dp
        // 两张卡片之间的间距要大于卡内间距，形成 16 > 12 > 8 > 6 的层级
        val outerGap = if (compact) 10.dp else 16.dp
        // 卡内间距适度加大，把原本堆在金额上方的大片空白匀给控件之间
        val innerGap = if (compact) 8.dp else 12.dp
        val cardPadding = if (compact) 10.dp else 12.dp
        val keyGap = if (compact) 4.dp else 6.dp
        // 键盘整行定高，退格键与数字键完全等高
        val keyHeight = if (compact) 50.dp else 56.dp
        val savePadding = if (compact) 9.dp else 13.dp
        val chipPadding = if (compact) 6.dp else 8.dp
        val baseAmountSize = if (compact) 34.sp else 42.sp
        val amountFontSize = if (amountText.length > 8) {
            if (compact) 28.sp else 32.sp
        } else {
            baseAmountSize
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(outerGap)
        ) {
            // ===== 顶部信息条：本月支出 / 收入，固定一行 =====
            DashboardCard(
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = if (compact) 8.dp else 10.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "本月支出",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatMoney(monthExpense),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "本月收入",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatMoney(monthIncome),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ===== 一整张输入卡：金额 / 类型 / 分类 / 键盘 / 保存 =====
            DashboardCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = cardPadding),
                    verticalArrangement = Arrangement.spacedBy(innerGap)
                ) {
                    // 金额展示区：吸收剩余高度，内容垂直居中。
                    // 提醒槽固定高度预留：有没有「今天没记账」，高度恒定，金额行不跳位。
                    val reminderSlotHeight = if (compact) 28.dp else 32.dp
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 固定槽：始终占位，提醒出现/消失时金额不移位
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(reminderSlotHeight),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (!todayRecorded) {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer,
                                            SquircleShape(ControlCorner)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        AppGlyphs.notice,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        "今天还没记账，记一笔吧",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "¥",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (amountText.isEmpty()) "0" else amountText,
                                fontSize = amountFontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )

                    // 支出 / 收入 切换 + 清空
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeToggle(
                            label = "支出",
                            selected = !isIncome,
                            verticalPadding = chipPadding,
                            onClick = { isIncome = false },
                            modifier = Modifier.weight(1f)
                        )
                        TypeToggle(
                            label = "收入",
                            selected = isIncome,
                            verticalPadding = chipPadding,
                            onClick = { isIncome = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 分类：单行横向滑动，不再占两行、不再把内容撑出屏幕
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        orderedCategories.forEach { cat ->
                            CategoryChip(
                                category = cat,
                                selected = selectedCategory == cat.key,
                                verticalPadding = chipPadding,
                                onClick = { selectedCategory = cat.key }
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                    }

                    NumPad(
                        canSave = canSave,
                        keyGap = keyGap,
                        keyHeight = keyHeight,
                        savePadding = savePadding,
                        onInput = { key -> amountText = handleInput(amountText, key) },
                        onLongDelete = { amountText = "" },
                        onSave = {
                            if (canSave) {
                                // 收入记为负数，支出为正数
                                onSave(if (isIncome) -amountFen else amountFen, selectedCategory)
                                amountText = ""
                            }
                        }
                    )
                }
            }
        }
    }
}

/** 支出/收入 切换按钮。 */
@Composable
private fun TypeToggle(
    label: String,
    selected: Boolean,
    verticalPadding: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "type-bg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "type-fg"
    )
    PressSurface(
        onClick = onClick,
        shape = SquircleShape(ControlCorner),
        role = Role.RadioButton,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = fg
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    verticalPadding: Dp,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "cat-bg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "cat-fg"
    )
    PressSurface(
        onClick = onClick,
        shape = SquircleCapsule,
        role = Role.RadioButton
    ) {
        Row(
            modifier = Modifier
                .background(bg)
                .padding(horizontal = 12.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                CategoryGlyphs.byKey(category.key),
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Text(
                category.label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = fg
            )
        }
    }
}

/** 数字键盘：1-9、0、小数点、退格、保存，全部位于同一张输入卡内。 */
@Composable
private fun NumPad(
    canSave: Boolean,
    keyGap: Dp,
    keyHeight: Dp,
    savePadding: Dp,
    onInput: (String) -> Unit,
    onLongDelete: () -> Unit,
    onSave: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(keyGap)) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyHeight),
                horizontalArrangement = Arrangement.spacedBy(keyGap)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onLongClick = if (key == "⌫") onLongDelete else null,
                        onClick = { onInput(key) }
                    )
                }
            }
        }
        // 保存按钮（金额为空时禁用并弱化）
        PressSurface(
            onClick = onSave,
            enabled = canSave,
            shape = SquircleShape(BlockCorner),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (canSave) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .padding(vertical = savePadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "保存",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canSave) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: String,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isDelete = key == "⌫"
    PressSurface(
        onClick = onClick,
        shape = SquircleShape(ControlCorner),
        modifier = modifier,
        onLongClick = onLongClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (isDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除（长按清空）",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    key,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 处理按键输入，限制最多两位小数、最多 8 位整数。 */
private fun handleInput(current: String, key: String): String {
    return when (key) {
        "⌫" -> current.dropLast(1)
        "." -> if (current.contains(".") || current.isEmpty()) current else current + "."
        else -> {
            if (key == "0" && current == "0") return current
            // 限制长度与小数位
            if (current.contains(".")) {
                val decimals = current.substringAfter(".")
                if (decimals.length >= 2) return current
            } else {
                if (current.length >= 7) return current
            }
            current + key
        }
    }
}

/** 金额格式化：分 → 元，千分位；固定 Locale，避免系统语言改变千分位符号。 */
fun formatMoney(fen: Long): String {
    val sign = if (fen < 0) "-" else ""
    val abs = kotlin.math.abs(fen)
    val yuan = abs / 100
    val cents = abs % 100
    val formatted = String.format(Locale.US, "%,d", yuan)
    return if (cents == 0L) "$sign$formatted"
    else "$sign$formatted.${String.format(Locale.US, "%02d", cents)}"
}

private val dayOnlyFormat = SimpleDateFormat("M月d日", Locale.CHINA)
private val dayTimeFormat = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)

/**
 * 时间戳格式化（流水 / 日历明细 / 最近删除共用）。
 * 补记的记录落在当天零点，没有真实消费时刻，只显示日期并标注「补记」；
 * 正常记账的记录显示日期 + 时分。
 */
fun formatDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hasClockTime = cal.get(Calendar.HOUR_OF_DAY) != 0 ||
        cal.get(Calendar.MINUTE) != 0 ||
        cal.get(Calendar.SECOND) != 0
    val date = Date(timestamp)
    return if (hasClockTime) dayTimeFormat.format(date)
    else dayOnlyFormat.format(date) + " · 补记"
}
