package app.shinsatsunote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.domain.diffMin
import app.shinsatsunote.domain.durJ
import app.shinsatsunote.domain.hm
import app.shinsatsunote.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MINUTE_STEP = 5
private val MINUTES = List(60 / MINUTE_STEP) { it * MINUTE_STEP }
private val RowHeight = 44.dp

/** 時刻を選ぶ1つ分。bedLayout なら時の列を12時から並べる(22・23・0・1時が続いて並ぶ) */
data class TimeSlot(val label: String, val minutes: Int, val bedLayout: Boolean = false)

/**
 * 5分きざみの時刻を選ぶシート。slots が2つならタブで切り替え、showDuration なら2つの間の長さを出す。
 * OK で onConfirm に選んだ分(slots と同じ順)を返す
 */
@Composable
fun TimeWheelSheet(
    title: String,
    slots: List<TimeSlot>,
    initialIndex: Int = 0,
    showDuration: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
) {
    val values = remember { mutableStateListOf(*slots.map { it.minutes }.toTypedArray()) }
    var which by rememberSaveable { mutableIntStateOf(initialIndex) }
    val c = AppTheme.colors

    AppSheet(title, onDismiss) {
        if (slots.size > 1) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(c.seg).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                slots.forEachIndexed { i, s ->
                    val on = i == which
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on) c.surface else c.seg)
                            .clickable(role = Role.Tab) { which = i }
                            .semantics { selected = on }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(s.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) c.ink else c.muted)
                        Text(hm(values[i]), fontSize = 13.sp, color = if (on) c.ink else c.muted)
                    }
                }
            }
        }

        val current = values[which]
        val slot = slots[which]
        val hours = if (slot.bedLayout) List(24) { (it + 12) % 24 } else List(24) { it }
        fun set(minutes: Int) {
            values[which] = minutes
        }
        // タブを切り替えたら列を作り直し、そのタブの時刻に合わせる
        key(which) {
            Box(Modifier.fillMaxWidth().height(RowHeight * 5), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxWidth(0.76f).height(RowHeight)
                        .clip(RoundedCornerShape(12.dp)).background(c.accentSoft.copy(alpha = 0.6f)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    WheelColumn(
                        labels = hours.map { "%02d".format(it) },
                        initialIndex = hours.indexOf(current / 60),
                        description = "${slot.label}の時",
                        onSelect = { i -> if (hours[i] != values[which] / 60) set(hours[i] * 60 + values[which] % 60) },
                    )
                    Text(":", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    WheelColumn(
                        labels = MINUTES.map { "%02d".format(it) },
                        initialIndex = (current % 60) / MINUTE_STEP,
                        description = "${slot.label}の分",
                        onSelect = { i ->
                            val now = values[which]
                            if (i != (now % 60) / MINUTE_STEP) set(now / 60 * 60 + MINUTES[i])
                        },
                    )
                }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0f to c.surface, 0.32f to c.surface.copy(alpha = 0f),
                            0.68f to c.surface.copy(alpha = 0f), 1f to c.surface,
                        ),
                    ),
                )
            }
        }
        if (showDuration && values.size == 2) {
            Text(
                "長さ ${durJ(diffMin(values[0], values[1]))}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
            )
        }
        PrimaryButton("OK", { onConfirm(values.toList()) }, Modifier.fillMaxWidth())
    }
}

/**
 * 上下にスクロールして選ぶ列。真ん中の行が選択中。
 * スクロールが止まると、いちばん近い行にぴたりと合わせる。上下の行をタップすると、その向きへ1段動かす。
 */
@Composable
private fun WheelColumn(labels: List<String>, initialIndex: Int, description: String, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceAtLeast(0))
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    var ready by remember { mutableStateOf(false) }
    val c = AppTheme.colors

    val centerIndex by remember { derivedStateOf { listState.centerItem()?.index ?: initialIndex.coerceAtLeast(0) } }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        listState.distanceToCenter(initialIndex)?.let { listState.scrollBy(it) }
        ready = true
    }
    LaunchedEffect(centerIndex, ready) {
        if (ready) select(centerIndex)
    }
    LaunchedEffect(listState.isScrollInProgress, ready) {
        if (ready && !listState.isScrollInProgress) {
            val target = listState.centerItem()?.index ?: return@LaunchedEffect
            val d = listState.distanceToCenter(target) ?: return@LaunchedEffect
            if (abs(d) > 1f) listState.animateScrollBy(d)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.width(88.dp).height(RowHeight * 5).semantics { contentDescription = description },
        contentPadding = PaddingValues(vertical = RowHeight * 2),
    ) {
        itemsIndexed(labels) { i, label ->
            val on = i == centerIndex
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(RowHeight)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        val target = centerIndex + (i - centerIndex).coerceIn(-1, 1)
                        scope.launch { listState.distanceToCenter(target)?.let { listState.animateScrollBy(it) } }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = if (on) 24.sp else 20.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) c.ink else c.muted,
                )
            }
        }
    }
}

private fun LazyListState.centerItem() = layoutInfo.let { info ->
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }
}

private fun LazyListState.distanceToCenter(index: Int): Float? = layoutInfo.let { info ->
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    item.offset + item.size / 2f - center
}
