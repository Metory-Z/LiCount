package com.licount.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.Transaction
import java.util.Calendar

/**
 * 统计页：本月每日收支趋势图（FlClash 流量图风格）+ 各分类支出占比。
 * 点任一分类卡片会在底部弹层里展开该分类本月的全部支出明细，可直接编辑或删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    transactions: List<Transaction>,
    categoryCounts: Map<String, Int> = emptyMap(),
    onEdit: (Transaction) -> Unit = {},
    onDelete: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    val expenseByCategory = remember(transactions) {
        transactions
            .filter { it.amountFen > 0 }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountFen } }
            .toList()
            .sortedByDescending { it.second }
    }
    val totalExpense = expenseByCategory.sumOf { it.second }

    // 每日支出/收入序列（下标 = 日 - 1）。趋势画到今天为止；
    // 不用 remember 缓存天数，否则 App 常驻跨天/跨月后横轴不会跟着更新。
    val dayCount = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val dailyExpense = remember(transactions, dayCount) {
        val arr = LongArray(dayCount)
        transactions.forEach { tx ->
            val day = Calendar.getInstance()
                .apply { timeInMillis = tx.timestamp }
                .get(Calendar.DAY_OF_MONTH)
            if (day in 1..dayCount && tx.amountFen > 0) arr[day - 1] += tx.amountFen
        }
        arr.toList()
    }
    val dailyIncome = remember(transactions, dayCount) {
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
            Icon(
                AppGlyphs.stats,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
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
        items(expenseByCategory, key = { it.first }) { (key, amount) ->
            val cat = Category.byKey(key)
            val ratio = if (totalExpense == 0L) 0f else amount.toFloat() / totalExpense
            CategoryBar(
                cat = cat,
                amount = amount,
                ratio = ratio,
                onClick = {
                    selectedKey = key
                    editing = null
                }
            )
        }
    }

    // 分类明细：与日历当天明细同一套全宽底部弹层，记录可就地编辑/删除
    selectedKey?.let { key ->
        val cat = Category.byKey(key)
        val list = remember(transactions, key) {
            transactions.filter { it.amountFen > 0 && it.category == key }
                .sortedByDescending { it.timestamp }
        }
        val sum = list.sumOf { it.amountFen }
        val ratioText = if (totalExpense > 0L) {
            " · 占本月支出 ${(sum.toFloat() / totalExpense * 100).toInt()}%"
        } else ""
        ModalBottomSheet(
            onDismissRequest = {
                selectedKey = null
                editing = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        CategoryGlyphs.byKey(key),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${cat.label} · 本月 ${list.size} 笔",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "支出合计 ¥${formatMoney(sum)}$ratioText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                if (list.isEmpty()) {
                    Text(
                        "暂无支出记录",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    list.forEach { tx ->
                        RecordRow(
                            tx = tx,
                            onClick = {
                                editing = if (editing?.id == tx.id) null else tx
                            },
                            onDelete = {
                                if (editing?.id == tx.id) editing = null
                                onDelete(tx)
                            }
                        )
                        if (editing?.id == tx.id) {
                            EditTransactionDialog(
                                tx = tx,
                                categoryCounts = categoryCounts,
                                embedded = true,
                                onDismiss = { editing = null },
                                onConfirm = { updated ->
                                    onEdit(updated)
                                    editing = null
                                }
                            )
                        }
                    }
                }
            }
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

/**
 * Y 轴刻度：以 P90 分位数作为「正常日」的尺度。
 * 原来直接取全局最大值，只要某天有一笔大额收支，其余所有天的曲线就被压成贴着基线的一条直线。
 * 现在离群的大额不参与决定刻度，只在图里截断并标记，正常波动依然占满图表高度。
 */
private data class ChartScale(val ceiling: Long, val maxValue: Long)

private fun computeChartScale(expense: List<Long>, income: List<Long>): ChartScale {
    val positive = (expense + income).filter { it > 0 }.sorted()
    if (positive.isEmpty()) return ChartScale(1L, 0L)
    val max = positive.last()
    // 有效样本太少时 P90 不可靠，退回全局最大值并留 5% 顶部余量
    if (positive.size < 5) {
        return ChartScale((max * 105 / 100).coerceAtLeast(1L), max)
    }
    val p90 = positive[((positive.size - 1) * 0.9f).toInt()]
    val normal = (p90 * 5 / 4).coerceAtLeast(1L)
    // 只有最大值明显超出正常尺度（1.5 倍）才削顶，避免正常波动被误判
    return if (max > normal * 3 / 2) {
        ChartScale(normal, max)
    } else {
        ChartScale((max * 105 / 100).coerceAtLeast(1L), max)
    }
}

/**
 * 把逐日数据点连成平滑曲线：单调三次 Hermite 插值（Fritsch–Carlson 切线）。
 *
 * 普通 Catmull-Rom 会在尖峰前后过冲，配合「削顶」时可能把线拱出图表上边界，
 * 甚至画出数据里并不存在的假峰。单调插值保证每一段都严格落在相邻两点的高度
 * 之间，峰谷处切线压成 0，只做形状柔化、不改动任何数据点的位置。
 */
private fun smoothCurve(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    val n = points.size
    if (n < 2) return path

    // x 必须严格递增才能插值；异常数据退回普通折线，避免除零
    for (i in 1 until n) {
        if (points[i].x <= points[i - 1].x) {
            for (j in 1 until n) path.lineTo(points[j].x, points[j].y)
            return path
        }
    }

    val delta = FloatArray(n - 1) { i ->
        (points[i + 1].y - points[i].y) / (points[i + 1].x - points[i].x)
    }
    val tangent = FloatArray(n)
    tangent[0] = delta[0]
    tangent[n - 1] = delta[n - 2]
    for (i in 1 until n - 1) {
        val d0 = delta[i - 1]
        val d1 = delta[i]
        tangent[i] = if (d0 * d1 <= 0f) {
            0f
        } else {
            val h0 = points[i].x - points[i - 1].x
            val h1 = points[i + 1].x - points[i].x
            val w1 = 2f * h1 + h0
            val w2 = h1 + 2f * h0
            (w1 + w2) / (w1 / d0 + w2 / d1)
        }
    }

    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val third = (p1.x - p0.x) / 3f
        path.cubicTo(
            p0.x + third, p0.y + tangent[i] * third,
            p1.x - third, p1.y - tangent[i + 1] * third,
            p1.x, p1.y
        )
    }
    return path
}

/** 每日收支双曲线趋势图（平滑曲线 + 半透明填充 + 日期刻度，仿 FlClash 上下行流量图）。 */
@Composable
private fun TrendChart(
    expense: List<Long>,
    income: List<Long>,
    expenseColor: Color,
    incomeColor: Color,
    gridColor: Color
) {
    val scale = remember(expense, income) { computeChartScale(expense, income) }
    val chartHeight = 130.dp
    val dayCount = expense.size
    // 具体哪几天超出刻度，脚注里要写清楚，否则用户不知道该去日历翻哪天
    val outliers = remember(expense, income, scale) {
        val list = mutableListOf<Pair<Int, Long>>()
        expense.forEachIndexed { i, v -> if (v > scale.ceiling) list += (i + 1) to v }
        income.forEachIndexed { i, v -> if (v > scale.ceiling) list += (i + 1) to v }
        list
    }
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
            // 顶部留出放「削顶」三角标记的空间
            val topPad = 12f
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
                // 超出刻度的数值钳到顶端，其余日子仍按正常刻度分布
                val shown = value.coerceAtMost(scale.ceiling)
                val y = h - 4f - (shown.toFloat() / scale.ceiling) * usable
                return Offset(x, y)
            }

            fun drawSeries(values: List<Long>, color: Color) {
                if (values.isEmpty()) return
                // 曲线仍严格经过每个数据点，圆点 / 削顶三角画在原始点位上
                val points = values.mapIndexed { i, v -> point(i, v) }
                val path = smoothCurve(points)
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
                    if (v > 0 && v <= scale.ceiling) {
                        drawCircle(color, radius = 2.dp.toPx(), center = point(i, v))
                    }
                }
                // 超出刻度的日子在顶点画小三角，明确表示这里被「削顶」了
                values.forEachIndexed { i, v ->
                    if (v > scale.ceiling) {
                        val p = point(i, v)
                        val r = 4.dp.toPx()
                        val triangle = Path().apply {
                            moveTo(p.x, p.y - r)
                            lineTo(p.x - r * 0.85f, p.y + r * 0.6f)
                            lineTo(p.x + r * 0.85f, p.y + r * 0.6f)
                            close()
                        }
                        drawPath(triangle, color)
                    }
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

        // 脚注放在 X 轴日期标签「下面」：夹在图和日期之间会让人误读成某个刻度的内容。
        // 同时写出具体日期，和上一轮「趋势图看不出是哪天」的诉求对齐。
        if (outliers.isNotEmpty()) {
            val days = outliers.map { "${it.first}日" }.distinct()
            val daysText = days.take(3).joinToString("、") +
                if (days.size > 3) " 等 ${days.size} 天" else ""
            Text(
                "▲ $daysText 超出图表上限，最高 ¥${formatMoney(scale.maxValue)}；超出部分已截断",
                fontSize = 10.sp,
                color = labelColor,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun CategoryBar(
    cat: Category,
    amount: Long,
    ratio: Float,
    onClick: () -> Unit
) {
    DashboardCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        radius = BlockCorner
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    CategoryGlyphs.byKey(cat.key),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
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
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "查看明细",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(14.dp)
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
