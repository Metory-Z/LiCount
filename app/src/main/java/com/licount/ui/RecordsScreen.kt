package com.licount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.TimeUtils
import com.licount.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 流水列表页：按时间倒序列出全部记录，同一天的记录归为一组，
 * 组前插入椭圆形日期胶囊（如 11.19）；点击记录可修改，右侧按钮删除。
 */
@Composable
fun RecordsScreen(
    transactions: List<Transaction>,
    categoryCounts: Map<String, Int>,
    onDelete: (Long) -> Unit,
    onEdit: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    var editing by remember { mutableStateOf<Transaction?>(null) }
    val dateFormat = remember { SimpleDateFormat("MM.dd", Locale.US) }
    // groupBy 使用 LinkedHashMap，保留倒序输入的日期顺序
    val groups = remember(transactions) {
        transactions.groupBy { TimeUtils.dayStart(it.timestamp) }
    }

    if (transactions.isEmpty()) {
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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp,
                bottom = bottomInset + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            groups.forEach { (dayStart, dayTransactions) ->
                item(key = "day-$dayStart") {
                    DatePill(dateFormat.format(Date(dayStart)))
                }
                items(dayTransactions, key = { it.id }) { tx ->
                    RecordRow(
                        tx = tx,
                        onClick = { editing = tx },
                        onDelete = { onDelete(tx.id) }
                    )
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
}

/**
 * 日期分隔胶囊，居中显示，例如「11.19」。
 * 用主题主色实心填充：原先的 surfaceContainerHigh 与页面背景亮度只差 0.015，
 * 在浅色主题下几乎完全融进背景，换成主色后分组界限才真正立得住。
 */
@Composable
private fun DatePill(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, SquircleCapsule)
                .padding(horizontal = 20.dp, vertical = 7.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
