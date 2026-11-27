package com.licount.data

import android.content.Context
import java.io.File

/**
 * 「最近删除」的本地留存。
 *
 * 故意不改 Room 表结构（加 deleted 字段会牵扯数据库迁移、备份格式和所有统计查询），
 * 而是把删掉的记录原样存成应用私有目录里的一个小 JSON 文件：
 * 复用 BackupCodec 的格式，最多 20 条，重启 App 后依然能找回。
 */
object DeletedStore {

    const val MAX_ITEMS = 20
    private const val FILE_NAME = "recent_deleted.json"

    fun load(context: Context): List<Transaction> = runCatching {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) emptyList()
        else BackupCodec.decode(file.readText(Charsets.UTF_8)).items.take(MAX_ITEMS)
    }.getOrDefault(emptyList())

    fun save(context: Context, items: List<Transaction>) {
        runCatching {
            // 与覆盖恢复快照一致：临时文件 + 原子替换，避免写一半被杀
            // 留下半截 JSON，下次启动时「最近删除」被静默清空
            val dir = context.filesDir
            val tmp = File(dir, "$FILE_NAME.tmp")
            tmp.writeText(BackupCodec.encode(items.take(MAX_ITEMS)), Charsets.UTF_8)
            val target = File(dir, FILE_NAME)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        }
    }
}
