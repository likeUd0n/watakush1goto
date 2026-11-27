package app.shinsatsunote.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import app.shinsatsunote.data.AppSettings
import app.shinsatsunote.data.Kind
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppSnackbar
import app.shinsatsunote.ui.components.CrisisSheet
import app.shinsatsunote.ui.screens.CoachId
import app.shinsatsunote.ui.screens.CoachMarkOverlay
import app.shinsatsunote.ui.screens.VisitFormSheet
import app.shinsatsunote.ui.screens.coachTarget
import app.shinsatsunote.ui.screens.recordCoachSteps
import app.shinsatsunote.ui.screens.visitCoachSteps
import kotlinx.coroutines.launch
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.screens.ConditionScreen
import app.shinsatsunote.ui.screens.HistoryScreen
import app.shinsatsunote.ui.screens.RecordScreen
import app.shinsatsunote.ui.screens.SettingsScreen
import app.shinsatsunote.ui.screens.SleepScreen
import app.shinsatsunote.ui.screens.VisitScreen
import app.shinsatsunote.ui.theme.AppTheme

/** 画面の行き先。タブは2つ、サブ画面は記録タブから深さ1だけ */
object Routes {
    const val RECORD = "record"
    const val VISIT = "visit"
    const val SLEEP = "sleep/{date}"
    const val CONDITION = "condition/{date}/{id}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    val tabs = setOf(RECORD, VISIT)

    fun sleep(date: String) = "sleep/$date"

    /** id が null なら新しく足す */
    fun condition(date: String, id: Long?) = "condition/$date/${id ?: -1}"
}

/**
 * 2つ目のタブの見せ方。設定の「使うもの」で決まる。
 * 片方だけオンならその名前、両方オフならタブを隠す。
 */
data class VisitTabConfig(val label: String = "診察", val visible: Boolean = true) {
    companion object {
        fun of(settings: AppSettings) = VisitTabConfig(
            label = if (settings.kinds.size == 1) Kind.label(settings.kinds[0]) else "診察",
            visible = settings.kinds.isNotEmpty(),
        )
    }
}

@Composable
fun AppNav(
    vm: AppViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val data by vm.data.collectAsState()
    val settingsOrNull by vm.settings.collectAsState()
    val crisis by vm.crisis.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    // サブ画面を開いているあいだは下のタブを隠す
    val showTabs = route == null || route in Routes.tabs
    val snackbar = remember { SnackbarHostState() }
    var help by remember { mutableStateOf(false) }
    var visitForm by remember { mutableStateOf<Pair<String, String>?>(null) }
    val coachTargets = remember { mutableStateMapOf<String, Rect>() }

    // お知らせ。「元に戻す」が押されたら戻す
    LaunchedEffect(Unit) {
        vm.toasts.collect { t ->
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val r = snackbar.showSnackbar(
                    message = t.message,
                    actionLabel = if (t.undo != null) "元に戻す" else null,
                    duration = if (t.undo != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                if (r == SnackbarResult.ActionPerformed) t.undo?.let { vm.launchUndo(it) }
            }
        }
    }

    val appData = data
    val settings = settingsOrNull
    if (appData == null || settings == null) {
        Box(Modifier.fillMaxSize().background(AppTheme.colors.paper))
        return
    }
    val visitTab = VisitTabConfig.of(settings)

    fun openTab(target: String) {
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppTheme.colors.paper,
            snackbarHost = { SnackbarHost(snackbar) { AppSnackbar(it) } },
            bottomBar = {
                if (showTabs) {
                    BottomTabs(
                        visitTab = visitTab,
                        coachTargets = coachTargets,
                        onSelect = ::openTab,
                        isSelected = { tab -> backStack?.destination?.hierarchy?.any { it.route == tab } == true },
                    )
                }
            },
        ) { inner ->
            NavHost(
                navController = navController,
                startDestination = Routes.RECORD,
                modifier = Modifier.padding(inner),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable(Routes.RECORD) {
                    RecordScreen(
                        vm = vm,
                        data = appData,
                        settings = settings,
                        coachTargets = coachTargets,
                        onOpenSleep = { navController.navigate(Routes.sleep(it)) },
                        onOpenCondition = { d, id -> navController.navigate(Routes.condition(d, id)) },
                        onOpenHistory = { navController.navigate(Routes.HISTORY) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenVisitTab = {
                            vm.visitMode.value = "prep"
                            openTab(Routes.VISIT)
                        },
                        onHelp = { help = true },
                        onRecordVisit = { k, d -> visitForm = k to d },
                    )
                }
                composable(Routes.VISIT) {
                    VisitScreen(
                        vm = vm,
                        data = appData,
                        settings = settings,
                        coachTargets = coachTargets,
                        onHelp = { help = true },
                    )
                }
                composable(Routes.SLEEP, listOf(navArgument("date") { type = NavType.StringType })) { e ->
                    SleepScreen(vm, appData, e.arguments?.getString("date").orEmpty(), onBack = navController::popBackStack)
                }
                composable(
                    Routes.CONDITION,
                    listOf(navArgument("date") { type = NavType.StringType }, navArgument("id") { type = NavType.LongType }),
                ) { e ->
                    val id = e.arguments?.getLong("id")?.takeIf { it >= 0 }
                    ConditionScreen(vm, appData, e.arguments?.getString("date").orEmpty(), id, onBack = navController::popBackStack)
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        data = appData,
                        onBack = navController::popBackStack,
                        onOpenSleep = { navController.navigate(Routes.sleep(it)) },
                        onOpenCondition = { d, id -> navController.navigate(Routes.condition(d, id)) },
                    )
                }
                composable(Routes.SETTINGS) { SettingsScreen(vm, settings, onBack = navController::popBackStack) }
            }
        }

        // 初回オンボーディング または 右上「？」からのナビゲーションガイド（コーチマーク）
        val showFirstCoach = !settings.onboardingDone && (route == null || route == Routes.RECORD)
        if (showFirstCoach) {
            val steps = remember(visitTab.visible) { recordCoachSteps(firstLaunch = true, hasVisitTab = visitTab.visible) }
            CoachMarkOverlay(steps = steps, targets = coachTargets, onFinish = vm::setOnboardingDone)
        } else if (help) {
            val steps = remember(route, visitTab.visible) {
                if (route == Routes.VISIT) visitCoachSteps()
                else recordCoachSteps(firstLaunch = false, hasVisitTab = visitTab.visible)
            }
            CoachMarkOverlay(steps = steps, targets = coachTargets, onFinish = { help = false })
        }
    }

    visitForm?.let { (k, d) -> VisitFormSheet(vm, appData, settings, k, d) { visitForm = null } }
    if (crisis) CrisisSheet(onDismiss = vm::dismissCrisis)
}

/** 下のタブ(.tabs): アイコン＋ラベル。今いるタブは色に加えて背景と太い下線でも示す */
@Composable
private fun BottomTabs(
    visitTab: VisitTabConfig,
    coachTargets: SnapshotStateMap<String, Rect>,
    onSelect: (String) -> Unit,
    isSelected: (String) -> Boolean,
) {
    val colors = AppTheme.colors
    Column(Modifier.background(colors.surface).windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(color = colors.line)
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp).selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            TabItem(AppIcons.Rec, "記録", isSelected(Routes.RECORD)) { onSelect(Routes.RECORD) }
            if (visitTab.visible) {
                TabItem(
                    icon = AppIcons.Visit,
                    label = visitTab.label,
                    selected = isSelected(Routes.VISIT),
                    modifier = Modifier.coachTarget(CoachId.VISIT_TAB, coachTargets),
                ) { onSelect(Routes.VISIT) }
            }
        }
    }
}

@Composable
private fun TabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val tint = if (selected) colors.accent else colors.muted
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .widthIn(min = 72.dp)
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(colors.accent.copy(alpha = 0.08f))
                        .drawBehind {
                            val h = 3.dp.toPx()
                            drawRect(colors.accent, topLeft = Offset(0f, size.height - h), size = size.copy(height = h))
                        }
                } else Modifier,
            )
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = tint)
    }
}
