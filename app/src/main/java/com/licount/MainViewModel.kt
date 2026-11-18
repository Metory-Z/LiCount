package com.licount

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.licount.data.AppDatabase
import com.licount.data.TimeUtils
import com.licount.data.Transaction
import com.licount.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 首页仪表盘聚合数据。 */
data class DashboardData(
    val monthExpense: Long = 0,
    val monthIncome: Long = 0,
    /** 今天是否已经有记账记录（未记则在首页提醒）。 */
    val todayRecorded: Boolean = false,
    val recent: List<Transaction> = emptyList()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: TransactionDao = AppDatabase.get(app).transactionDao()

    private val monthRange: Pair<Long, Long> = TimeUtils.currentMonthRange()
    private val monthStart: Long = monthRange.first
    private val monthEnd: Long = monthRange.second

    val dashboard: StateFlow<DashboardData> = combine(
        dao.observeExpenseSum(monthStart, monthEnd),
        dao.observeIncomeSum(monthStart, monthEnd),
        dao.observeAll()
    ) { expense, income, all ->
        val today = TimeUtils.dayStart(System.currentTimeMillis())
        DashboardData(
            monthExpense = expense,
            monthIncome = income,
            todayRecorded = all.any { TimeUtils.dayStart(it.timestamp) == today },
            recent = all
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardData()
    )

    /** 本月每笔流水（用于统计页分类聚合与趋势图）。 */
    val monthTransactions = dao.observeBetween(monthStart, monthEnd)

    /** 记一笔。amountFen 正数=支出，负数=收入。 */
    fun addTransaction(amountFen: Long, category: String, note: String = "") {
        if (amountFen == 0L) return
        viewModelScope.launch {
            dao.insert(
                Transaction(
                    amountFen = amountFen,
                    category = category,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /** 修改一笔记录。 */
    fun updateTransaction(tx: Transaction) {
        if (tx.amountFen == 0L) return
        viewModelScope.launch { dao.update(tx) }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}
