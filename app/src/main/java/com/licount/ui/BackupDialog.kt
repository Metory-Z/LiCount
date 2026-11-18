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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 顶栏入口：备份与恢复菜单。 */
@Composable
fun BackupMenuDialog(
    totalCount: Int,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    emoji = "⬆️",
                    title = "导出备份",
                    desc = "选择保存位置，生成备份文件",
                    onClick = onExport
                )
                BackupOption(
                    emoji = "⬇️",
                    title = "从文件恢复",
                    desc = "选择之前的备份文件导入",
                    onClick = onImport
                )
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
                    emoji = "🧩",
                    title = "合并导入",
                    desc = "保留现有记录，只补入备份里没有的（重复自动跳过）",
                    onClick = onMerge
                )
                BackupOption(
                    emoji = "♻️",
                    title = "覆盖恢复",
                    desc = "清空当前全部记录，完全还原备份（换机迁移用，不可撤销）",
                    danger = true,
                    onClick = onReplace
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun BackupOption(
    emoji: String,
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
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg)
                Text(desc, fontSize = 11.sp, color = sub)
            }
        }
    }
}
