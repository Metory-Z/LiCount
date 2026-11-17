package com.licount.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(tx: Transaction): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    /** 某月起止范围内的记录。startInclusive 含，endExclusive 不含。 */
    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<Transaction>>

    /** 本月总支出（分）。 */
    @Query("SELECT COALESCE(SUM(amountFen), 0) FROM transactions WHERE amountFen > 0 AND timestamp >= :start AND timestamp < :end")
    fun observeExpenseSum(start: Long, end: Long): Flow<Long>

    /** 本月总收入（分）。 */
    @Query("SELECT COALESCE(SUM(-amountFen), 0) FROM transactions WHERE amountFen < 0 AND timestamp >= :start AND timestamp < :end")
    fun observeIncomeSum(start: Long, end: Long): Flow<Long>

    /** 有记录的所有日期（按天，毫秒零点），用于连续打卡计算。 */
    @Query("SELECT DISTINCT timestamp FROM transactions ORDER BY timestamp DESC")
    fun observeDistinctDays(): Flow<List<Long>>
}
