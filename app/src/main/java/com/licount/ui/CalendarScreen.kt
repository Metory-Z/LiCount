package com.licount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Transaction
import java.util.Calendar
import java.util.Locale

/**
 * 日历页：整块卡片内的月历网格，每格显示当天支出（无支出但有收入时显示收入）。
 * 可切换月份；点击某天用全宽底部弹层展示当天明细，明细沿用流水页的记录卡片与编辑/删除能力。
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = bottomInset + 12.dp)
    ) {
        // 月份头、星期、网格、当月合计装进同一张卡，元素不再散落在近背景色上
        // 左右留白收紧到 16 / 10dp，把宽度尽量让给 7 列格子，避免金额被裁
        DashboardCard(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
            radius = BlockCorner
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 标题居中，箭头紧挨标题，而不是被 SpaceBetween 顶到屏幕两端
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { shiftMonth(-1) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上个月",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        "${year}年${month + 1}月",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    IconButton(onClick = { shiftMonth(1) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下个月",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 星期标题：周末用主色淡字区分
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { i, label ->
                        val weekend = i == 0 || i == 6
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = if (weekend) FontWeight.Medium else FontWeight.Normal,
                            color = if (weekend) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // 网格：行距收紧到 2dp，格子高度一致，日期统一在同一水平线上
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (week in 0 until weekCount) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dow in 0..6) {
                                val day = week * 7 + dow - leadingBlanks + 1
                                if (day !in 1..daysInMonth) {
                                    Spacer(Modifier.weight(1f).height(54.dp))
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
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 当月合计：两列各占一半，金额按长度自动降字号，长数字不会被拦腰裁掉
                val expenseText = formatMoney(monthExpense)
                val incomeText = formatMoney(monthIncome)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "当月支出",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            expenseText,
                            fontSize = when {
                                expenseText.length >= 11 -> 12.sp
                                expenseText.length >= 9 -> 14.sp
                                else -> 16.sp
                            },
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "当月收入",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            incomeText,
                            fontSize = when {
                                incomeText.length >= 11 -> 12.sp
                                incomeText.length >= 9 -> 14.sp
                                else -> 16.sp
                            },
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // 某天明细：全宽底部弹层，记录卡片不再被塞进窄对话框里挤压变形
    if (selectedDay > 0) {
        val dayTxs = byDay[selectedDay].orEmpty()
        val dayExpense = dayTxs.filter { it.amountFen > 0 }.sumOf { it.amountFen }
        val dayIncome = dayTxs.filter { it.amountFen < 0 }.sumOf { -it.amountFen }
        ModalBottomSheet(
            onDismissRequest = { selectedDay = 0 },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${month + 1}月${selectedDay}日 · ${dayTxs.size} 笔",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "支出 ${formatMoney(dayExpense)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "收入 ${formatMoney(dayIncome)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (dayTxs.isEmpty()) {
                    Text(
                        "这天没有记录",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
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
        }
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

/** 月历里的单个日期格。内容高度固定，保证有/无金额的格子日期不会上下跳动。 */
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
    val shape = SquircleShape(ControlCorner)
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        txs.isNotEmpty() -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .padding(horizontal = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(bg)
                .then(
                    if (isSelected) {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                        SquircleCapsule
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$day",
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Spacer(Modifier.height(2.dp))
            // 无记录时也保留这一行的高度，日期才能对齐；金额行占满格宽并自动缩放
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    expense > 0 -> DayAmount(
                        "-${shortMoney(expense)}",
                        MaterialTheme.colorScheme.error
                    )
                    income > 0 -> DayAmount(
                        "+${shortMoney(income)}",
                        MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 日历格子里空间有限，用整数 + 万 / k 缩写保证完整显示；
 * 精确金额在点开当天明细里看。最长形如「-99万 / -99k / -12.5」。
 */
private fun shortMoney(fen: Long): String {
    val yuan = fen / 100.0
    return when {
        yuan >= 10000 -> String.format(Locale.US, "%.0f万", yuan / 10000)
        yuan >= 1000 -> String.format(Locale.US, "%.0fk", yuan / 1000)
        yuan >= 100 -> String.format(Locale.US, "%.0f", yuan)
        yuan % 1.0 == 0.0 -> String.format(Locale.US, "%.0f", yuan)
        else -> String.format(Locale.US, "%.1f", yuan)
    }
}

/**
 * 格子金额：先按 9sp 排版，一旦检测到横向溢出就自动降字号（最低 7sp），
 * 保证数字完整显示，而不是被格子裁掉半截。
 */
@Composable
private fun DayAmount(text: String, color: Color) {
    var fontSize by remember(text) { mutableStateOf(9.sp) }
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Clip,
        modifier = Modifier.fillMaxWidth(),
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > 7f) {
                fontSize = (fontSize.value - 0.5f).sp
            }
        }
    )
}
