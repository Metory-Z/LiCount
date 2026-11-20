package com.licount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Transaction
import java.util.Calendar
import java.util.Locale

/**
 * 日历页：月历网格，每格显示当天支出（无支出但有收入时显示收入）。
 * 可切换月份；点击某天弹出当天明细，明细沿用流水页的记录卡片与编辑/删除能力。
 */
@Composable
fun CalendarScreen(
    transactions: List<Transaction>,
    categoryCounts: Map<String, Int>,
    onDelete: (Long) -> Unit,
    onEdit: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    val now = remember { Calendar.getInstance() }
    var year by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(now.get(Calendar.MONTH)) } // 0-based
    var selectedDay by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    // 当前所选月份的全部记录，按「日」分组
    val byDay = remember(transactions, year, month) {
        val cal = Calendar.getInstance()
        transactions.filter { tx ->
            cal.timeInMillis = tx.timestamp
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }.groupBy { tx ->
            cal.timeInMillis = tx.timestamp
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }

    val firstDay = remember(year, month) {
        Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = firstDay.get(Calendar.DAY_OF_WEEK) - 1 // 周日为第 0 列
    val weekCount = ((leadingBlanks + daysInMonth + 6) / 7).coerceAtMost(6)
    val isCurrentMonth = year == now.get(Calendar.YEAR) && month == now.get(Calendar.MONTH)
    val today = now.get(Calendar.DAY_OF_MONTH)

    val monthExpense = byDay.values.flatten().filter { it.amountFen > 0 }.sumOf { it.amountFen }
    val monthIncome = byDay.values.flatten().filter { it.amountFen < 0 }.sumOf { -it.amountFen }

    fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month + delta)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        year = cal.get(Calendar.YEAR)
        month = cal.get(Calendar.MONTH)
        selectedDay = 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = bottomInset + 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 月份切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { shiftMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            Text(
                "${year}年${month + 1}月",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { shiftMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
            }
        }

        // 星期标题
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 月历网格
        for (week in 0 until weekCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dow in 0..6) {
                    val day = week * 7 + dow - leadingBlanks + 1
                    if (day !in 1..daysInMonth) {
                        Spacer(Modifier.weight(1f).height(52.dp))
                    } else {
                        DayCell(
                            day = day,
                            txs = byDay[day].orEmpty(),
                            isToday = isCurrentMonth && day == today,
                            isSelected = selectedDay == day,
                            onClick = { selectedDay = day }
                        )
                    }
                }
            }
        }

        // 当月合计
        DashboardCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
            radius = BlockCorner
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "当月支出",
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
                        "当月收入",
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
    }

    // 某天明细
    if (selectedDay > 0) {
        val dayTxs = byDay[selectedDay].orEmpty()
        val dayExpense = dayTxs.filter { it.amountFen > 0 }.sumOf { it.amountFen }
        val dayIncome = dayTxs.filter { it.amountFen < 0 }.sumOf { -it.amountFen }
        AlertDialog(
            onDismissRequest = { selectedDay = 0 },
            title = {
                Text(
                    "${month + 1}月${selectedDay}日 · ${dayTxs.size} 笔",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "支出 ${formatMoney(dayExpense)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "收入 ${formatMoney(dayIncome)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (dayTxs.isEmpty()) {
                        Text(
                            "这天没有记录",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        dayTxs.forEach { tx ->
                            RecordRow(
                                tx = tx,
                                onClick = {
                                    selectedDay = 0
                                    editing = tx
                                },
                                onDelete = { onDelete(tx.id) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = 0 }) { Text("关闭") }
            }
        )
    }

    editing?.let { tx ->
        EditTransactionDialog(
            tx = tx,
            categoryCounts = categoryCounts,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                onEdit(updated)
                editing = null
            }
        )
    }
}

/** 月历里的单个日期格。 */
@Composable
private fun RowScope.DayCell(
    day: Int,
    txs: List<Transaction>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val expense = txs.filter { it.amountFen > 0 }.sumOf { it.amountFen }
    val income = txs.filter { it.amountFen < 0 }.sumOf { -it.amountFen }
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        txs.isNotEmpty() -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(SquircleShape(ControlCorner))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    SquircleCapsule
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$day",
                fontSize = 11.sp,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isToday) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        when {
            expense > 0 -> Text(
                "-${shortMoney(expense)}",
                fontSize = 9.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.error
            )
            income > 0 -> Text(
                "+${shortMoney(income)}",
                fontSize = 9.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** 日历格子里空间有限，大金额用万 / k 缩写。 */
private fun shortMoney(fen: Long): String {
    val yuan = fen / 100.0
    return when {
        yuan >= 10000 -> String.format(Locale.US, "%.1f万", yuan / 10000)
        yuan >= 1000 -> String.format(Locale.US, "%.1fk", yuan / 1000)
        yuan % 1.0 == 0.0 -> String.format(Locale.US, "%.0f", yuan)
        else -> String.format(Locale.US, "%.1f", yuan)
    }
}
