package com.qingzhang

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qingzhang.data.AppDatabase
import com.qingzhang.data.TimeUtils
import com.qingzhang.data.Transaction
import com.qingzhang.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 首页仪表盘聚合数据。 */
data class DashboardData(
    val monthExpense: Long = 0,
    val monthIncome: Long = 0,
    val monthBalance: Long = 0,
    val streak: Int = 0,
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
        dao.observeDistinctDays(),
        dao.observeAll()
    ) { expense, income, days, all ->
        val daySet = days.map { TimeUtils.dayStart(it) }.toSet()
        DashboardData(
            monthExpense = expense,
            monthIncome = income,
            monthBalance = income - expense,
            streak = TimeUtils.streakDays(daySet),
            recent = all
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardData()
    )

    /** 本月每笔流水（用于统计页分类聚合）。 */
    val monthTransactions = dao.observeBetween(monthStart, monthEnd)

    /** 某分类本月支出合计。 */
    fun categoryExpenseFlow(category: String) =
        dao.observeBetween(monthStart, monthEnd).map { list ->
            list.filter { it.category == category && it.amountFen > 0 }
                .sumOf { it.amountFen }
        }

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

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}
