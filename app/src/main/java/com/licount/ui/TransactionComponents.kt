package com.licount.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.MoneyInput
import com.licount.data.TimeUtils
import com.licount.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
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
                Icon(
                    CategoryGlyphs.byKey(cat.key),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatDate(tx.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val amountText =
                (if (isExpense) "-" else "+") + formatMoney(kotlin.math.abs(tx.amountFen))
            Text(
                amountText,
                // 超长金额自动降字号，避免在明细弹层里被分类列挤出屏幕
                fontSize = when {
                    amountText.length >= 11 -> 12.sp
                    amountText.length >= 9 -> 14.sp
                    else -> 17.sp
                },
                maxLines = 1,
                softWrap = false,
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
 * 修改记录：金额、收支类型、分类、备注。
 * 分类按累计使用次数降序，常用的排在最前。
 *
 * 两种呈现方式共用同一份表单与状态：
 * - 流水页：`embedded = false`，标准居中 AlertDialog；
 * - 日历当天明细：`embedded = true`，在底部弹层的列表里就地展开成一张卡片，
 *   当天的其他记录保持原位可见，不再把整个明细弹层收起。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    tx: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    categoryCounts: Map<String, Int> = emptyMap(),
    embedded: Boolean = false,
    isNew: Boolean = false,
    initialTimestamp: Long = tx.timestamp
) {
    // 内联展开时切换编辑对象不会卸载这个 Composable，状态必须按记录 id 重置
    val initialAmount = remember(tx.id, isNew) {
        if (isNew) "" else {
            val abs = kotlin.math.abs(tx.amountFen)
            if (abs % 100 == 0L) (abs / 100).toString()
            else String.format(Locale.US, "%d.%02d", abs / 100, abs % 100)
        }
    }
    var amountText by rememberSaveable(tx.id, isNew) { mutableStateOf(initialAmount) }
    var isIncome by rememberSaveable(tx.id, isNew) { mutableStateOf(tx.amountFen < 0) }
    var category by rememberSaveable(tx.id, isNew) { mutableStateOf(tx.category) }
    var note by rememberSaveable(tx.id, isNew) { mutableStateOf(tx.note) }
    // 日期按「天」调整，保留原时刻；补记时目标日是零点，明细里就不显示假时刻
    var dateMillis by rememberSaveable(tx.id, isNew) {
        mutableStateOf(TimeUtils.dayStart(initialTimestamp))
    }

    val amountFen = MoneyInput.parseYuanToFen(amountText)
    val canSave = amountFen > 0
    val orderedCategories = remember(categoryCounts) {
        Category.presets.sortedByDescending { categoryCounts[it.key] ?: 0 }
    }
    val save = {
        if (canSave) {
            onConfirm(
                tx.copy(
                    amountFen = if (isIncome) -amountFen else amountFen,
                    category = category,
                    note = note,
                    timestamp = TimeUtils.withDay(initialTimestamp, dateMillis)
                )
            )
        }
    }

    // 内联模式下外层弹层已经可竖向滚动，表单不能再套一层 verticalScroll，
    // 否则两层同向滚动会互相抢事件。scrollState 仍无条件创建，保证 remember 位置稳定。
    val scrollState = rememberScrollState()
    val fields: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = if (embedded) Modifier else Modifier.verticalScroll(scrollState)
        ) {
            DateField(dateMillis = dateMillis, onChange = { dateMillis = it })
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = MoneyInput.sanitize(it) },
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
                            Icon(
                                CategoryGlyphs.byKey(cat.key),
                                contentDescription = null,
                                tint = fg,
                                modifier = Modifier.size(14.dp)
                            )
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
    }

    if (embedded) {
        DashboardCard(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            radius = BlockCorner
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isNew) "补记一笔" else "修改这一笔",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onDismiss) { Text("收起", fontSize = 13.sp) }
                }
                fields()
                Button(
                    onClick = save,
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isNew) "保存这一笔" else "保存修改") }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    if (isNew) "补记一笔" else "修改记录",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { fields() },
            confirmButton = {
                TextButton(onClick = save, enabled = canSave) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    }
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

/**
 * 日期微调：左右箭头按天移动，不允许改到未来。
 * 不用 DatePicker 是为了绕开 UTC 与本地时区差一天的坑；补记/改错日期通常只需前后挪几天，
 * 真要跳到很久以前，直接在日历页点那天「补记一笔」更快。
 */
@Composable
private fun DateField(dateMillis: Long, onChange: (Long) -> Unit) {
    // 不缓存：跨过午夜后「今天 / 昨天」标签与右箭头禁用状态仍然正确
    val today = TimeUtils.dayStart(System.currentTimeMillis())
    val label = remember(dateMillis, today) {
        val text = SimpleDateFormat("M月d日 EEE", Locale.CHINA).format(Date(dateMillis))
        when (dateMillis) {
            today -> "$text · 今天"
            TimeUtils.addDays(today, -1) -> "$text · 昨天"
            else -> text
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("日期", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange(TimeUtils.addDays(dateMillis, -1)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "前一天",
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            IconButton(
                onClick = { onChange(TimeUtils.addDays(dateMillis, 1)) },
                enabled = dateMillis < today,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "后一天",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
