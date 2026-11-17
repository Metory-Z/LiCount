package com.qingzhang.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingzhang.data.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 首页：上半部分是本月收支仪表盘 + 连续打卡，下半部分是极简快记。
 * 打开即见大金额、点分类、按保存，3 秒记完一笔。
 */
@Composable
fun HomeScreen(
    monthExpense: Long,
    monthIncome: Long,
    monthBalance: Long,
    streak: Int,
    onSave: (amountFen: Long, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.presets[0].key) }

    val amountFen = remember(amountText) {
        // 输入的是「元」，转成分；最多两位小数
        val yuan = amountText.toDoubleOrNull() ?: 0.0
        (yuan * 100).toLong()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 仪表盘卡片
        DashboardCard(contentPadding = DashboardCardPadding) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "本月结余",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatMoney(monthBalance),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (monthBalance >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatItem("支出", monthExpense, MaterialTheme.colorScheme.error)
                    StatItem("收入", monthIncome, MaterialTheme.colorScheme.primary)
                    StatItem("连续打卡", streak.toLong(), MaterialTheme.colorScheme.tertiary, suffix = "天")
                }
            }
        }

        // 记账卡片
        DashboardCard(contentPadding = DashboardCardPadding) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "记一笔",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
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
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "¥",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (amountText.isEmpty()) "0" else amountText,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                // 分类快捷选择（一排横向滚动）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Category.presets.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            selected = selectedCategory == cat.key,
                            onClick = { selectedCategory = cat.key }
                        )
                    }
                }

                // 数字键盘
                NumPad(
                    amountText = amountText,
                    onInput = { key -> amountText = handleInput(amountText, key) },
                    onSave = {
                        if (amountFen > 0) {
                            onSave(amountFen, selectedCategory)
                            amountText = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: Long,
    color: androidx.compose.ui.graphics.Color,
    suffix: String = ""
) {
    Column {
        Text(
            "$label $suffix",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            formatMoney(value),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
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

/** 数字键盘：1-9、0、小数点、退格、保存。 */
@Composable
private fun NumPad(
    amountText: String,
    onInput: (String) -> Unit,
    onSave: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    KeyButton(key, Modifier.weight(1f)) { onInput(key) }
                }
            }
        }
        // 保存按钮
        PressSurface(
            onClick = onSave,
            shape = SquircleShape(BlockCorner),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "保存",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
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
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    key,
                    fontSize = 22.sp,
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
