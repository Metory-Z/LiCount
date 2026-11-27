package com.licount

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.licount.data.AppDatabase
import com.licount.data.BackupCodec
import com.licount.data.DeletedStore
import com.licount.data.RestoreSnapshot
import com.licount.data.TimeUtils
import com.licount.data.Transaction
import com.licount.data.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao: TransactionDao = db.transactionDao()

    /** 当前「今天 / 本月」的时间窗口；跨天跨月时自动更新，不需要重启 App。 */
    private data class ClockWindow(
        val dayStart: Long,
        val monthStart: Long,
        val monthEnd: Long
    )

    private fun clockWindow(now: Long = System.currentTimeMillis()): ClockWindow {
        val (start, end) = TimeUtils.currentMonthRange(now)
        return ClockWindow(TimeUtils.dayStart(now), start, end)
    }

    private val clock = MutableStateFlow(clockWindow())

    private fun refreshClock() {
        val next = clockWindow()
        if (next != clock.value) clock.value = next
    }

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

    /** 上一次「覆盖恢复」被清空的记录数，>0 时界面提供撤销入口。 */
    private val _snapshotCount = MutableStateFlow(0)
    val snapshotCount: StateFlow<Int> = _snapshotCount.asStateFlow()

    init {
        _deleted.value = DeletedStore.load(app)
        viewModelScope.launch {
            _snapshotCount.value = withContext(Dispatchers.IO) { RestoreSnapshot.load(app).size }
        }
        // 每分钟检查一次；只有跨天或跨月才真正 emit，不会造成无谓重组
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                refreshClock()
            }
        }
    }

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

    val dashboard: StateFlow<DashboardData> = clock.flatMapLatest { window ->
        combine(
            dao.observeExpenseSum(window.monthStart, window.monthEnd),
            dao.observeIncomeSum(window.monthStart, window.monthEnd),
            dao.observeAll()
        ) { expense, income, all ->
            DashboardData(
                monthExpense = expense,
                monthIncome = income,
                todayRecorded = all.any { TimeUtils.dayStart(it.timestamp) == window.dayStart },
                recent = all
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardData()
    )

    /** 本月每笔流水（用于统计页分类聚合与趋势图），随月份自动切换区间。 */
    val monthTransactions: Flow<List<Transaction>> =
        clock.flatMapLatest { dao.observeBetween(it.monthStart, it.monthEnd) }

    /** 记一笔（默认记在今天）。amountFen 正数=支出，负数=收入。 */
    fun addTransaction(amountFen: Long, category: String, note: String = "") =
        addTransactionAt(amountFen, category, note, System.currentTimeMillis())

    /** 补记：可指定时间戳，用于日历页给过去的某天补一笔。 */
    fun addTransactionAt(amountFen: Long, category: String, note: String, timestamp: Long) {
        if (amountFen == 0L) return
        // 万一 App 在后台跨了月，写之前先把统计区间切到正确的月份
        refreshClock()
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
        refreshClock()
        viewModelScope.launch { dao.update(tx) }
    }

    /** 删除一笔，并留一份到「最近删除」+ 发出可撤销事件。 */
    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            val ok = runCatching {
                dao.deleteById(tx.id)
                // 原子更新：快速连删两笔时不会互相覆盖，「最近删除」不会少一条
                _deleted.update { current ->
                    (listOf(tx) + current.filter { it.id != tx.id }).take(DeletedStore.MAX_ITEMS)
                }
                withContext(Dispatchers.IO) { DeletedStore.save(app, _deleted.value) }
            }.onFailure { e ->
                _message.tryEmit("删除失败：${e.message ?: "未知错误"}")
            }.isSuccess
            if (!ok) return@launch
            refreshClock()
            _deletedEvent.tryEmit(tx)
        }
    }

    /** 撤销刚刚那次删除。 */
    fun undoDelete(tx: Transaction) = restoreDeleted(tx)

    /** 从「最近删除」恢复一笔：保留原金额、分类、备注与时间。 */
    fun restoreDeleted(tx: Transaction) {
        if (!restoringIds.add(tx.id)) return
        viewModelScope.launch {
            try {
                runCatching {
                    // id 交给 Room 重新生成，避免与库中已有主键冲突；业务数据完全一致
                    dao.insert(tx.copy(id = 0L))
                    _deleted.update { current -> current.filterNot { it.id == tx.id } }
                    withContext(Dispatchers.IO) { DeletedStore.save(app, _deleted.value) }
                }.onSuccess {
                    _message.tryEmit("已恢复这笔记录")
                    refreshClock()
                }.onFailure { e ->
                    _message.tryEmit("恢复失败：${e.message ?: "未知错误"}")
                }
            } finally {
                // 无论成败都释放，避免异常后这笔永远无法再恢复
                restoringIds.remove(tx.id)
            }
        }
    }

    fun clearDeleted() {
        _deleted.value = emptyList()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { DeletedStore.save(app, emptyList()) }
            }.onFailure { e ->
                _message.tryEmit("清空失败：${e.message ?: "未知错误"}")
            }
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
                    // 覆盖前先在事务外留一份完整快照：写不成功就中止覆盖，
                    // 绝不能出现「提示可撤销、实际没有留底」的丢数据路径
                    var beforeCount = 0
                    if (mode == RestoreMode.REPLACE) {
                        val before = dao.getAllOnce()
                        beforeCount = before.size
                        if (before.isEmpty()) {
                            RestoreSnapshot.clear(app)
                        } else if (!RestoreSnapshot.save(app, before)) {
                            RestoreSnapshot.clear(app)
                            throw IllegalStateException("无法保存原记录快照，已取消覆盖，数据未改动")
                        }
                    }
                    val added = db.withTransaction {
                        val toInsert: List<Transaction> = if (mode == RestoreMode.REPLACE) {
                            dao.deleteAll()
                            backup.items
                        } else {
                            val existing = dao.getAllOnce().map { it.businessKey() }.toHashSet()
                            backup.items
                                .filter { it.businessKey() !in existing }
                                // 备份文件自身也可能含重复条目，一并去掉
                                .distinctBy { it.businessKey() }
                                .map { it.copy(id = 0L) }
                        }
                        if (toInsert.isEmpty()) 0
                        else dao.insertIgnoreAll(toInsert).count { it != -1L }
                    }
                    if (mode == RestoreMode.REPLACE) _snapshotCount.value = beforeCount
                    added to beforeCount
                }
            }.onSuccess { (added, beforeCount) ->
                // 用实际入库条数提示，decode 跳过无效条目、IGNORE 跳过重复时数字仍然准确
                _message.tryEmit(
                    when {
                        mode == RestoreMode.REPLACE && beforeCount > 0 ->
                            "已覆盖恢复 $added 笔记录，原记录可在备份菜单撤销"
                        mode == RestoreMode.REPLACE -> "已覆盖恢复 $added 笔记录"
                        else -> "已合并导入 $added 笔新记录（重复项已跳过）"
                    }
                )
                refreshClock()
            }.onFailure { e ->
                _message.tryEmit("恢复失败：${e.message ?: "未知错误"}")
            }
        }
    }

    /** 撤销上一次「覆盖恢复」，把被清空的记录原样写回。 */
    fun undoReplace() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val snapshot = RestoreSnapshot.load(app)
                    if (snapshot.isEmpty()) throw IllegalStateException("没有可撤销的覆盖记录")
                    db.withTransaction {
                        dao.deleteAll()
                        dao.insertIgnoreAll(snapshot)
                    }
                    RestoreSnapshot.clear(app)
                    snapshot.size
                }
            }.onSuccess { count ->
                _snapshotCount.value = 0
                _message.tryEmit("已撤销覆盖恢复，找回 $count 笔原记录")
                refreshClock()
            }.onFailure { e ->
                _message.tryEmit("撤销失败：${e.message ?: "未知错误"}")
            }
        }
    }

    /** 合并时用于判重的业务键：同一金额、分类、备注、时刻视为同一笔。 */
    private data class BusinessKey(
        val amountFen: Long,
        val category: String,
        val note: String,
        val timestamp: Long
    )

    private fun Transaction.businessKey() = BusinessKey(amountFen, category, note, timestamp)

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
