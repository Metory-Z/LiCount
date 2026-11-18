package com.licount.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 首页：极简快记。
 * 上半区（本月收支 / 提醒 / 切换 / 金额 / 分类）可滚动并占满剩余空间；
 * 下半区（数字键盘 + 保存按钮）固定钉在屏幕底部、悬浮导航栏上方，
 * 保证任何屏幕高度下键盘和「保存」都无需上滑即可点到。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    monthExpense: Long,
    monthIncome: Long,
    todayRecorded: Boolean,
    onSave: (amountFen: Long, category: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(Category.presets[0].key) }

    val amountFen = remember(amountText) {
        // 输入的是「元」，转成分；最多两位小数
        val yuan = amountText.toDoubleOrNull() ?: 0.0
        (yuan * 100).toLong()
    }
    val canSave = amountFen > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ===== 上半区：可滚动，占满剩余空间 =====
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 概览卡：提醒 + 本月支出/收入 + 收支切换 + 金额
            DashboardCard(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 今日未记账提醒
                    if (!todayRecorded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    SquircleShape(ControlCorner)
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📌", fontSize = 13.sp)
                            Text(
                                "今天还没记账，记一笔吧",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // 本月支出 / 收入
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "本月支出",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatMoney(monthExpense),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "本月收入",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatMoney(monthIncome),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 支出 / 收入 切换 + 清空
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeToggle(
                            label = "支出",
                            selected = !isIncome,
                            onClick = { isIncome = false },
                            modifier = Modifier.weight(1f)
                        )
                        TypeToggle(
                            label = "收入",
                            selected = isIncome,
                            onClick = { isIncome = true },
                            modifier = Modifier.weight(1f)
                        )
                        if (amountText.isNotEmpty()) {
                            TextButton(onClick = { amountText = "" }) {
                                Text("清空", fontSize = 13.sp)
                            }
                        }
                    }

                    // 金额显示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                SquircleShape(BlockCorner)
                            )
                            .padding(vertical = 10.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "¥",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (amountText.isEmpty()) "0" else amountText,
                                fontSize = 38.sp,
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
                }
            }

            // 分类卡（两行流式，不再溢出）
            DashboardCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Category.presets.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            selected = selectedCategory == cat.key,
                            onClick = { selectedCategory = cat.key }
                        )
                    }
                }
            }
        }

        // ===== 下半区：固定键盘 + 保存，永远可见 =====
        DashboardCard(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NumPad(
                canSave = canSave,
                onInput = { key -> amountText = handleInput(amountText, key) },
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

/** 支出/收入 切换按钮。 */
@Composable
private fun TypeToggle(
    label: String,
    selected: Boolean,
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
                .padding(vertical = 8.dp),
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
        role = androidx.compose.ui.semantics.Role.RadioButton
    ) {
        Row(
            modifier = Modifier
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(category.emoji, fontSize = 14.sp)
            Text(
                category.label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = fg
            )
        }
    }
}

/** 数字键盘：1-9、0、小数点、退格、保存。整块固定在底部，不参与滚动。 */
@Composable
private fun NumPad(
    canSave: Boolean,
    onInput: (String) -> Unit,
    onSave: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    KeyButton(key, Modifier.weight(1f)) { onInput(key) }
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
                    .padding(vertical = 12.dp),
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
    onClick: () -> Unit
) {
    val isDelete = key == "⌫"
    PressSurface(
        onClick = onClick,
        shape = SquircleShape(ControlCorner),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
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

/** 金额格式化：分 → 元，千分位。 */
fun formatMoney(fen: Long): String {
    val sign = if (fen < 0) "-" else ""
    val abs = kotlin.math.abs(fen)
    val yuan = abs / 100
    val cents = abs % 100
    val formatted = "%,d".format(yuan)
    return if (cents == 0L) "$sign$formatted" else "$sign$formatted.${"%02d".format(cents)}"
}

/** 时间戳格式化（供流水列表用）。 */
fun formatDate(timestamp: Long): String {
    val fmt = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault())
    return fmt.format(Date(timestamp))
}
