package app.shinsatsunote.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.data.AppData
import app.shinsatsunote.data.AppSettings
import app.shinsatsunote.data.ConditionRecord
import app.shinsatsunote.data.Kind
import app.shinsatsunote.domain.dueKinds
import app.shinsatsunote.domain.durJ
import app.shinsatsunote.domain.hm
import app.shinsatsunote.domain.key
import app.shinsatsunote.domain.mdw
import app.shinsatsunote.domain.napMin
import app.shinsatsunote.domain.nightMin
import app.shinsatsunote.domain.parseDate
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppIconButton
import app.shinsatsunote.ui.components.AppSheet
import app.shinsatsunote.ui.components.Chevron
import app.shinsatsunote.ui.components.CountText
import app.shinsatsunote.ui.components.EntryButton
import app.shinsatsunote.ui.components.EntryIcon
import app.shinsatsunote.ui.components.HeaderIconTile
import app.shinsatsunote.ui.components.MiniBattery
import app.shinsatsunote.ui.components.MiniMind
import app.shinsatsunote.ui.components.NoteField
import app.shinsatsunote.ui.components.PrimaryButton
import app.shinsatsunote.ui.components.SheetActions
import app.shinsatsunote.ui.components.SubButton
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun RecordScreen(
    vm: AppViewModel,
    data: AppData,
    settings: AppSettings,
    coachTargets: SnapshotStateMap<String, Rect>,
    onOpenSleep: (String) -> Unit,
    onOpenCondition: (String, Long?) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVisitTab: () -> Unit,
    onHelp: () -> Unit,
    onRecordVisit: (kind: String, date: String) -> Unit,
) {
    val today = LocalDate.now()
    val selected by vm.selectedDate.collectAsState()
    val day = if (selected > today) today else selected
    val k = day.key()
    var addMemo by remember { mutableStateOf(false) }
    val c = AppTheme.colors

    ScreenColumn {
        // 日付ナビ(今日より先には行けない)と、ナビゲーションガイド・設定
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIconButton(AppIcons.Left, "前の日", { vm.selectedDate.value = day.minusDays(1) })
            Text(
                day.mdw(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            AppIconButton(AppIcons.Right, "次の日", { vm.selectedDate.value = day.plusDays(1) }, enabled = day < today)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderIconTile(
                    icon = AppIcons.Help,
                    label = "ナビゲーションガイド",
                    onClick = onHelp,
                    modifier = Modifier.coachTarget(CoachId.RECORD_GUIDE, coachTargets),
                )
                HeaderIconTile(
                    icon = AppIcons.Gear,
                    label = "設定",
                    onClick = onOpenSettings,
                )
            }
        }

        // 予定日の診察・カウンセリング(今日か、その予定日を見ているときだけ)
        dueKinds(day, today, settings.kinds, data.appointments).forEach { kind ->
            val a = data.appointments.getValue(kind)
            DueBanner(
                title = "${if (a.date == today.key()) "今日" else parseDate(a.date).mdw()}の${Kind.label(kind)}を記録",
                sub = "${a.timeMin?.let { hm(it) + "・" } ?: ""}話したこととアドバイスを残す",
                onClick = { onRecordVisit(kind, a.date) },
            )
        }

        // 睡眠: 記録があれば長さだけ。未記録は薄い灰色にして圧をかけない
        val sr = data.sleeps[k]
        EntryButton(
            title = sr?.let { durJ(it.nightMin) } ?: "睡眠",
            subtitle = when {
                sr == null -> "未記録"
                sr.napMin > 0 -> "二度寝 ${durJ(sr.napMin)}"
                else -> null
            },
            icon = AppIcons.Moon,
            muted = sr == null,
            onClick = { onOpenSleep(k) },
            modifier = Modifier
                .coachTarget(CoachId.SLEEP, coachTargets)
                .semantics {
                    contentDescription = sr?.let {
                        "睡眠 ${durJ(it.nightMin)}" + if (it.napMin > 0) "、二度寝 ${durJ(it.napMin)}" else ""
                    } ?: "睡眠 未記録"
                },
        )

        // 調子: 未記録なら薄い灰色の入口、あればその日の行(押すと直す)と右上の件数＋
        val conds = data.conditionsOn(k)
        if (conds.isEmpty()) {
            EntryButton(
                title = "調子",
                onClick = { onOpenCondition(k, null) },
                icon = AppIcons.Smile,
                subtitle = "未記録",
                muted = true,
                modifier = Modifier.coachTarget(CoachId.CONDITION, coachTargets),
            )
        } else {
            ConditionBox(
                conds = conds,
                modifier = Modifier.coachTarget(CoachId.CONDITION, coachTargets),
                onAdd = { onOpenCondition(k, null) },
                onOpen = { onOpenCondition(k, it.id) },
            )
        }

        // 振り返り(旧: これまで)
        EntryButton(
            title = "振り返り",
            onClick = onOpenHistory,
            icon = AppIcons.Rec,
            modifier = Modifier.coachTarget(CoachId.HISTORY, coachTargets),
        )

        // 診察・カウンセリング(両方オフなら出さない)
        val kinds = settings.kinds
        if (kinds.isNotEmpty()) {
            Text(
                if (kinds.size == 1) Kind.label(kinds[0]) else "診察・カウンセリング",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp).semantics { heading() },
            )
            Surface(
                modifier = Modifier.fillMaxWidth().coachTarget(CoachId.TALK, coachTargets),
                shape = MaterialTheme.shapes.medium,
                color = c.surface,
                border = BorderStroke(1.dp, c.line.copy(alpha = 0.7f)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).clickable(onClick = onOpenVisitTab).padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        EntryIcon(AppIcons.Talk)
                        Text("話すこと", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (data.memos.isNotEmpty()) {
                            CountText("${data.memos.size}件")
                        }
                    }
                    AppIconButton(AppIcons.Plus, "話すことを追加", { addMemo = true })
                }
            }
        }
    }

    if (addMemo) {
        MemoAddSheet(
            kinds = settings.kinds,
            onDismiss = { addMemo = false },
            onAdd = { text, kind, pinned -> vm.addMemo(text, kind, pinned); addMemo = false },
        )
    }
}

/** 予定日の記録の通知(.due-banner) */
@Composable
private fun DueBanner(title: String, sub: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = c.warmSoft,
        border = BorderStroke(1.dp, c.warm.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EntryIcon(AppIcons.Visit, bg = c.surface, tint = c.warm, box = 34.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                Text(sub, fontSize = 13.sp, color = c.ink.copy(alpha = 0.85f))
            }
            Chevron()
        }
    }
}

/** その日の調子の一覧(.cond-box) */
@Composable
private fun ConditionBox(
    conds: List<ConditionRecord>,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onOpen: (ConditionRecord) -> Unit,
) {
    val c = AppTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = c.surface,
        border = BorderStroke(1.dp, c.line.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(bottom = 6.dp)) {
            Row(
                Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EntryIcon(AppIcons.Smile)
                Text("調子", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                CountText("${conds.size}件")
                AppIconButton(AppIcons.Plus, "調子を追加", onAdd)
            }
            conds.forEach { r ->
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onOpen(r) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "${hm(r.timeMin)} ココロ${r.mind ?: "-"} カラダ${r.body ?: "-"} ${r.note}(押すと直す)"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(hm(r.timeMin), fontSize = 13.sp, color = c.muted)
                    MiniMind(r.mind?.toDouble())
                    MiniBattery(r.body?.toDouble())
                    Text(
                        r.note.ifEmpty { "ひとことなし" },
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = if (r.note.isEmpty()) c.muted else c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Chevron()
                }
            }
        }
    }
}

/** 話すことを追加するシート */
@Composable
fun MemoAddSheet(
    kinds: List<String>,
    onDismiss: () -> Unit,
    onAdd: (text: String, kind: String?, pinned: Boolean) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf<String?>(null) }
    var pinned by remember { mutableStateOf(false) }
    AppSheet("話すことを追加", onDismiss) {
        if (kinds.isNotEmpty()) {
            MemoRoutingChips(
                kinds = kinds,
                kind = kind,
                pinned = pinned,
                onChange = { k, p -> kind = k; pinned = p },
            )
        }
        NoteField(text, { text = it }, label = "話すこと", autoFocus = true)
        SheetActions {
            SubButton("やめる", onDismiss)
            PrimaryButton("追加", { onAdd(text, kind, pinned) }, icon = AppIcons.Check, enabled = text.isNotBlank())
        }
    }
}
