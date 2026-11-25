package com.licount.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 备份文件的编解码。
 *
 * 用 Android 内置的 org.json，不引入第三方库；纯文本 JSON 便于用户自行查看、
 * 长期归档，也不受 Room 数据库版本/机型差异影响。
 */
object BackupCodec {

    private const val FORMAT = "licount-backup"
    private const val VERSION = 1

    data class Backup(
        val exportedAt: Long,
        val items: List<Transaction>
    )

    fun encode(items: List<Transaction>): String {
        val array = JSONArray()
        items.forEach { tx ->
            array.put(
                JSONObject()
                    .put("id", tx.id)
                    .put("amountFen", tx.amountFen)
                    .put("category", tx.category)
                    .put("note", tx.note)
                    .put("timestamp", tx.timestamp)
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("app", "LiCount")
            .put("exportedAt", System.currentTimeMillis())
            .put("count", items.size)
            .put("transactions", array)
            .toString(2)
    }

    /** 解析失败时抛出带中文说明的异常，便于直接提示给用户。 */
    fun decode(text: String): Backup {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("文件不是有效的 JSON", e)
        }
        if (root.optString("format") != FORMAT) {
            throw IllegalArgumentException("这不是轻记的备份文件")
        }
        val version = root.optInt("version", 1)
        if (version < 1 || version > VERSION) {
            throw IllegalArgumentException("备份文件版本 v$version 不受支持")
        }
        val array = root.optJSONArray("transactions")
            ?: throw IllegalArgumentException("备份文件缺少记录数据")
        // 导出时写了 count，用它发现文件被截断或改坏的情况
        val declaredCount = root.optInt("count", -1)
        if (declaredCount >= 0 && declaredCount != array.length()) {
            throw IllegalArgumentException("备份文件记录数不匹配，可能已损坏")
        }
        val items = ArrayList<Transaction>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val amount = obj.optLong("amountFen", 0L)
            val timestamp = obj.optLong("timestamp", 0L)
            if (amount == 0L || timestamp <= 0L) continue
            items.add(
                Transaction(
                    id = obj.optLong("id", 0L),
                    amountFen = amount,
                    category = obj.optString("category", "other").ifEmpty { "other" },
                    note = obj.optString("note", ""),
                    timestamp = timestamp
                )
            )
        }
        return Backup(root.optLong("exportedAt", 0L), items)
    }
}
