package app.shinsatsunote.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.shinsatsunote.data.AppData
import app.shinsatsunote.domain.hm
import app.shinsatsunote.domain.mdw
import app.shinsatsunote.domain.parseDate
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppCard
import app.shinsatsunote.ui.components.AppIconButton
import app.shinsatsunote.ui.components.AppSheet
import app.shinsatsunote.ui.components.NoteField
import app.shinsatsunote.ui.components.PrimaryButton
import app.shinsatsunote.ui.components.SheetActions
import app.shinsatsunote.ui.components.SubButton
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme
import app.shinsatsunote.ui.theme.Ink
import java.time.LocalTime

private val WORDS = listOf("とてもつらい", "つらい", "ふつう", "少し楽", "楽")

/** 調子の画面(2ページ)。id があればその記録を直す */
@Composable
fun ConditionScreen(vm: AppViewModel, data: AppData, date: String, id: Long?, onBack: () -> Unit) {
    val editing = id?.let { i -> data.conditionsOn(date).firstOrNull { it.id == i } }
    var page by rememberSaveable { mutableStateOf(1) }
    var mind by rememberSaveable { mutableStateOf(editing?.mind) }
    var body by rememberSaveable { mutableStateOf(editing?.body) }
    var note by rememberSaveable { mutableStateOf(editing?.note ?: "") }
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    val timeMin = rememberSaveable { editing?.timeMin ?: LocalTime.now().let { it.hour * 60 + it.minute } }

    BackHandler(enabled = page == 2) { page = 1 }

    ScreenColumn {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIconButton(AppIcons.Left, if (page == 2) "ココロとカラダへ戻る" else "記録へ戻る", { if (page == 2) page = 1 else onBack() })
            Row(
                Modifier.weight(1f).semantics { contentDescription = "手順 $page / 2" },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                listOf(1, 2).forEach { p ->
                    Box(
                        Modifier.width(24.dp).height(4.dp).clip(RoundedCornerShape(999.dp))
                            .background(if (p == page) AppTheme.colors.accent else AppTheme.colors.line),
                    )
                }
            }
            if (editing != null) {
                AppIconButton(AppIcons.Trash, "この調子の記録を削除", { vm.deleteCondition(editing); onBack() })
            }
            AppIconButton(AppIcons.Help, "調子の見方", { helpOpen = true })
        }
        Text("${parseDate(date).mdw()} ${hm(timeMin)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

        if (page == 1) {
            AppCard {
                Text("ココロ", style = MaterialTheme.typography.titleSmall)
                MindTrack(mind) { mind = it }
            }
            AppCard {
                Text("カラダ", style = MaterialTheme.typography.titleSmall)
                BigBattery(body) { body = it }
            }
            PrimaryButton("次へ", { page = 2 }, Modifier.fillMaxWidth(), icon = AppIcons.Right, iconEnd = true)
        } else {
            AppCard {
                Text("ひとこと（任意）", style = MaterialTheme.typography.titleSmall)
                NoteField(note, { note = it }, label = "ひとこと", placeholder = "例：昨日はおそくまで起きてしまった。")
            }
            PrimaryButton(
                "保存する",
                {
                    vm.saveCondition(editing, date, timeMin, mind, body, note)
                    onBack()
                },
                Modifier.fillMaxWidth(),
                icon = AppIcons.Check,
            )
        }
    }

    if (helpOpen) {
        AppSheet("調子の見方", { helpOpen = false }) {
            Text("ココロ（顔）とカラダ（電池）は、どちらも左ほどつらく、右ほど楽な5段階です。", style = MaterialTheme.typography.bodyMedium)
            Text("どちらか片方だけ選んだり、ひとことだけ書いて保存することもできます。", style = MaterialTheme.typography.bodyMedium)
            SheetActions { SubButton("閉じる", { helpOpen = false }) }
        }
    }
}

/** ココロ: 5つの顔。選んだ位置まで塗り、表情と色をそろえる(.track) */
@Composable
private fun MindTrack(value: Int?, onSelect: (Int) -> Unit) {
    val c = AppTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
        val inner = maxWidth - 48.dp
        // 下の線と、選んだ位置までの色の帯
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)).background(c.line))
        if (value != null && value > 1) {
            val colors = c.minds.take(maxOf(value, 2)).map { it.copy(alpha = 0.65f) }
            Box(
                Modifier.offset(x = 24.dp).width(inner * ((value - 1) / 4f)).height(8.dp)
                    .clip(RoundedCornerShape(999.dp)).background(Brush.horizontalGradient(colors)),
            )
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (v in 1..5) {
                val on = value != null && v <= value
                val selected = v == value
                Box(
                    Modifier
                        .size(48.dp)
                        .selectable(selected = selected, role = Role.RadioButton) { onSelect(v) }
                        .semantics { contentDescription = "ココロ $v ${WORDS[v - 1]}" },
                    contentAlignment = Alignment.Center,
                ) {
                    val shown = if (on) value!! else v
                    val fill = if (on) c.mind(value!!) else c.surface
                    val stroke = if (on) fill else c.line
                    val feature = if (on) Ink else c.line
                    Canvas(Modifier.size(44.dp).scale(if (selected) 1.16f else 1f)) {
                        val s = size.width / 48f
                        val curve = listOf(-6f, -3f, 0f, 4f, 7f)[shown - 1]
                        drawCircle(fill, 20f * s, Offset(24f * s, 24f * s))
                        drawCircle(stroke, 20f * s, Offset(24f * s, 24f * s), style = Stroke(2.5f * s))
                        drawCircle(feature, 2.3f * s, Offset(18f * s, 20f * s))
                        drawCircle(feature, 2.3f * s, Offset(30f * s, 20f * s))
                        val p = Path().apply {
                            moveTo(17f * s, 31f * s)
                            quadraticTo(24f * s, (31f + curve) * s, 31f * s, 31f * s)
                        }
                        drawPath(p, feature, style = Stroke(2.5f * s, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}

/** カラダ: 5区切りの電池。軽やかな枠と残量セルで体力の度合いを示す */
@Composable
private fun BigBattery(value: Int?, onSelect: (Int) -> Unit) {
    val c = AppTheme.colors
    val fill = value?.let { c.level(it) } ?: Color.Transparent
    val frameColor = if (value != null) c.muted else c.line
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.paper)
                .border(1.8.dp, frameColor, RoundedCornerShape(14.dp))
                .padding(5.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            for (v in 1..5) {
                val on = value != null && v <= value
                val shape = RoundedCornerShape(7.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(if (on) fill.copy(alpha = 0.85f) else c.surface)
                        .border(
                            width = if (v == value) 1.5.dp else 1.dp,
                            color = when {
                                v == value -> c.ink.copy(alpha = 0.55f)
                                on -> Color.Transparent
                                else -> c.line.copy(alpha = 0.8f)
                            },
                            shape = shape,
                        )
                        .selectable(selected = v == value, role = Role.RadioButton) { onSelect(v) }
                        .semantics {
                            contentDescription = "カラダ $v ${WORDS[v - 1]}"
                            stateDescription = if (v == value) "選択中" else ""
                        },
                )
            }
        }
        Box(
            Modifier
                .width(7.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
                .background(frameColor),
        )
    }
}
