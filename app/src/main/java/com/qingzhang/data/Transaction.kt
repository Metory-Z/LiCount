package com.qingzhang.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一笔收支记录。金额单位为「分」，用 Long 避免浮点误差。 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 金额（分），正数=支出，负数=收入。 */
    val amountFen: Long,
    /** 分类 key，见 Category 预置表。 */
    val category: String,
    /** 备注，可空。 */
    val note: String = "",
    /** 记账时间戳（毫秒）。 */
    val timestamp: Long
)
