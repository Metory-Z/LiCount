package com.licount.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.Transaction
import java.util.Calendar

/** 统计页：本月每日收支趋势图（FlClash 流量图风格）+ 各分类支出占比。 */
@Composable
fun StatsScreen(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp
) {
    val expenseByCategory = remember(transactions) {
        transactions
            .filter { it.amountFen > 0 }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountFen } }
            .toList()
            .sortedByDescending { it.second }
    }

    // 每日支出/收入序列（下标 = 日 - 1）
    val dayCount = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }
    val dailyExpense = remember(transactions) {
        val arr = LongArray(dayCount)
        transactions.forEach { tx ->
            val day = Calendar.getInstance()
                .apply { timeInMillis = tx.timestamp }
                .get(Calendar.DAY_OF_MONTH)
            if (day in 1..dayCount && tx.amountFen > 0) arr[day - 1] += tx.amountFen
        }
        arr.toList()
    }
    val dailyIncome = remember(transactions) {
        val arr = LongArray(dayCount)
        transactions.forEach { tx ->
            val day = Calendar.getInstance()
                .apply { timeInMillis = tx.timestamp }
                .get(Calendar.DAY_OF_MONTH)
            if (day in 1..dayCount && tx.amountFen < 0) arr[day - 1] += -tx.amountFen
        }
        arr.toList()
    }

    if (transactions.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📊", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "暂无数据",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp,
            bottom = bottomInset + 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 每日收支趋势图
        item {
            DashboardCard(
                contentPadding = DashboardCardPadding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "本月趋势",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Legend("支出", MaterialTheme.colorScheme.error)
                            Legend("收入", MaterialTheme.colorScheme.primary)
                        }
                    }
                    TrendChart(
                        expense = dailyExpense,
                        income = dailyIncome,
                        expenseColor = MaterialTheme.colorScheme.error,
                        incomeColor = MaterialTheme.colorScheme.primary,
                        gridColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
            }
        }
        items(expenseByCategory) { (key, amount) ->
            val cat = Category.byKey(key)
            val totalExpense = expenseByCategory.sumOf { it.second }
            val ratio = if (totalExpense == 0L) 0f else amount.toFloat() / totalExpense
            CategoryBar(cat, amount, ratio)
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, SquircleCapsule)
        )
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 每日收支双线趋势图（线 + 半透明填充 + 日期刻度，仿 FlClash 上下行流量图）。 */
@Composable
private fun TrendChart(
    expense: List<Long>,
    income: List<Long>,
    expenseColor: Color,
    incomeColor: Color,
    gridColor: Color
) {
    val maxValue = remember(expense, income) {
        (expense.maxOrNull() ?: 0L).coerceAtLeast(income.maxOrNull() ?: 0L).coerceAtLeast(1L)
    }
    val chartHeight = 130.dp
    val dayCount = expense.size
    // 最多 5 个刻度，等距分布，首尾正好是 1 日和本月最后一天
    val tickDays = remember(dayCount) {
        if (dayCount <= 1) listOf(1)
        else (0..4).map { 1 + it * (dayCount - 1) / 4 }.distinct()
    }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val w = size.width
            val h = size.height
            val topPad = 6f
            val usable = h - topPad - 4f
            val stepX = if (dayCount <= 1) 0f else w / (dayCount - 1)

            // 日期刻度竖线，与下方标签一一对应
            tickDays.forEach { day ->
                val x = if (dayCount <= 1) w / 2f else (day - 1) * stepX
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h - 4f),
                    strokeWidth = 1f
                )
            }

            // 基线
            drawLine(
                color = gridColor,
                start = Offset(0f, h - 4f),
                end = Offset(w, h - 4f),
                strokeWidth = 1.5f
            )

            fun point(index: Int, value: Long): Offset {
                val x = if (dayCount <= 1) w / 2f else index * stepX
                val y = h - 4f - (value.toFloat() / maxValue) * usable
                return Offset(x, y)
            }

            fun drawSeries(values: List<Long>, color: Color) {
                if (values.isEmpty()) return
                val path = Path()
                values.forEachIndexed { i, v ->
                    val p = point(i, v)
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                // 半透明面积填充
                val fill = Path().apply {
                    addPath(path)
                    lineTo(w, h - 4f)
                    lineTo(0f, h - 4f)
                    close()
                }
                drawPath(fill, color.copy(alpha = 0.12f))
                drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                // 有记录的日期打一个小圆点，方便把线对应到具体某天
                values.forEachIndexed { i, v ->
                    if (v > 0) drawCircle(color, radius = 2.dp.toPx(), center = point(i, v))
                }
            }

            drawSeries(expense, expenseColor)
            drawSeries(income, incomeColor)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tickDays.forEach { day ->
                Text("${day}日", fontSize = 10.sp, color = labelColor)
            }
        }
    }
}

@Composable
private fun CategoryBar(cat: Category, amount: Long, ratio: Float) {
    DashboardCard(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        radius = BlockCorner
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cat.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    cat.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatMoney(amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${(ratio * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 横向条形
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        SquircleCapsule
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            SquircleCapsule
                        )
                )
            }
        }
    }
}
