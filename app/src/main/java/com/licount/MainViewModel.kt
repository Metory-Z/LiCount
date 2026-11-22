package com.licount

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.licount.data.AppDatabase
import com.licount.data.BackupCodec
import com.licount.data.DeletedStore
import com.licount.data.TimeUtils
import com.licount.data.Transaction
import com.licount.data.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 首页仪表盘聚合数据。 */
data class DashboardData(
    val monthExpense: Long = 0,
    val monthIncome: Long = 0,
    /** 今天是否已经有记账记录（未记则在首页提醒）。 */
    val todayRecorded: Boolean = false,
    /** 全部记录（按时间倒序），流水页与备份计数用。 */
    val recent: List<Transaction> = emptyList()
)

/** 恢复方式：覆盖（换机迁移）/ 合并（补齐缺失记录，跳过重复）。 */
enum class RestoreMode { REPLACE, MERGE }

/** 已选好备份文件、等待用户确认恢复方式。 */
data class PendingRestore(
    val uri: Uri,
    val count: Int,
    val exportedAt: Long
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao: TransactionDao = db.transactionDao()

    private val monthRange: Pair<Long, Long> = TimeUtils.currentMonthRange()
    private val monthStart: Long = monthRange.first
    private val monthEnd: Long = monthRange.second

    /** 一次性提示（Toast）。 */
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    /** 最近删除的记录（本地文件持久化，重启后仍在）。 */
    private val _deleted = MutableStateFlow<List<Transaction>>(emptyList())
    val deleted: StateFlow<List<Transaction>> = _deleted.asStateFlow()

    /** 每删一笔发一次事件，供界面弹「撤销」提示。 */
    private val _deletedEvent = MutableSharedFlow<Transaction>(extraBufferCapacity = 1)
    val deletedEvent: SharedFlow<Transaction> = _deletedEvent.asSharedFlow()

    /** 防止连点「恢复」把同一笔插两次。 */
    private val restoringIds = HashSet<Long>()

    init {
        _deleted.value = DeletedStore.load(app)
    }

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

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

    /** 记一笔（默认记在今天）。amountFen 正数=支出，负数=收入。 */
    fun addTransaction(amountFen: Long, category: String, note: String = "") =
        addTransactionAt(amountFen, category, note, System.currentTimeMillis())

    /** 补记：可指定时间戳，用于日历页给过去的某天补一笔。 */
    fun addTransactionAt(amountFen: Long, category: String, note: String, timestamp: Long) {
        if (amountFen == 0L) return
        viewModelScope.launch {
            dao.insert(
                Transaction(
                    amountFen = amountFen,
                    category = category,
                    note = note,
                    timestamp = timestamp
                )
            )
        }
    }

    /** 修改一笔记录（含日期）。 */
    fun updateTransaction(tx: Transaction) {
        if (tx.amountFen == 0L) return
        viewModelScope.launch { dao.update(tx) }
    }

    /** 删除一笔，并留一份到「最近删除」+ 发出可撤销事件。 */
    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            dao.deleteById(tx.id)
            val next = (listOf(tx) + _deleted.value.filter { it.id != tx.id })
                .take(DeletedStore.MAX_ITEMS)
            _deleted.value = next
            withContext(Dispatchers.IO) { DeletedStore.save(app, next) }
            _deletedEvent.tryEmit(tx)
        }
    }

    /** 撤销刚刚那次删除。 */
    fun undoDelete(tx: Transaction) = restoreDeleted(tx)

    /** 从「最近删除」恢复一笔：保留原金额、分类、备注与时间。 */
    fun restoreDeleted(tx: Transaction) {
        if (!restoringIds.add(tx.id)) return
        viewModelScope.launch {
            // id 交给 Room 重新生成，避免与库中已有主键冲突；业务数据完全一致
            dao.insert(tx.copy(id = 0L))
            val next = _deleted.value.filterNot { it.id == tx.id }
            _deleted.value = next
            withContext(Dispatchers.IO) { DeletedStore.save(app, next) }
            _message.tryEmit("已恢复这笔记录")
            restoringIds.remove(tx.id)
        }
    }

    fun clearDeleted() {
        _deleted.value = emptyList()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { DeletedStore.save(app, emptyList()) }
        }
    }

    // ---- 备份 / 恢复 ----

    /** 把全部记录导出到用户选择的文件。 */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val items = withContext(Dispatchers.IO) { dao.getAllOnce() }
                val json = withContext(Dispatchers.IO) { BackupCodec.encode(items) }
                withContext(Dispatchers.IO) { writeText(uri, json) }
                items.size
            }.onSuccess { count ->
                _message.tryEmit("已备份 $count 笔记录")
            }.onFailure { e ->
                _message.tryEmit("备份失败：${e.message ?: "未知错误"}")
            }
        }
    }

    /** 读取备份文件并校验，成功后挂起等待用户选择恢复方式。 */
    fun prepareRestore(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { BackupCodec.decode(readText(uri)) }
            }.onSuccess { backup ->
                if (backup.items.isEmpty()) {
                    _message.tryEmit("备份文件里没有可恢复的记录")
                } else {
                    _pendingRestore.value = PendingRestore(
                        uri = uri,
                        count = backup.items.size,
                        exportedAt = backup.exportedAt
                    )
                }
            }.onFailure { e ->
                _message.tryEmit("读取失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }

    fun confirmRestore(mode: RestoreMode) {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val backup = BackupCodec.decode(readText(pending.uri))
                    db.withTransaction {
                        val toInsert: List<Transaction> = if (mode == RestoreMode.REPLACE) {
                            dao.deleteAll()
                            backup.items
                        } else {
                            val existing = dao.getAllOnce().map { it.businessKey() }.toHashSet()
                            backup.items
                                .filter { it.businessKey() !in existing }
                                .map { it.copy(id = 0L) }
                        }
                        if (toInsert.isEmpty()) 0
                        else dao.insertIgnoreAll(toInsert).count { it != -1L }
                    }
                }
            }.onSuccess { added ->
                _message.tryEmit(
                    if (mode == RestoreMode.REPLACE) "已覆盖恢复 ${pending.count} 笔记录"
                    else "已合并导入 $added 笔新记录（重复项已跳过）"
                )
            }.onFailure { e ->
                _message.tryEmit("恢复失败：${e.message ?: "未知错误"}")
            }
        }
    }

    /** 合并时用于判重的业务键：同一金额、分类、备注、时刻视为同一笔。 */
    private fun Transaction.businessKey(): String =
        "$amountFen|$category|$note|$timestamp"

    private fun readText(uri: Uri): String =
        app.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } ?: throw IllegalStateException("无法读取所选文件")

    private fun writeText(uri: Uri, text: String) {
        app.contentResolver.openOutputStream(uri)?.use {
            it.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("无法写入所选位置")
    }
}
