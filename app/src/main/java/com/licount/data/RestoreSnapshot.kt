package com.licount.data

import android.content.Context
import java.io.File

/**
 * 「覆盖恢复」前给现有记录留一份快照，支持一键撤销，避免误点后永久丢数据。
 * 与「最近删除」一样复用备份 JSON 格式，不改 Room 表结构、不引入数据库迁移。
 */
object RestoreSnapshot {

    private const val FILE_NAME = "pre_restore_snapshot.json"

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun save(context: Context, items: List<Transaction>) {
        runCatching {
            // 先写临时文件再替换，避免写到一半被杀留下半截 JSON
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(BackupCodec.encode(items), Charsets.UTF_8)
            val target = file(context)
            if (!tmp.renameTo(target)) tmp.copyTo(target, overwrite = true)
        }
    }

    fun load(context: Context): List<Transaction> = runCatching {
        val f = file(context)
        if (!f.exists()) emptyList()
        else BackupCodec.decode(f.readText(Charsets.UTF_8)).items
    }.getOrDefault(emptyList())

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }
}
