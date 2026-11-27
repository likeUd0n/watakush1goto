package app.shinsatsunote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.data.AppData
import app.shinsatsunote.data.ConditionRecord
import app.shinsatsunote.domain.avgBody
import app.shinsatsunote.domain.avgMind
import app.shinsatsunote.domain.durJ
import app.shinsatsunote.domain.durShort
import app.shinsatsunote.domain.hm
import app.shinsatsunote.domain.key
import app.shinsatsunote.domain.level
import app.shinsatsunote.domain.md
import app.shinsatsunote.domain.mdw
import app.shinsatsunote.domain.monday
import app.shinsatsunote.domain.monthDays
import app.shinsatsunote.domain.napMin
import app.shinsatsunote.domain.nightMin
import app.shinsatsunote.domain.parseDate
import app.shinsatsunote.domain.totalMin
import app.shinsatsunote.domain.weekdayJ
import app.shinsatsunote.ui.components.AppCard
import app.shinsatsunote.ui.components.AppIconButton
import app.shinsatsunote.ui.components.ChartHead
import app.shinsatsunote.ui.components.Chevron
import app.shinsatsunote.ui.components.EmptyText
import app.shinsatsunote.ui.components.EntryIcon
import app.shinsatsunote.ui.components.MiniBattery
import app.shinsatsunote.ui.components.MiniButton
import app.shinsatsunote.ui.components.MiniMind
import app.shinsatsunote.ui.components.MoodChart
import app.shinsatsunote.ui.components.MoodFilter
import app.shinsatsunote.ui.components.MoodFilterChips
import app.shinsatsunote.ui.components.SleepChart
import app.shinsatsunote.ui.components.SummaryRow
import app.shinsatsunote.ui.components.periodAverage
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** これまで(個人で長期的に振り返る)。月ごとのグラフと週ごとの折りたたみ一覧 */
@Composable
fun HistoryScreen(
    data: AppData,
    onBack: () -> Unit,
    onOpenSleep: (String) -> Unit,
    onOpenCondition: (String, Long?) -> Unit,
) {
    val today = LocalDate.now()
    var ym by rememberSaveable { mutableStateOf(today.year * 12 + today.monthValue - 1) }
    var raw by rememberSaveable { mutableStateOf(false) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var moodFilter by rememberSaveable { mutableStateOf(MoodFilter.ALL) }
    val year = ym / 12
    val month = ym % 12 + 1
    val isThisMonth = year == today.year && month == today.monthValue
    val days = monthDays(year, month)
    val selected = selectedKey?.let(::parseDate)?.takeIf { it in days }
    val c = AppTheme.colors

    ScreenColumn {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIconButton(AppIcons.Left, "記録へ戻る", onBack)
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                AppIconButton(AppIcons.Left, "前の月", { ym--; selectedKey = null })
                Text("${year}年${month}月", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                AppIconButton(AppIcons.Right, "次の月", { ym++; selectedKey = null }, enabled = !isThisMonth)
            }
            MiniButton(
                if (raw) "グラフ" else "一覧",
                { raw = !raw },
                icon = if (raw) AppIcons.Chart else AppIcons.ListIcon,
                modifier = Modifier.semantics { contentDescription = if (raw) "グラフ表示に戻す" else "生の記録の一覧に切り替え" },
            )
        }

        if (!raw) {
            AppCard {
                SummaryRow(periodAverage(days.filter { it <= today }, data))
                ChartHead("ココロ・カラダ", listOf(c.accent to "ココロ", c.body to "カラダ"))
                MoodFilterChips(moodFilter, { moodFilter = it })
                MoodChart(days, data, selected, { selectedKey = it.key() }, shareAxis = true, filter = moodFilter)
                ChartHead("睡眠", listOf(c.accent to "夜", c.nap to "二度寝"), dashed = "平均")
                SleepChart(days, data, selected, { selectedKey = it.key() }, showAvg = true)
                Text("点や棒を押すと、その日の記録を見られます", style = MaterialTheme.typography.bodySmall)
            }
            if (selected != null) {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selected.mdw(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        AppIconButton(AppIcons.Close, "この日の記録を閉じる", { selectedKey = null })
                    }
                    val has = data.sleeps[selected.key()] != null || data.conditionsOn(selected.key()).isNotEmpty()
                    if (has) DayItems(selected, data, onOpenSleep, onOpenCondition) else EmptyText("この日の記録はありません")
                }
            }
        }

        // 週ごとの折りたたみ(新しい週から)。一覧モードでは全部開く
        val weeks = days.filter { it <= today }.reversed()
            .filter { data.sleeps[it.key()] != null || data.conditionsOn(it.key()).isNotEmpty() }
            .groupBy { it.monday() }
        if (weeks.isEmpty()) {
            EmptyText("この月の記録はありません", Modifier.fillMaxWidth().padding(vertical = 12.dp))
        }
        val thisMon = today.monday()
        weeks.forEach { (mon, wdays) ->
            val ago = ChronoUnit.WEEKS.between(mon, thisMon)
            WeekFold(
                mon = mon,
                days = wdays,
                data = data,
                title = if (isThisMonth) {
                    when (ago) {
                        0L -> "今週"
                        1L -> "先週"
                        else -> "${ago}週間前"
                    }
                } else {
                    "${mon.md()}〜${mon.plusDays(6).md()}"
                },
                sub = if (isThisMonth) "${mon.md()}〜${mon.plusDays(6).md()}" else "${wdays.size}日分",
                initiallyOpen = raw || (isThisMonth && ago == 0L),
                openKey = raw,
                today = today,
                onOpenSleep = onOpenSleep,
                onOpenCondition = onOpenCondition,
            )
        }
    }
}

@Composable
private fun WeekFold(
    mon: LocalDate,
    days: List<LocalDate>,
    data: AppData,
    title: String,
    sub: String,
    initiallyOpen: Boolean,
    openKey: Boolean,
    today: LocalDate,
    onOpenSleep: (String) -> Unit,
    onOpenCondition: (String, Long?) -> Unit,
) {
    val c = AppTheme.colors
    var open by rememberSaveable(mon.key(), openKey) { mutableStateOf(initiallyOpen) }
    val minds = days.mapNotNull { data.conditionsOn(it.key()).avgMind()?.level() }
    val sleeps = days.mapNotNull { data.sleeps[it.key()]?.totalMin?.takeIf { m -> m > 0 } }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = c.surface,
        border = BorderStroke(1.dp, c.line.copy(alpha = 0.65f)),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { open = !open }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                    Text(sub, fontSize = 13.sp, color = c.muted)
                }
                if (minds.isNotEmpty()) {
                    MiniMind(minds.min().toDouble(), size = 16.dp)
                    val lo = minds.min()
                    val hi = minds.max()
                    Text(if (lo == hi) "$lo" else "$lo〜$hi", fontSize = 13.sp, color = c.muted)
                }
                Icon(AppIcons.Moon, null, Modifier.size(15.dp), tint = c.muted)
                Text(sleeps.takeIf { it.isNotEmpty() }?.let { durShort(it.average().toInt()) } ?: "-", fontSize = 13.sp, color = c.muted)
                Box(Modifier.rotate(if (open) 90f else 0f)) { Chevron() }
            }
            AnimatedVisibility(open) {
                Column {
                    HorizontalDivider(color = c.line)
                    days.forEach { d ->
                        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                            Column(
                                Modifier.width(44.dp).padding(top = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "${d.dayOfMonth}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 20.sp,
                                    color = if (d == today) c.accent else c.ink,
                                )
                                Text(
                                    if (d == today) "今日" else d.weekdayJ().toString(),
                                    fontSize = 12.sp,
                                    color = if (d.dayOfWeek.value == 7) c.warm else c.muted,
                                )
                            }
                            Box(Modifier.weight(1f)) { DayItems(d, data, onOpenSleep, onOpenCondition) }
                        }
                    }
                }
            }
        }
    }
}

/** その日の記録の行(睡眠・調子)。押すと編集画面へ */
@Composable
fun DayItems(
    date: LocalDate,
    data: AppData,
    onOpenSleep: (String) -> Unit,
    onOpenCondition: (String, Long?) -> Unit,
) {
    val k = date.key()
    val c = AppTheme.colors
    val sr = data.sleeps[k]
    val conds = data.conditionsOn(k)
    Column {
        if (sr != null) {
            ItemRow(onClick = { onOpenSleep(k) }, desc = "睡眠 ${durJ(sr.nightMin)}") {
                EntryIcon(AppIcons.Moon, box = 30.dp)
                Column(Modifier.weight(1f)) {
                    Text(durJ(sr.nightMin), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (sr.napMin > 0) Text("二度寝 ${durJ(sr.napMin)}", fontSize = 13.sp, color = c.muted)
                }
                Chevron()
            }
        }
        if (conds.size == 1) {
            val r = conds[0]
            ItemRow(onClick = { onOpenCondition(k, r.id) }, desc = condDesc(r)) {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) { MiniMind(r.mind?.toDouble(), size = 24.dp) }
                Column(Modifier.weight(1f)) {
                    MiniBattery(r.body?.toDouble())
                    if (r.note.isNotEmpty()) Text(r.note, fontSize = 14.sp)
                }
                Text("1件", fontSize = 13.sp, color = c.muted)
                Chevron()
            }
        } else if (conds.size > 1) {
            var open by remember(k) { mutableStateOf(false) }
            val notes = conds.map { it.note }.filter { it.isNotEmpty() }.joinToString(" / ")
            ItemRow(onClick = { open = !open }, desc = "調子 ${conds.size}件(押すと開く)") {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) { MiniMind(conds.avgMind(), size = 24.dp) }
                Column(Modifier.weight(1f)) {
                    MiniBattery(conds.avgBody())
                    if (!open && notes.isNotEmpty()) Text(notes, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${conds.size}件", fontSize = 13.sp, color = c.muted)
                Box(Modifier.rotate(if (open) 90f else 0f)) { Chevron() }
            }
            if (open) {
                conds.forEach { r ->
                    ItemRow(onClick = { onOpenCondition(k, r.id) }, desc = condDesc(r), indent = true) {
                        Text(hm(r.timeMin), fontSize = 13.sp, color = c.muted)
                        MiniMind(r.mind?.toDouble())
                        MiniBattery(r.body?.toDouble())
                        Text(r.note, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Chevron()
                    }
                }
            }
        }
    }
}

private fun condDesc(r: ConditionRecord) = "調子 ${hm(r.timeMin)} ココロ${r.mind ?: "-"} カラダ${r.body ?: "-"} ${r.note}"

@Composable
private fun ItemRow(onClick: () -> Unit, desc: String, indent: Boolean = false, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .padding(start = if (indent) 40.dp else 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
