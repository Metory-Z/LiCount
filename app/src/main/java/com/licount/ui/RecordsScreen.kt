package com.licount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.Transaction

/** 流水列表页：按时间倒序列出所有记录，左滑/点击删除。 */
@Composable
fun RecordsScreen(
    transactions: List<Transaction>,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp
) {
    if (transactions.isEmpty()) {
        // 空状态
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📭", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "还没有记录",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "去首页记下第一笔吧",
                fontSize = 13.sp,
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(transactions, key = { it.id }) { tx ->
            RecordRow(tx) { onDelete(tx.id) }
        }
    }
}

@Composable
private fun RecordRow(tx: Transaction, onDelete: () -> Unit) {
    val cat = Category.byKey(tx.category)
    val isExpense = tx.amountFen > 0
    DashboardCard(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        radius = BlockCorner
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        SquircleShape(ControlCorner)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(cat.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    cat.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (tx.note.isNotEmpty()) {
                    Text(
                        tx.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatDate(tx.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 金额
            Text(
                (if (isExpense) "-" else "+") + formatMoney(kotlin.math.abs(tx.amountFen)),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isExpense) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
