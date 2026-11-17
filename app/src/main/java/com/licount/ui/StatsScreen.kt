package com.licount.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.Transaction

/** 统计页：本月各分类支出占比（横向条形）+ 汇总。 */
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
    val totalExpense = expenseByCategory.sumOf { it.second }

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
        item {
            DashboardCard(contentPadding = DashboardCardPadding) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "本月支出",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatMoney(totalExpense),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        items(expenseByCategory) { (key, amount) ->
            val cat = Category.byKey(key)
            val ratio = if (totalExpense == 0L) 0f else amount.toFloat() / totalExpense
            CategoryBar(cat, amount, ratio)
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
