package com.qingzhang.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingzhang.MainViewModel
import com.qingzhang.ui.theme.QingZhangTheme
import com.qingzhang.ui.theme.rememberIsDarkTheme
import kotlinx.coroutines.launch
import kotlin.math.min

private val PAGE_TITLES = listOf("记账", "流水", "统计")

private val NavLabelStyle = TextStyle(
    lineHeight = 12.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

@Composable
fun QingZhangApp() {
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
    QingZhangTheme(darkTheme = darkTheme) {
        AppContent(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppContent(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = tab) { PAGE_TITLES.size }
    val scope = rememberCoroutineScope()

    val dashboard by vm.dashboard.collectAsState()
    val monthTx by vm.monthTransactions.collectAsState(initial = emptyList())

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
                    )
                )
            }
        ) { padding ->
            val navDockInset = 96.dp +
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val topInset = padding.calculateTopPadding()
            val bottomInset = if (keyboardVisible) 12.dp else navDockInset

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        monthExpense = dashboard.monthExpense,
                        monthIncome = dashboard.monthIncome,
                        monthBalance = dashboard.monthBalance,
                        streak = dashboard.streak,
                        onSave = { amount, cat -> vm.addTransaction(amount, cat) },
                        modifier = Modifier.padding(top = topInset)
                    )
                    1 -> RecordsScreen(
                        transactions = monthTx,
                        onDelete = { vm.deleteTransaction(it) },
                        modifier = Modifier.padding(top = topInset),
                        bottomInset = bottomInset
                    )
                    2 -> StatsScreen(
                        transactions = monthTx,
                        modifier = Modifier.padding(top = topInset),
                        bottomInset = bottomInset
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
    }
}

@Composable
private fun FloatingNavigation(
    selected: Int,
    position: Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    val icons = listOf(AppGlyphs.home, AppGlyphs.records, AppGlyphs.stats)
    val labels = PAGE_TITLES
    val itemWidth = 84.dp
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
                                            icons[index], null, tint = foreground,
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
