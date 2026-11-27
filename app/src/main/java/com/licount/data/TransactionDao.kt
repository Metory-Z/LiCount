package com.licount.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(tx: Transaction): Long

    @Update
    suspend fun update(tx: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 所有列表查询都补 id DESC 作次级排序：补记落在当天零点，同一天多笔的
    // timestamp 完全相同，SQLite 不保证返回顺序，流水/明细里的同日记录会跳位。
    /** 一次性取全部记录（备份导出用，不受月份限制）。 */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllOnce(): List<Transaction>

    /** 批量插入，主键冲突时跳过；返回每项的 rowId，冲突项为 -1。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(items: List<Transaction>): List<Long>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    /** 某月起止范围内的记录。startInclusive 含，endExclusive 不含。 */
    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC, id DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<Transaction>>

    /** 本月总支出（分）。 */
    @Query("SELECT COALESCE(SUM(amountFen), 0) FROM transactions WHERE amountFen > 0 AND timestamp >= :start AND timestamp < :end")
    fun observeExpenseSum(start: Long, end: Long): Flow<Long>

    /** 本月总收入（分）。 */
    @Query("SELECT COALESCE(SUM(-amountFen), 0) FROM transactions WHERE amountFen < 0 AND timestamp >= :start AND timestamp < :end")
    fun observeIncomeSum(start: Long, end: Long): Flow<Long>
}
