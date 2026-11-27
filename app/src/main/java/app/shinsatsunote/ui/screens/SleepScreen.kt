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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.data.AppData
import app.shinsatsunote.data.SleepRecord
import app.shinsatsunote.data.clockMinutes
import app.shinsatsunote.data.guessNightMinutes
import app.shinsatsunote.data.guessNapStartMinutes
import app.shinsatsunote.domain.hm
import app.shinsatsunote.domain.md
import app.shinsatsunote.domain.mdw
import app.shinsatsunote.domain.parseDate
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppCard
import app.shinsatsunote.ui.components.AppIconButton
import app.shinsatsunote.ui.components.PrimaryButton
import app.shinsatsunote.ui.components.SubTopRow
import app.shinsatsunote.ui.components.TimeSlot
import app.shinsatsunote.ui.components.TimeWheelSheet
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme

/** 未記録の日の初期値。推定も前回も無いときに使う */
private const val DEFAULT_BED = 40
private const val DEFAULT_NAP_START = 13 * 60
private const val DEFAULT_NAP_END = 14 * 60 + 30

/** 睡眠の画面。日付は起きた日(「前日の夜 → 当日の朝」) */
@Composable
fun SleepScreen(vm: AppViewModel, data: AppData, date: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val existing = data.sleeps[date]
    var bed by rememberSaveable { mutableStateOf<Int?>(null) }
    var wake by rememberSaveable { mutableStateOf(0) }
    var napOn by rememberSaveable { mutableStateOf(false) }
    var napS by rememberSaveable { mutableStateOf(DEFAULT_NAP_START) }
    var napW by rememberSaveable { mutableStateOf(DEFAULT_NAP_END) }
    var guessed by rememberSaveable { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }

    // 初めて開いたときだけ初期値を決める(記録があればそれ、無ければ推定 → 前回 → 既定)
    LaunchedEffect(date) {
        if (bed != null) return@LaunchedEffect
        if (existing != null) {
            bed = existing.sleepMin
            wake = existing.wakeMin
            napOn = existing.napSleepMin != null && existing.napWakeMin != null
            existing.napSleepMin?.let { napS = it }
            existing.napWakeMin?.let { napW = it }
        } else {
            val now = System.currentTimeMillis()
            val g = guessNightMinutes(context, now)
            val last = vm.latestSleep()
            guessed = g != null
            bed = g?.first ?: last?.sleepMin ?: DEFAULT_BED
            wake = g?.second ?: last?.wakeMin ?: clockMinutes(now)
            guessNapStartMinutes(context, now)?.let { napS = it; napW = clockMinutes(now) }
        }
    }

    val day = parseDate(date)
    val c = AppTheme.colors
    ScreenColumn {
        SubTopRow(title = "睡眠", backLabel = "記録へ戻る", onBack = onBack) {
            if (existing != null) {
                AppIconButton(AppIcons.Trash, "この日の睡眠の記録を削除", { vm.deleteSleep(date); onBack() })
            }
        }
        Text("${day.minusDays(1).md()}の夜 → ${day.mdw()}の朝", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

        val b = bed ?: return@ScreenColumn
        AppCard(Modifier.semantics { contentDescription = "夜の睡眠" }) {
            TimePair(b, wake) { picker = "night" }
            if (guessed && existing == null) {
                Text("寝た・起きた時刻は、画面がオフだった時間からの推定です", style = MaterialTheme.typography.bodySmall)
            }
            if (!napOn) {
                Surface(
                    onClick = { napOn = true },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, c.line),
                    color = c.surface,
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(
                        "＋ 二度寝・昼寝",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontSize = 13.sp,
                        color = c.muted,
                    )
                }
            }
        }
        if (napOn) {
            AppCard(Modifier.semantics { contentDescription = "二度寝・昼寝" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("二度寝・昼寝", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    AppIconButton(AppIcons.Close, "二度寝・昼寝を外す", { napOn = false })
                }
                TimePair(napS, napW) { picker = "nap" }
            }
        }
        PrimaryButton(
            "保存する",
            {
                vm.saveSleep(
                    SleepRecord(
                        date = date,
                        sleepMin = b,
                        wakeMin = wake,
                        napSleepMin = if (napOn) napS else null,
                        napWakeMin = if (napOn) napW else null,
                    ),
                )
                onBack()
            },
            Modifier.fillMaxWidth(),
            icon = AppIcons.Check,
        )
    }

    when (picker) {
        "night" -> TimeWheelSheet(
            title = "夜の睡眠",
            slots = listOf(TimeSlot("寝た", bed ?: DEFAULT_BED, bedLayout = true), TimeSlot("起きた", wake)),
            showDuration = true,
            onDismiss = { picker = null },
            onConfirm = { bed = it[0]; wake = it[1]; guessed = false; picker = null },
        )
        "nap" -> TimeWheelSheet(
            title = "二度寝・昼寝",
            slots = listOf(TimeSlot("寝た", napS), TimeSlot("起きた", napW)),
            showDuration = true,
            onDismiss = { picker = null },
            onConfirm = { napS = it[0]; napW = it[1]; picker = null },
        )
    }
}

/** 寝た・起きたの2つの時刻(.times)。押すと時刻のシートを開く */
@Composable
private fun TimePair(start: Int, end: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TimeCell("寝た", start, Modifier.weight(1f), onClick)
        TimeCell("起きた", end, Modifier.weight(1f), onClick)
    }
}

@Composable
private fun TimeCell(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(
        modifier
            .clickable(onClickLabel = "${label}時刻を変える", onClick = onClick)
            .semantics { contentDescription = "$label ${hm(minutes)}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 13.sp, color = c.muted, modifier = Modifier.padding(top = 6.dp))
        Text(hm(minutes), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.heightIn(min = 40.dp).padding(top = 4.dp))
        HorizontalDivider(color = c.line)
    }
}
