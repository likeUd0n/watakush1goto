package app.shinsatsunote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.data.AppData
import app.shinsatsunote.data.AppSettings
import app.shinsatsunote.data.ConsultMemo
import app.shinsatsunote.data.Kind
import app.shinsatsunote.domain.key
import app.shinsatsunote.domain.md
import app.shinsatsunote.domain.mdw
import app.shinsatsunote.domain.parseDate
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppCard
import app.shinsatsunote.ui.components.AppIconButton
import app.shinsatsunote.ui.components.CardTitle
import app.shinsatsunote.ui.components.Chevron
import app.shinsatsunote.ui.components.CountText
import app.shinsatsunote.ui.components.EmptyText
import app.shinsatsunote.ui.components.KindBadge
import app.shinsatsunote.ui.components.MiniButton
import app.shinsatsunote.ui.components.Segmented
import app.shinsatsunote.ui.components.Tone
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import app.shinsatsunote.domain.daysSince
import app.shinsatsunote.domain.durJ
import app.shinsatsunote.domain.hm
import app.shinsatsunote.domain.napMin
import app.shinsatsunote.domain.nightMin
import app.shinsatsunote.ui.components.AppSheet
import app.shinsatsunote.ui.components.ChartHead
import app.shinsatsunote.ui.components.HeaderIconTile
import app.shinsatsunote.ui.components.MiniBattery
import app.shinsatsunote.ui.components.MiniMind
import app.shinsatsunote.ui.components.SheetActions
import app.shinsatsunote.ui.components.SleepChart
import app.shinsatsunote.ui.components.SplitMoodCharts
import app.shinsatsunote.ui.components.SubButton
import app.shinsatsunote.ui.components.SummaryRow
import app.shinsatsunote.ui.components.periodAverage
import app.shinsatsunote.domain.AiPrompt
import app.shinsatsunote.domain.buildAiPrompt
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Sheet { None, Appointments, Visits, AddMemo }

/** 診察タブ(準備 / 見せる) */
@Composable
fun VisitScreen(
    vm: AppViewModel,
    data: AppData,
    settings: AppSettings,
    coachTargets: SnapshotStateMap<String, Rect>,
    onHelp: () -> Unit,
) {
    val mode by vm.visitMode.collectAsState()
    var sheet by remember { mutableStateOf(Sheet.None) }
    var editMemo by remember { mutableStateOf<ConsultMemo?>(null) }
    var visitForm by remember { mutableStateOf<Pair<String, String>?>(null) }
    var aiPrompt by remember { mutableStateOf<AiPrompt?>(null) }
    var aiTutorial by remember { mutableStateOf(false) }
    var paste by remember { mutableStateOf(false) }
    var lastPrompt by rememberSaveable { mutableStateOf<String?>(null) }
    val kinds = settings.kinds
    if (kinds.isEmpty()) return

    fun openAiShare() {
        val p = buildAiPrompt(data, kinds, LocalDate.now())
        lastPrompt = p.text
        aiPrompt = p
    }

    ScreenColumn {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Segmented(
                listOf("prep" to "準備", "show" to "見せる"),
                mode,
                { vm.visitMode.value = it },
                Modifier.weight(1f).coachTarget(CoachId.VISIT_SEG, coachTargets),
            )
            HeaderIconTile(
                icon = AppIcons.Cal,
                label = "次の予定",
                onClick = { sheet = Sheet.Appointments },
                modifier = Modifier.coachTarget(CoachId.VISIT_CAL, coachTargets),
            )
            HeaderIconTile(
                icon = AppIcons.Help,
                label = "ナビゲーションガイド",
                onClick = onHelp,
                modifier = Modifier.coachTarget(CoachId.VISIT_GUIDE, coachTargets),
            )
        }
        if (mode == "prep") {
            PrepPane(
                data = data,
                settings = settings,
                coachTargets = coachTargets,
                onAddMemo = { sheet = Sheet.AddMemo },
                onEditMemo = { editMemo = it },
                onClearAi = vm::clearAiSummaries,
                onOpenVisits = { sheet = Sheet.Visits },
                onAi = {
                    if (!settings.aiGuideDone) aiTutorial = true else openAiShare()
                },
                onPaste = { paste = true },
            )
        } else {
            ShowPane(data, settings)
        }
    }

    when (sheet) {
        Sheet.Appointments -> AppointmentsSheet(vm, data, settings) { sheet = Sheet.None }
        Sheet.Visits -> VisitsSheet(
            vm, data, settings,
            onAddVisit = { sheet = Sheet.None; visitForm = kinds[0] to LocalDate.now().key() },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.AddMemo -> MemoAddSheet(
            kinds = kinds,
            onDismiss = { sheet = Sheet.None },
            onAdd = { text, kind, pinned -> vm.addMemo(text, kind, pinned); sheet = Sheet.None },
        )
        Sheet.None -> Unit
    }
    editMemo?.let { m -> MemoEditSheet(vm, m, kinds) { editMemo = null } }
    visitForm?.let { (k, d) -> VisitFormSheet(vm, data, settings, k, d) { visitForm = null } }
    if (aiTutorial) {
        AiTutorialSheet(
            onContinue = {
                aiTutorial = false
                if (!settings.aiGuideDone) vm.setAiGuideDone()
                openAiShare()
            },
            onDismiss = { aiTutorial = false },
        )
    }
    aiPrompt?.let { p ->
        AiShareSheet(
            vm = vm,
            prompt = p,
            onShowTutorial = {
                aiPrompt = null
                aiTutorial = true
            },
            onDismiss = { aiPrompt = null },
        )
    }
    if (paste) AiPasteSheet(vm, kinds, lastPrompt) { paste = false }
}

@Composable
private fun PrepPane(
    data: AppData,
    settings: AppSettings,
    coachTargets: SnapshotStateMap<String, Rect>,
    onAddMemo: () -> Unit,
    onEditMemo: (ConsultMemo) -> Unit,
    onClearAi: () -> Unit,
    onOpenVisits: () -> Unit,
    onAi: () -> Unit,
    onPaste: () -> Unit,
) {
    val kinds = settings.kinds
    val both = kinds.size >= 2
    val c = AppTheme.colors
    var memosExpanded by rememberSaveable { mutableStateOf(false) }
    var aiOpen by rememberSaveable { mutableStateOf(false) }
    var adviceOpen by rememberSaveable { mutableStateOf(false) }

    // 話すこと(4件以上あるときは最新3件だけ見せ、残りはたたむ)
    AppCard(Modifier.coachTarget(CoachId.VISIT_MEMO, coachTargets)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle("話すこと", Modifier.weight(1f), icon = AppIcons.Talk)
            if (data.memos.isNotEmpty()) {
                CountText("${data.memos.size}件")
            }
            AppIconButton(AppIcons.Plus, "話すことを追加", onAddMemo)
        }
        if (data.memos.isEmpty()) EmptyText("なし")
        val visibleMemos = if (memosExpanded || data.memos.size <= 3) data.memos else data.memos.take(3)
        visibleMemos.forEach { m ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { onEditMemo(m) }
                    .semantics(mergeDescendants = true) { contentDescription = "${m.text}(押すと直せます)" }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    m.createdDate().md(),
                    fontSize = 13.sp,
                    color = c.muted,
                    modifier = Modifier.align(Alignment.Top).padding(top = 2.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (m.pinned) {
                        PinBadge(m.kind, both)
                    } else if (both && m.kind != null) {
                        KindBadge(m.kind)
                    }
                    Text(m.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Chevron()
            }
        }
        if (data.memos.size > 3) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .clickable { memosExpanded = !memosExpanded }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (memosExpanded) "たたむ" else "ほか ${data.memos.size - 3}件をすべて見る",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.accent,
                    modifier = Modifier.weight(1f),
                )
                Chevron(Modifier.rotate(if (memosExpanded) 90f else 0f))
            }
        }
        HorizontalDivider(color = c.line)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            MiniButton("AIで整理", onAi, icon = AppIcons.Ai, tone = Tone.Accent)
            MiniButton("答えを貼る", onPaste)
        }
    }

    // AIの整理(貼ってあるときだけ。デフォルト折りたたみ)
    val aiKinds = kinds.filter { data.aiSummaries[it] != null }
    if (aiKinds.isNotEmpty()) {
        AppCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .clickable { aiOpen = !aiOpen },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardTitle(if (both) "AIの整理" else "AIの整理（${Kind.label(aiKinds[0])}向け）", Modifier.weight(1f), icon = AppIcons.Ai)
                Text("反映済み", fontSize = 13.sp, color = c.accent, fontWeight = FontWeight.Bold)
                Chevron(Modifier.rotate(if (aiOpen) 90f else 0f))
            }
            AnimatedVisibility(visible = aiOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = c.line)
                    aiKinds.forEachIndexed { i, k ->
                        if (i > 0) HorizontalDivider(color = c.line)
                        if (both) KindBadge(k)
                        Text(data.aiSummaries.getValue(k).text, fontSize = 15.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        MiniButton("消す", onClearAi)
                    }
                }
            }
        }
    }

    // 前回のアドバイス(デフォルト折りたたみ)
    val last = kinds.mapNotNull { data.lastVisit(it) }
    if (last.isNotEmpty()) {
        AppCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .clickable { adviceOpen = !adviceOpen },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardTitle("前回のアドバイス", Modifier.weight(1f), icon = AppIcons.Visit)
                Text(
                    if (last.size == 1) parseDate(last[0].date).md() else "${last.size}件",
                    fontSize = 13.sp,
                    color = c.muted,
                )
                Chevron(Modifier.rotate(if (adviceOpen) 90f else 0f))
            }
            AnimatedVisibility(visible = adviceOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = c.line)
                    last.forEach { v ->
                        KindStrip(v.kind) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KindBadge(v.kind)
                                Text(parseDate(v.date).mdw(), fontSize = 13.sp, color = c.muted)
                            }
                            Text(v.feedback, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // 過去の記録(旧: これまで)
    val pastTitle = if (kinds.size == 1) "過去の${Kind.label(kinds[0])}" else "過去の記録"
    Surface(
        onClick = onOpenVisits,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
        color = c.surface,
        border = BorderStroke(1.dp, c.line.copy(alpha = 0.65f)),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(AppIcons.Rec, null, Modifier.size(18.dp), tint = c.muted)
            Text(pastTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                "${data.visits.count { it.kind in kinds }}回・話したこと ${data.talked.size}件",
                fontSize = 13.sp,
                color = c.muted,
            )
            Chevron()
        }
    }
}

/** 見せる(前回からの客観データを相手に伝える)。両方オンのときは下線タブ＋左右スワイプで切替 */
@Composable
private fun ShowPane(data: AppData, settings: AppSettings) {
    val kinds = settings.kinds
    val both = kinds.size >= 2
    if (!both) {
        ShowKindContent(kind = kinds[0], both = false, data = data)
        return
    }
    val pagerState = rememberPagerState(pageCount = { kinds.size })
    val scope = rememberCoroutineScope()
    ShowKindTabs(
        kinds = kinds,
        selectedIndex = pagerState.currentPage,
        onSelect = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } },
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        pageSpacing = 12.dp,
    ) { page ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShowKindContent(kind = kinds[page], both = true, data = data)
        }
    }
}

/** 診察 / カウンセリングの下線タブ(上の Segmented と見た目を区別し、スワイプ可能を示す) */
@Composable
private fun ShowKindTabs(
    kinds: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            kinds.forEachIndexed { idx, kind ->
                val on = idx == selectedIndex
                val activeCol = kindColor(kind)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .selectable(selected = on, role = Role.Tab) { onSelect(idx) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = Kind.label(kind),
                            fontSize = 14.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            color = if (on) activeCol else c.muted,
                            maxLines = 1,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on) activeCol else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = c.line)
    }
}

@Composable
private fun ShowKindContent(kind: String, both: Boolean, data: AppData) {
    val mode = kind
    var selectedKey by rememberSaveable(mode) { mutableStateOf<String?>(null) }
    var chartsOpen by rememberSaveable(mode) { mutableStateOf(false) }
    var notesOpen by rememberSaveable(mode) { mutableStateOf(false) }
    var memosOpen by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val last = data.lastVisit(mode)
    val days = daysSince(last?.date, today)
    val c = AppTheme.colors

    val periodText = (if (last != null) "前回から " else "直近 ") +
        if (days.size == 1) days[0].md() else "${days.first().md()}〜${days.last().md()}"

    // 今回話すこと(両方オンのときは分類済みのみ。必ず指定を先頭に表示。AIの整理があれば整理を主役にし、元のメモは折りたたむ)
    val memos = (if (both) data.memos.filter { it.kind == mode } else data.memos).sortedByDescending { it.pinned }
    val ai = data.aiSummaries[mode]
    AppCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(if (ai != null) "今回話すこと（AIの整理）" else "今回話すこと", Modifier.weight(1f))
            Text(periodText, fontSize = 12.sp, color = c.muted, maxLines = 1)
        }
        if (ai != null) {
            Text(ai.text, fontSize = 15.sp)
            if (memos.isNotEmpty()) {
                HorizontalDivider(color = c.line)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 36.dp).clickable { memosOpen = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "元のメモを見る",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = c.accent,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${memos.size}件", fontSize = 13.sp, color = c.muted)
                    Chevron()
                }
            }
        } else {
            if (memos.isEmpty()) EmptyText("なし")
            memos.take(3).forEach { m ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    Text("・", fontSize = 16.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (m.pinned) PinBadge(m.kind, both)
                        Text(m.text, fontSize = 16.sp)
                    }
                }
            }
            if (memos.size > 3) {
                HorizontalDivider(color = c.line)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 36.dp).clickable { memosOpen = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "元のメモをすべて見る",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = c.accent,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${memos.size}件", fontSize = 13.sp, color = c.muted)
                    Chevron()
                }
            }
        }
    }

    if (mode == Kind.DOC) {
        val selected = selectedKey?.let(::parseDate)?.takeIf { it in days }
        AppCard {
            SummaryRow(periodAverage(days, data))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 38.dp).clickable { chartsOpen = !chartsOpen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (chartsOpen) "グラフを閉じる" else "グラフを見る",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = c.accent,
                    modifier = Modifier.weight(1f),
                )
                Chevron(Modifier.rotate(if (chartsOpen) 90f else 0f))
            }
            AnimatedVisibility(visible = chartsOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SplitMoodCharts(days, data, selected, { selectedKey = it.key() })
                    ChartHead("睡眠", listOf(c.accent to "夜", c.nap to "二度寝"))
                    SleepChart(days, data, selected, { selectedKey = it.key() }, showAvg = false)
                }
            }
        }
        if (chartsOpen && selected != null) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selected.mdw(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    AppIconButton(AppIcons.Close, "この日の詳しい記録を閉じる", { selectedKey = null })
                }
                DayDetail(selected, data)
            }
        }
    } else {
        // 最近の出来事・気持ち(ひとことのある記録を新しい順に10件まで。デフォルトは非展開)
        val notes = days.reversed().flatMap { d -> data.conditionsOn(d.key()).reversed().filter { it.note.isNotEmpty() }.map { d to it } }.take(10)
        AppCard {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(enabled = notes.isNotEmpty()) { notesOpen = !notesOpen },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardTitle("最近の出来事・気持ち", Modifier.weight(1f))
                Text(
                    if (notes.isEmpty()) "なし" else "${notes.size}件",
                    fontSize = 13.sp,
                    color = c.muted,
                )
                if (notes.isNotEmpty()) Chevron(Modifier.rotate(if (notesOpen) 90f else 0f))
            }
            AnimatedVisibility(visible = notesOpen && notes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { (d, r) ->
                        HorizontalDivider(color = c.line)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${d.md()} ${hm(r.timeMin)}", fontSize = 13.sp, color = c.muted)
                                MiniMind(r.mind?.toDouble())
                                MiniBattery(r.body?.toDouble())
                            }
                            Text(r.note, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        if (last != null) {
            AppCard {
                CardTitle("前回の振り返り")
                Text(parseDate(last.date).mdw(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c.accent)
                Text(last.feedback, fontSize = 15.sp)
            }
        }
    }

    if (memosOpen) {
        AppSheet("元のメモ（${Kind.label(mode)}）", { memosOpen = false }) {
            memos.forEach { m ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("・", fontSize = 15.sp)
                    Text(m.text, fontSize = 15.sp)
                }
            }
            SheetActions { SubButton("閉じる", { memosOpen = false }) }
        }
    }
}

/** 見せる画面の、その日の詳しい記録 */
@Composable
private fun DayDetail(date: LocalDate, data: AppData) {
    val c = AppTheme.colors
    val sr = data.sleeps[date.key()]
    val conds = data.conditionsOn(date.key())
    if (sr == null && conds.isEmpty()) {
        Text("記録なし", fontSize = 14.sp)
        return
    }
    if (sr != null) {
        Text("${hm(sr.sleepMin)}〜${hm(sr.wakeMin)} (${durJ(sr.nightMin)})", fontSize = 14.sp)
        if (sr.napSleepMin != null && sr.napWakeMin != null) {
            Text("二度寝 ${hm(sr.napSleepMin)}〜${hm(sr.napWakeMin)} (${durJ(sr.napMin)})", fontSize = 14.sp)
        }
    }
    conds.forEach { r ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(hm(r.timeMin), fontSize = 13.sp, color = c.muted)
            MiniMind(r.mind?.toDouble())
            MiniBattery(r.body?.toDouble())
            Text(r.note, fontSize = 14.sp)
        }
    }
}
