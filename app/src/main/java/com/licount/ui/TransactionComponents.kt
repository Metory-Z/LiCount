package com.licount.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.Transaction
import java.util.Locale

/**
 * 流水页与日历详情共用的一行记录卡片：点击可编辑，右侧按钮删除。
 */
@Composable
fun RecordRow(tx: Transaction, onClick: () -> Unit, onDelete: () -> Unit) {
    val cat = Category.byKey(tx.category)
    val isExpense = tx.amountFen > 0
    DashboardCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        radius = BlockCorner
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

/**
 * 修改记录对话框：金额、收支类型、分类、备注。
 * 分类按累计使用次数降序，常用的排在最前。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    tx: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    categoryCounts: Map<String, Int> = emptyMap()
) {
    val initialAmount = remember {
        val abs = kotlin.math.abs(tx.amountFen)
        if (abs % 100 == 0L) (abs / 100).toString()
        else String.format(Locale.US, "%d.%02d", abs / 100, abs % 100)
    }
    var amountText by remember { mutableStateOf(initialAmount) }
    var isIncome by remember { mutableStateOf(tx.amountFen < 0) }
    var category by remember { mutableStateOf(tx.category) }
    var note by remember { mutableStateOf(tx.note) }

    val amountFen = (amountText.toDoubleOrNull() ?: 0.0).let { (it * 100).toLong() }
    val orderedCategories = remember(categoryCounts) {
        Category.presets.sortedByDescending { categoryCounts[it.key] ?: 0 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("修改记录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额（元）") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogToggle(
                        label = "支出",
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        modifier = Modifier.weight(1f)
                    )
                    DialogToggle(
                        label = "收入",
                        selected = isIncome,
                        onClick = { isIncome = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("分类", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    orderedCategories.forEach { cat ->
                        val selected = category == cat.key
                        val bg by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "edit-cat-bg"
                        )
                        val fg by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "edit-cat-fg"
                        )
                        PressSurface(
                            onClick = { category = cat.key },
                            shape = SquircleCapsule,
                            role = Role.RadioButton
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(bg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(cat.emoji, fontSize = 12.sp)
                                Text(cat.label, fontSize = 12.sp, color = fg)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (amountFen > 0) {
                        onConfirm(
                            tx.copy(
                                amountFen = if (isIncome) -amountFen else amountFen,
                                category = category,
                                note = note
                            )
                        )
                    }
                },
                enabled = amountFen > 0
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DialogToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "dlg-bg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "dlg-fg"
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
