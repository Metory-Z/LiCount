package com.licount.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.licount.MainViewModel
import com.licount.RestoreMode
import com.licount.ui.theme.LiCountTheme
import com.licount.ui.theme.rememberIsDarkTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private fun backupFileName(): String =
    "LiCount-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.json"

private val PAGE_TITLES = listOf("记账", "流水", "统计", "日历")

private val NavLabelStyle = TextStyle(
    lineHeight = 12.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

@Composable
fun LiCountApp() {
    val vm: MainViewModel = viewModel()
    val darkTheme = isSystemInDarkTheme()
    val activity = LocalContext.current as? Activity
    SideEffect {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    LiCountTheme(darkTheme = darkTheme) {
        AppContent(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppContent(vm: MainViewModel) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var showBackupMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(initialPage = tab) { PAGE_TITLES.size }
    val scope = rememberCoroutineScope()

    val dashboard by vm.dashboard.collectAsState()
    val monthTx by vm.monthTransactions.collectAsState(initial = emptyList())
    val pendingRestore by vm.pendingRestore.collectAsState()
    val deleted by vm.deleted.collectAsState()
    val snapshotCount by vm.snapshotCount.collectAsState()

    // 各分类累计使用次数，用于首页与编辑弹窗的「常用优先」排序
    val categoryCounts = remember(dashboard.recent) {
        dashboard.recent.groupingBy { it.category }.eachCount()
    }

    // 系统文件选择器：导出用 CreateDocument，导入用 OpenDocument（无需任何存储权限）
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportTo(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.prepareRestore(it) } }

    LaunchedEffect(Unit) {
        vm.message.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 删除后立刻给一次反悔机会；不点撤销也能在「最近删除」里找回
    LaunchedEffect(Unit) {
        vm.deletedEvent.collect { tx ->
            // 每笔删除独立弹提示：连续删两笔时后一条直接顶掉前一条，不会排队等 10 秒。
            // 被顶掉的那笔仍留在「最近删除」里可找回。
            launch {
                val label = (if (tx.amountFen > 0) "支出 " else "收入 ") +
                    formatMoney(kotlin.math.abs(tx.amountFen))
                val result = snackbarHostState.showSnackbar(
                    message = "已删除 $label",
                    actionLabel = "撤销",
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
                if (result == SnackbarResult.ActionPerformed) vm.undoDelete(tx)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { tab = it }
    }

    val title = PAGE_TITLES[tab]
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = { showBackupMenu = true }) {
                            Icon(
                                AppGlyphs.archive,
                                contentDescription = "备份与恢复",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            val navBarBottom =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            // 首页保存键贴着卡片底边，要完整避开悬浮药丸；三个可滚动页只需
            // 让最后一项能滚到药丸上方，多出的 24dp 正是日历下方那块空白
            val homeInset = 96.dp + navBarBottom
            val scrollInset = 72.dp + navBarBottom
            val topInset = padding.calculateTopPadding()
            val homeBottomInset = if (keyboardVisible) 12.dp else homeInset
            val scrollBottomInset = if (keyboardVisible) 12.dp else scrollInset

            HorizontalPager(
                state = pagerState,
                // 四个页面全部常驻，切 tab 回来不会销毁组合、丢掉输了一半的金额或所选月份
                beyondViewportPageCount = 3,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        monthExpense = dashboard.monthExpense,
                        monthIncome = dashboard.monthIncome,
                        todayRecorded = dashboard.todayRecorded,
                        onSave = { amount, cat -> vm.addTransaction(amount, cat) },
                        categoryCounts = categoryCounts,
                        modifier = Modifier.padding(top = topInset),
                        bottomInset = homeBottomInset
                    )
                    1 -> RecordsScreen(
                        // 流水显示全部记录（含历史月份），否则恢复旧备份后看不到数据
                        transactions = dashboard.recent,
                        categoryCounts = categoryCounts,
                        onDelete = { vm.deleteTransaction(it) },
                        onEdit = { vm.updateTransaction(it) },
                        modifier = Modifier.padding(top = topInset),
                        bottomInset = scrollBottomInset
                    )
                    2 -> StatsScreen(
                        transactions = monthTx,
                        categoryCounts = categoryCounts,
                        onEdit = { vm.updateTransaction(it) },
                        onDelete = { vm.deleteTransaction(it) },
                        modifier = Modifier.padding(top = topInset),
                        bottomInset = scrollBottomInset
                    )
                    3 -> CalendarScreen(
                        transactions = dashboard.recent,
                        categoryCounts = categoryCounts,
                        onDelete = { vm.deleteTransaction(it) },
                        onEdit = { vm.updateTransaction(it) },
                        onAdd = { amount, cat, note, time ->
                            vm.addTransactionAt(amount, cat, note, time)
                        },
                        modifier = Modifier.padding(top = topInset),
                        bottomInset = scrollBottomInset
                    )
                }
            }
        }
        if (!keyboardVisible) {
            FloatingNavigation(
                selected = tab,
                position = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                onSelect = { index ->
                    tab = index
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        }

        // 抬到悬浮导航栏上方，避免被药丸挡住；提示本身换成 App 同款白卡片
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
            snackbar = { data -> ThemedSnackbar(data) }
        )

        if (showBackupMenu) {
            BackupMenuDialog(
                totalCount = dashboard.recent.size,
                deletedCount = deleted.size,
                snapshotCount = snapshotCount,
                onUndoReplace = {
                    showBackupMenu = false
                    vm.undoReplace()
                },
                onExport = {
                    showBackupMenu = false
                    exportLauncher.launch(backupFileName())
                },
                onImport = {
                    showBackupMenu = false
                    // */* 兼容部分文件管理器把 .json 识别为 octet-stream 的情况
                    importLauncher.launch(arrayOf("*/*"))
                },
                onOpenDeleted = {
                    showBackupMenu = false
                    showDeleted = true
                },
                onDismiss = { showBackupMenu = false }
            )
        }

        if (showDeleted) {
            RecentDeletedDialog(
                items = deleted,
                onRestore = { vm.restoreDeleted(it) },
                onClear = { vm.clearDeleted() },
                onDismiss = { showDeleted = false }
            )
        }

        pendingRestore?.let { pending ->
            RestoreConfirmDialog(
                count = pending.count,
                exportedAt = pending.exportedAt,
                onMerge = { vm.confirmRestore(RestoreMode.MERGE) },
                onReplace = { vm.confirmRestore(RestoreMode.REPLACE) },
                onDismiss = { vm.cancelRestore() }
            )
        }
    }
}

/**
 * 撤销提示：M3 默认 Snackbar 用 inverseSurface（浅色主题下接近纯黑），
 * 和全 App 的白卡片 / 线性图标不是一个语言。这里换成同款超椭圆白卡 + 主色动作。
 * 圆角用矮卡档位 BlockCorner，与流水行、统计分类行一致；ControlCorner 是按键档位，
 * 用在这张悬浮卡上会显得过方。
 */
@Composable
private fun ThemedSnackbar(data: SnackbarData) {
    val visuals = data.visuals
    DashboardCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        radius = BlockCorner,
        contentPadding = PaddingValues(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                visuals.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (!visuals.actionLabel.isNullOrEmpty()) {
                TextButton(
                    onClick = { data.performAction() },
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        visuals.actionLabel!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (visuals.withDismissAction) {
                IconButton(onClick = { data.dismiss() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavigation(
    selected: Int,
    position: Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    val icons = listOf(AppGlyphs.home, AppGlyphs.records, AppGlyphs.stats, AppGlyphs.calendar)
    val labels = PAGE_TITLES
    // 四个 tab 时收窄单项，避免药丸在窄屏贴边
    val itemWidth = 76.dp
    val itemHeight = 54.dp
    val gap = 4.dp
    val pad = 6.dp
    val lensOffset = (itemWidth + gap) * position.coerceIn(0f, (labels.size - 1).toFloat())
    val dark = rememberIsDarkTheme()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(Modifier.navigationDockShadow(dark)) {
            Surface(
                shape = SquircleCapsule,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(Modifier.padding(pad)) {
                    Box(
                        Modifier.offset(x = lensOffset)
                            .size(width = itemWidth, height = itemHeight)
                            .clip(SquircleCapsule)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        labels.forEachIndexed { index, label ->
                            val active = selected == index
                            val foreground by animateColorAsState(
                                if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "nav-fg-$index"
                            )
                            Box(
                                Modifier
                                    .clip(SquircleCapsule)
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { onSelect(index) }
                                    )
                                    .semantics {
                                        // 显式 receiver：FloatingNavigation 的参数也叫 selected（Int），会遮蔽语义属性
                                        this.selected = active
                                        stateDescription = if (active) "已选中" else "未选中"
                                    }
                            ) {
                                Box(
                                    Modifier.width(itemWidth).height(itemHeight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            icons[index],
                                            contentDescription = label,
                                            tint = foreground,
                                            modifier = Modifier.size(27.dp)
                                        )
                                        Text(
                                            label, fontSize = 10.sp, color = foreground,
                                            fontWeight = if (active) FontWeight.SemiBold
                                            else FontWeight.Normal,
                                            maxLines = 1,
                                            style = NavLabelStyle
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.navigationDockShadow(dark: Boolean) = drawWithCache {
    val specs: List<Triple<Float, Float, Float>> = if (dark) listOf(
        Triple(0x66 / 255f, 1.dp.toPx(), 3.dp.toPx()),
        Triple(0x59 / 255f, 4.dp.toPx(), 10.dp.toPx())
    ) else listOf(
        Triple(0x0D / 255f, 0.5f, 1.dp.toPx()),
        Triple(0x12 / 255f, 2.dp.toPx(), 8.dp.toPx())
    )
    val path = squirclePath(size, min(size.width, size.height) / 2f, CapsuleExponent)
    val paints = specs.map { (alpha, dy, blur) ->
        Paint().also {
            val fw = it.asFrameworkPaint()
            fw.isAntiAlias = true
            fw.setColor(android.graphics.Color.TRANSPARENT)
            fw.setShadowLayer(blur, 0f, dy, Color.Black.copy(alpha = alpha).toArgb())
        }
    }
    onDrawBehind {
        drawIntoCanvas { canvas ->
            for (paint in paints) canvas.drawPath(path, paint)
        }
    }
}
