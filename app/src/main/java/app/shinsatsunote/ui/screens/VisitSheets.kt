package app.shinsatsunote.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.shinsatsunote.data.Appointment
import app.shinsatsunote.data.ConsultMemo
import app.shinsatsunote.data.Kind
import app.shinsatsunote.domain.daysLeft
import app.shinsatsunote.domain.hm
import app.shinsatsunote.domain.key
import app.shinsatsunote.domain.leftText
import app.shinsatsunote.domain.md
import app.shinsatsunote.domain.mdw
import app.shinsatsunote.domain.parseDate
import app.shinsatsunote.domain.weekdayJ
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppSheet
import app.shinsatsunote.ui.components.KindBadge
import app.shinsatsunote.ui.components.MiniButton
import app.shinsatsunote.ui.components.NoteField
import app.shinsatsunote.ui.components.PrimaryButton
import app.shinsatsunote.ui.components.Segmented
import app.shinsatsunote.ui.components.SheetActions
import app.shinsatsunote.ui.components.SubButton
import app.shinsatsunote.ui.components.TimeSlot
import app.shinsatsunote.ui.components.TimeWheelSheet
import app.shinsatsunote.ui.components.Tone
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** 相手の色(診察は青、カウンセリングは橙) */
@Composable
fun kindColor(kind: String): Color = if (kind == Kind.COUN) AppTheme.colors.warm else AppTheme.colors.accent

fun ConsultMemo.createdDate(): LocalDate = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

// ---- 日付・時刻の入力欄 ----

/** 日付の欄。押すとカレンダーを出す。null は未定 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    FieldCell(label, value?.let { "${it.year}/${it.mdw()}" } ?: "未定", modifier) { open = true }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ open = false }) { Text("やめる") } },
            colors = DatePickerDefaults.colors(containerColor = AppTheme.colors.surface),
        ) { DatePicker(state, showModeToggle = false, colors = DatePickerDefaults.colors(containerColor = AppTheme.colors.surface)) }
    }
}

/** 時刻の欄。押すと5分きざみのホイールを出す。null は未定 */
@Composable
fun TimeField(label: String, value: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    FieldCell(label, value?.let(::hm) ?: "--:--", modifier) { open = true }
    if (open) {
        TimeWheelSheet(
            title = label,
            slots = listOf(TimeSlot(label, value ?: 10 * 60)),
            onDismiss = { open = false },
            onConfirm = { onChange(it[0]); open = false },
        )
    }
}

@Composable
private fun FieldCell(label: String, text: String, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(
        modifier.clickable(onClick = onClick).semantics(mergeDescendants = true) { contentDescription = "$label $text" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 13.sp, color = c.muted, modifier = Modifier.padding(top = 6.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.heightIn(min = 36.dp).padding(top = 4.dp))
        HorizontalDivider(color = c.line)
    }
}

/** 日付・時刻の2つ並び(.times) */
@Composable
fun DateTimeRow(date: LocalDate?, time: Int?, onDate: (LocalDate?) -> Unit, onTime: (Int?) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DateField("日付", date, onDate, Modifier.weight(1f))
        TimeField("時刻", time, onTime, Modifier.weight(1f))
    }
}

/** 左に色帯のある囲み(.advice-item / .appt-view-item) */
@Composable
fun KindStrip(kind: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val col = kindColor(kind)
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(12.dp))
            .background(col.copy(alpha = if (kind == Kind.COUN) 0.07f else 0.06f)),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(col))
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

// ---- 次の予定 ----

/** 次の予定。開いた直後は見る用、「予定を編集」で入力に切り替える */
@Composable
fun AppointmentsSheet(vm: AppViewModel, data: AppData, settings: AppSettings, onDismiss: () -> Unit) {
    val kinds = settings.kinds
    val today = LocalDate.now()
    var editing by remember { mutableStateOf(false) }
    val dates = remember { mutableStateMapOf<String, LocalDate?>().apply { kinds.forEach { put(it, data.appointments[it]?.date?.let(::parseDate)) } } }
    val times = remember { mutableStateMapOf<String, Int?>().apply { kinds.forEach { put(it, data.appointments[it]?.timeMin) } } }
    val c = AppTheme.colors

    AppSheet(if (editing) "次の予定を編集" else "次の予定", onDismiss) {
        if (!editing) {
            kinds.forEach { k ->
                val a = data.appointments[k]
                KindStrip(k) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        KindBadge(k)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                            if (a != null) {
                                Text(a.leftText(today), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (a.daysLeft(today) <= 0) c.warm else c.muted)
                            }
                        }
                    }
                    if (a == null) {
                        Text("まだ決まっていません", fontSize = 15.sp, color = c.muted)
                    } else {
                        val d = parseDate(a.date)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(d.md(), style = MaterialTheme.typography.titleLarge, fontSize = 22.sp)
                                Text("(${d.weekdayJ()})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.muted)
                            }
                            a.timeMin?.let { Text(hm(it), style = MaterialTheme.typography.titleLarge, fontSize = 20.sp, color = kindColor(k)) }
                        }
                    }
                }
            }
            SheetActions {
                SubButton("閉じる", onDismiss)
                PrimaryButton(if (kinds.any { data.appointments[it] != null }) "予定を編集" else "予定を設定", { editing = true })
            }
        } else {
            kinds.forEachIndexed { i, k ->
                if (i > 0) HorizontalDivider(color = c.line)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Kind.label(k), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    val a = dates[k]?.let { Appointment(k, it.key(), times[k]) }
                    Text(
                        a?.leftText(today) ?: "未定",
                        fontSize = 13.sp,
                        color = if (a != null && a.daysLeft(today) <= 0) c.warm else c.muted,
                        modifier = Modifier.padding(start = 6.dp).weight(1f),
                    )
                    if (dates[k] != null) MiniButton("消す", { dates[k] = null; times[k] = null }, tone = Tone.Warm)
                }
                DateTimeRow(dates[k], times[k], { dates[k] = it }, { times[k] = it })
            }
            SheetActions {
                SubButton("戻る", { editing = false })
                PrimaryButton("保存", {
                    vm.setAppointments(kinds.associateWith { k -> dates[k]?.let { Appointment(k, it.key(), times[k]) } })
                    onDismiss()
                })
            }
        }
    }
}

// ---- 診察・カウンセリングを記録する ----

/**
 * 記録するシート。チェックした話すことは「話したこと」へ移る(その相手向け＋未分類を最初からチェック)。
 * 予定日のバナーから開くと、相手と日付は予定のものが入る
 */
@Composable
fun VisitFormSheet(
    vm: AppViewModel,
    data: AppData,
    settings: AppSettings,
    kind0: String,
    date0: String,
    onDismiss: () -> Unit,
) {
    val kinds = settings.kinds
    val both = kinds.size >= 2
    var kind by remember { mutableStateOf(kind0) }
    var date by remember { mutableStateOf<LocalDate?>(parseDate(date0)) }
    fun defaultChecks(k: String) = data.memos.filter { !both || it.kind == null || it.kind == k }.map { it.id }.toSet()
    var checked by remember { mutableStateOf(defaultChecks(kind0)) }
    var feedback by remember { mutableStateOf("") }
    var nextDate by remember { mutableStateOf<LocalDate?>(null) }
    var nextTime by remember { mutableStateOf<Int?>(null) }
    val c = AppTheme.colors

    AppSheet("記録する", onDismiss) {
        if (both) {
            Segmented(kinds.map { it to Kind.label(it) }, kind, { kind = it; checked = defaultChecks(it) }, Modifier.fillMaxWidth())
        }
        DateField("日付", date, { date = it }, Modifier.fillMaxWidth())
        if (data.memos.isNotEmpty()) {
            Text("話したこと（外したものは次回へ残ります）", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.paper).padding(horizontal = 4.dp)) {
                data.memos.forEach { m ->
                    val on = m.id in checked
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            .clickable(role = Role.Checkbox) { checked = if (on) checked - m.id else checked + m.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(on, null, Modifier.padding(horizontal = 8.dp), colors = CheckboxDefaults.colors(checkedColor = c.accent))
                        if (both && m.kind != null) {
                            KindBadge(m.kind)
                            Text(" ")
                        }
                        Text(m.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                    }
                }
            }
        }
        Text("アドバイス", style = MaterialTheme.typography.titleSmall)
        NoteField(feedback, { feedback = it }, label = "アドバイス", minLines = 3)
        Text("次の予定（決まっていれば）", style = MaterialTheme.typography.titleSmall)
        DateTimeRow(nextDate, nextTime, { nextDate = it }, { nextTime = it })
        SheetActions {
            SubButton("やめる", onDismiss)
            PrimaryButton("保存", {
                vm.recordVisit(
                    kind = kind,
                    date = (date ?: LocalDate.now()).key(),
                    talkedIds = checked,
                    feedback = feedback,
                    next = nextDate?.let { Appointment(kind, it.key(), nextTime) },
                )
                onDismiss()
            })
        }
    }
}

// ---- 過去の記録と話したこと ----
@Composable
fun VisitsSheet(
    vm: AppViewModel,
    data: AppData,
    settings: AppSettings,
    onAddVisit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AppTheme.colors
    val title = if (settings.kinds.size == 1) "過去の${Kind.label(settings.kinds[0])}" else "過去の記録"
    AppSheet(title, onDismiss) {
        Text("記録", style = MaterialTheme.typography.titleSmall)
        val visits = data.visits.filter { it.kind in settings.kinds }
        if (visits.isEmpty()) Text("なし", style = MaterialTheme.typography.bodySmall)
        visits.forEach { v ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(parseDate(v.date).mdw(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    KindBadge(v.kind)
                }
                Text(v.talkedSummary, fontSize = 14.sp)
                Text(v.feedback, fontSize = 14.sp, color = c.accent)
            }
        }
        MiniButton("＋ 予定になかった日を記録", onAddVisit)
        Text("話したこと", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        if (data.talked.isEmpty()) Text("なし", style = MaterialTheme.typography.bodySmall)
        data.talked.forEach { m ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(m.createdDate().md(), fontSize = 13.sp, color = c.muted)
                Text(m.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                MiniButton("戻す", { vm.returnTalked(m) }, modifier = Modifier.semantics { contentDescription = "「${m.text.take(16)}」を話すことへ戻す" })
            }
        }
        SheetActions { SubButton("閉じる", onDismiss) }
    }
}

/** 相談メモの振り分け指定バッジ(診察・カウンセリング・優先) */
@Composable
fun PinBadge(kind: String?, both: Boolean) {
    val c = AppTheme.colors
    val col = when {
        both && kind == Kind.COUN -> c.warm
        both && kind == Kind.DOC -> c.accent
        else -> c.warm
    }
    val label = when {
        both && kind == Kind.DOC -> "診察"
        both && kind == Kind.COUN -> "カウンセリング"
        else -> "優先"
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = col.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, col.copy(alpha = 0.55f)),
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = col,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** 相談メモの振り分け指定チップ(追加・編集で共用) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoRoutingChips(
    kinds: List<String>,
    kind: String?,
    pinned: Boolean,
    onChange: (kind: String?, pinned: Boolean) -> Unit,
) {
    val c = AppTheme.colors
    val both = kinds.size >= 2
    val options: List<Triple<String, Pair<String?, Boolean>, Color>> = if (both) {
        listOf(
            Triple("おまかせ", (null to false), c.accent),
            Triple("診察", (Kind.DOC to true), c.accent),
            Triple("カウンセリング", (Kind.COUN to true), c.warm),
        )
    } else {
        val targetKind = kinds.firstOrNull()
        listOf(
            Triple("普通", (targetKind to false), c.accent),
            Triple("優先", (targetKind to true), c.warm),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("振り分け指定", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (label, value, activeCol) ->
                val selected = if (both) {
                    if (value.second) pinned && kind == value.first else !pinned
                } else {
                    pinned == value.second
                }
                Surface(
                    onClick = { onChange(value.first, value.second) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) activeCol.copy(alpha = 0.14f) else c.surface,
                    border = BorderStroke(1.dp, if (selected) activeCol.copy(alpha = 0.55f) else c.line),
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) activeCol else c.muted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

// ---- 話すことを直す ----
@Composable
fun MemoEditSheet(vm: AppViewModel, memo: ConsultMemo, kinds: List<String>, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(memo.text) }
    var kind by remember { mutableStateOf(memo.kind) }
    var pinned by remember { mutableStateOf(memo.pinned) }
    AppSheet("話すことを直す", onDismiss) {
        MemoRoutingChips(
            kinds = kinds,
            kind = kind,
            pinned = pinned,
            onChange = { k, p -> kind = k; pinned = p },
        )
        NoteField(text, { text = it }, label = "話すこと")
        SheetActions(start = { MiniButton("消す", { vm.deleteMemo(memo); onDismiss() }, icon = AppIcons.Trash, tone = Tone.Warm) }) {
            SubButton("やめる", onDismiss)
            PrimaryButton("保存", { vm.updateMemo(memo, text, kind, pinned); onDismiss() }, enabled = text.isNotBlank())
        }
    }
}
