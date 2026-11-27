package com.licount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.licount.data.Category
import com.licount.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** 顶栏入口：备份与恢复菜单（含误删找回）。 */
@Composable
fun BackupMenuDialog(
    totalCount: Int,
    deletedCount: Int,
    snapshotCount: Int = 0,
    onUndoReplace: () -> Unit = {},
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenDeleted: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleShape(CardCorner),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("备份与恢复", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "当前共 $totalCount 笔记录。备份会导出全部记录（含历史月份）为一个 .json 文件，可存到网盘或传到新手机。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BackupOption(
                    icon = AppGlyphs.upload,
                    title = "导出备份",
                    desc = "选择保存位置，生成备份文件",
                    onClick = onExport
                )
                BackupOption(
                    icon = AppGlyphs.download,
                    title = "从文件恢复",
                    desc = "选择之前的备份文件导入",
                    onClick = onImport
                )
                BackupOption(
                    icon = AppGlyphs.trash,
                    title = "最近删除" + if (deletedCount > 0) " ($deletedCount)" else "",
                    desc = "找回误删的记录，最多保留最近 20 条",
                    onClick = onOpenDeleted
                )
                if (snapshotCount > 0) {
                    BackupOption(
                        icon = AppGlyphs.replace,
                        title = "撤销上次覆盖 ($snapshotCount)",
                        desc = "找回被覆盖恢复清空的全部原始记录",
                        onClick = onUndoReplace
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

/** 恢复前确认：显示备份信息，并让用户选择覆盖还是合并。 */
@Composable
fun RestoreConfirmDialog(
    count: Int,
    exportedAt: Long,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeText = remember(exportedAt) {
        if (exportedAt > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(exportedAt))
        } else {
            "未知时间"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleShape(CardCorner),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("恢复备份", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "备份时间：$timeText\n包含 $count 笔记录",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("请选择恢复方式", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BackupOption(
                    icon = AppGlyphs.merge,
                    title = "合并导入",
                    desc = "保留现有记录，只补入备份里没有的（重复自动跳过）",
                    onClick = onMerge
                )
                BackupOption(
                    icon = AppGlyphs.replace,
                    title = "覆盖恢复",
                    desc = "清空当前全部记录，完全还原备份（清空前自动留快照，可撤销）",
                    danger = true,
                    onClick = onReplace
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 最近删除：把本地留存的已删记录逐条恢复，或清空。 */
@Composable
fun RecentDeletedDialog(
    items: List<Transaction>,
    onRestore: (Transaction) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleShape(CardCorner),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("最近删除", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "删除后会在这里保留最近 20 条，随时可以恢复；清空之后就无法找回了。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (items.isEmpty()) {
                    Text(
                        "暂无删除记录",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    items.forEach { tx -> DeletedRow(tx, onRestore = { onRestore(tx) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = {
            if (items.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
private fun DeletedRow(tx: Transaction, onRestore: () -> Unit) {
    val cat = Category.byKey(tx.category)
    val isExpense = tx.amountFen > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, SquircleShape(ControlCorner))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            CategoryGlyphs.byKey(cat.key),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(cat.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                formatDate(tx.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            (if (isExpense) "-" else "+") + formatMoney(abs(tx.amountFen)),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        TextButton(onClick = onRestore, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("恢复", fontSize = 12.sp)
        }
    }
}

@Composable
private fun BackupOption(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val bg = if (danger) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = if (danger) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val sub = if (danger) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    PressSurface(
        onClick = onClick,
        shape = SquircleShape(ControlCorner),
        role = Role.Button
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg)
                Text(desc, fontSize = 11.sp, color = sub)
            }
        }
    }
}
