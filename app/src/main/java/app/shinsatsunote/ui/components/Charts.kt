package app.shinsatsunote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.data.AppData
import app.shinsatsunote.domain.avgBody
import app.shinsatsunote.domain.avgMind
import app.shinsatsunote.domain.durJ
import app.shinsatsunote.domain.key
import app.shinsatsunote.domain.level
import app.shinsatsunote.domain.levelWord
import app.shinsatsunote.domain.md
import app.shinsatsunote.domain.napMin
import app.shinsatsunote.domain.nightMin
import app.shinsatsunote.domain.totalMin
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppColors
import app.shinsatsunote.ui.theme.AppTheme
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor

// モックの renderTrendCharts を写したもの。座標は SVG と同じ 320 幅で考え、画面の幅に合わせて広げる

private const val W = 320f
private const val L = 28f
private const val R = 6f
private const val T = 10f
private const val H2 = 94f
private const val B2 = 20f

/** 期間の平均(平均睡眠・平均ココロ・平均カラダ) */
data class PeriodAverage(val sleepMin: Int?, val mind: Double?, val body: Double?)

fun periodAverage(days: List<LocalDate>, data: AppData): PeriodAverage {
    val sleeps = days.mapNotNull { d -> data.sleeps[d.key()]?.totalMin?.takeIf { it > 0 } }
    val minds = days.mapNotNull { data.conditionsOn(it.key()).avgMind() }
    val bodies = days.mapNotNull { data.conditionsOn(it.key()).avgBody() }
    return PeriodAverage(
        sleeps.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        minds.takeIf { it.isNotEmpty() }?.average(),
        bodies.takeIf { it.isNotEmpty() }?.average(),
    )
}

/** 平均の3つ並び(.sum-row) */
@Composable
fun SummaryRow(avg: PeriodAverage) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        SumItem("平均睡眠", Modifier.weight(1f)) {
            Icon(AppIcons.Moon, null, Modifier.size(16.dp), tint = c.accent)
            SumValue(avg.sleepMin?.let { durJ(it) } ?: "-")
        }
        SumItem("ココロ", Modifier.weight(1f)) {
            MiniMind(avg.mind)
            avg.mind?.let { SumValue(levelWord(it.level())) }
        }
        SumItem("カラダ", Modifier.weight(1f)) {
            MiniBattery(avg.body)
            avg.body?.let { SumValue(levelWord(it.level())) }
        }
    }
    HorizontalDivider(color = c.line)
}

/** 平均の値。文字を大きくしても途中で折り返さないよう、1行に収まるまで小さくする */
@Composable
private fun SumValue(text: String) {
    BasicText(
        text,
        style = TextStyle(fontWeight = FontWeight.Bold, color = AppTheme.colors.ink),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 15.sp),
    )
}

@Composable
private fun SumItem(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 13.sp, color = AppTheme.colors.muted)
        Row(
            Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) { content() }
    }
}

/** グラフの小見出しと凡例(.card-head + .legend) */
@Composable
fun ChartHead(title: String, legend: List<Pair<Color, String>>, dashed: String? = null) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            legend.forEach { (col, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(col))
                    Spacer(Modifier.width(4.dp))
                    Text(label, fontSize = 13.sp, color = c.muted)
                }
            }
            if (dashed != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.width(14.dp).height(10.dp)) {
                        drawLine(
                            c.warm, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(dashed, fontSize = 13.sp, color = c.muted)
                }
            }
        }
    }
}

private fun x(i: Int, n: Int) = L + (i + 0.5f) * (W - L - R) / maxOf(1, n)

/** 押した位置の日。今日より先は選べない */
private fun Modifier.daySelect(days: List<LocalDate>, today: LocalDate, onSelect: ((LocalDate) -> Unit)?) =
    if (onSelect == null) this else pointerInput(days, today) {
        detectTapGestures { p ->
            val u = p.x / size.width * W
            val i = floor((u - L) / ((W - L - R) / days.size)).toInt().coerceIn(0, days.lastIndex)
            val d = days[i]
            if (d <= today) onSelect(d)
        }
    }

/** 自分で見る振り返りグラフの表示切り替え(すべて / ココロ / カラダ) */
enum class MoodFilter(val label: String) {
    ALL("すべて"),
    MIND("ココロ"),
    BODY("カラダ"),
}

/** ココロ・カラダの表示切替チップ */
@Composable
fun MoodFilterChips(current: MoodFilter, onChange: (MoodFilter) -> Unit) {
    val c = AppTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MoodFilter.entries.forEach { f ->
            val on = f == current
            val activeBg = when (f) {
                MoodFilter.BODY -> c.body.copy(alpha = 0.14f)
                else -> c.accentSoft
            }
            val activeInk = when (f) {
                MoodFilter.BODY -> c.body
                else -> c.accent
            }
            Surface(
                onClick = { onChange(f) },
                shape = RoundedCornerShape(999.dp),
                color = if (on) activeBg else c.surface,
                border = BorderStroke(1.dp, if (on) activeInk.copy(alpha = 0.45f) else c.line),
            ) {
                Text(
                    f.label,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) activeInk else c.muted,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** ココロ・カラダの折れ線(案A: 自分で見る振り返り用)。エリア塗り・カラダ破線・全点表示・フィルタ対応 */
@Composable
fun MoodChart(
    days: List<LocalDate>,
    data: AppData,
    selected: LocalDate?,
    onSelect: ((LocalDate) -> Unit)?,
    shareAxis: Boolean,
    filter: MoodFilter = MoodFilter.ALL,
    today: LocalDate = LocalDate.now(),
) {
    if (days.isEmpty()) return
    val c = AppTheme.colors
    val tm = rememberTextMeasurer()
    val b1 = if (shareAxis) 8f else 22f
    val h1 = if (shareAxis) 104f else 120f
    val minds = days.map { data.conditionsOn(it.key()).avgMind() }
    val bodies = days.map { data.conditionsOn(it.key()).avgBody() }
    val selIdx = selected?.let { days.indexOf(it) } ?: -1
    val showMind = filter == MoodFilter.ALL || filter == MoodFilter.MIND
    val showBody = filter == MoodFilter.ALL || filter == MoodFilter.BODY
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(W / h1).daySelect(days, today, onSelect)
            .semantics { contentDescription = "ココロとカラダの推移(${days.first().md()}から${days.last().md()})" },
    ) {
        val s = size.width / W
        val bottomY = (h1 - b1) * s
        fun y(v: Double) = (T + (5 - v.toFloat()) * (h1 - T - b1) / 4) * s
        drawMoodGrid(c, s, ::y)
        if (selIdx >= 0) selLine(c, x(selIdx, days.size) * s, (T - 4) * s, bottomY)
        miniFace(c, 5, y(5.0), s)
        miniFace(c, 1, y(1.0), s)
        if (showMind) {
            areaFill(minds, days.size, s, ::y, bottomY, c.accent.copy(alpha = 0.11f))
        } else if (showBody) {
            areaFill(bodies, days.size, s, ::y, bottomY, c.body.copy(alpha = 0.12f))
        }
        if (showBody) {
            val dashed = filter == MoodFilter.ALL
            linePath(bodies, days.size, s, ::y, c.body, dashed = dashed)
            drawBodyPoints(bodies, days.size, s, ::y, selIdx, c)
        }
        if (showMind) {
            linePath(minds, days.size, s, ::y, c.accent, dashed = false)
            drawMindPoints(minds, days.size, s, ::y, selIdx, c)
        }
        if (!shareAxis) axisLabels(tm, days, (h1 - 3) * s, s, c.muted)
    }
}

/** 診察「見せる」用の2段分離グラフ(案B): ココロとカラダを別枠で描き、一目で推移を把握できるようにする */
@Composable
fun SplitMoodCharts(
    days: List<LocalDate>,
    data: AppData,
    selected: LocalDate?,
    onSelect: ((LocalDate) -> Unit)?,
    today: LocalDate = LocalDate.now(),
) {
    if (days.isEmpty()) return
    val c = AppTheme.colors
    val minds = days.map { data.conditionsOn(it.key()).avgMind() }
    val bodies = days.map { data.conditionsOn(it.key()).avgBody() }
    val selIdx = selected?.let { days.indexOf(it) } ?: -1
    val hMini = 76f
    val bMini = 8f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChartHead("ココロ", listOf(c.accent to "1(つらい)〜5(よい)"))
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(W / hMini).daySelect(days, today, onSelect)
                .semantics { contentDescription = "ココロの推移(${days.first().md()}から${days.last().md()})" },
        ) {
            val s = size.width / W
            val bottomY = (hMini - bMini) * s
            fun y(v: Double) = (T + (5 - v.toFloat()) * (hMini - T - bMini) / 4) * s
            drawMoodGrid(c, s, ::y)
            if (selIdx >= 0) selLine(c, x(selIdx, days.size) * s, (T - 4) * s, bottomY)
            miniFace(c, 5, y(5.0), s)
            miniFace(c, 1, y(1.0), s)
            areaFill(minds, days.size, s, ::y, bottomY, c.accent.copy(alpha = 0.13f))
            linePath(minds, days.size, s, ::y, c.accent, dashed = false)
            drawMindPoints(minds, days.size, s, ::y, selIdx, c)
        }

        ChartHead("カラダ", listOf(c.body to "1(つらい)〜5(よい)"))
        val tm = rememberTextMeasurer()
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(W / hMini).daySelect(days, today, onSelect)
                .semantics { contentDescription = "カラダの推移(${days.first().md()}から${days.last().md()})" },
        ) {
            val s = size.width / W
            val bottomY = (hMini - bMini) * s
            fun y(v: Double) = (T + (5 - v.toFloat()) * (hMini - T - bMini) / 4) * s
            drawMoodGrid(c, s, ::y)
            if (selIdx >= 0) selLine(c, x(selIdx, days.size) * s, (T - 4) * s, bottomY)
            val labelStyle = TextStyle(fontSize = (9.5f * s).toSp(), color = c.muted)
            val hi = tm.measure("5", labelStyle)
            val lo = tm.measure("1", labelStyle)
            drawText(hi, topLeft = Offset((L - 6) * s - hi.size.width, y(5.0) - hi.size.height / 2f))
            drawText(lo, topLeft = Offset((L - 6) * s - lo.size.width, y(1.0) - lo.size.height / 2f))
            areaFill(bodies, days.size, s, ::y, bottomY, c.body.copy(alpha = 0.13f))
            linePath(bodies, days.size, s, ::y, c.body, dashed = false)
            drawBodyPoints(bodies, days.size, s, ::y, selIdx, c)
        }
    }
}

private fun DrawScope.drawMoodGrid(c: AppColors, s: Float, y: (Double) -> Float) {
    for (v in 1..5) {
        if (v == 3) {
            drawLine(
                c.muted.copy(alpha = 0.32f),
                Offset(L * s, y(3.0)),
                Offset((W - R) * s, y(3.0)),
                1.2f * s,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * s, 4f * s)),
            )
        } else {
            drawLine(c.line.copy(alpha = 0.85f), Offset(L * s, y(v.toDouble())), Offset((W - R) * s, y(v.toDouble())), 1f)
        }
    }
}

private fun DrawScope.drawMindPoints(
    values: List<Double?>,
    n: Int,
    s: Float,
    y: (Double) -> Float,
    selIdx: Int,
    c: AppColors,
) {
    values.forEachIndexed { i, v ->
        if (v == null) return@forEachIndexed
        val center = Offset(x(i, n) * s, y(v))
        if (i == selIdx) {
            drawCircle(c.surface, 5.7f * s, center)
            drawCircle(c.warm, 4.2f * s, center)
        } else {
            drawCircle(c.surface, 3.6f * s, center)
            drawCircle(c.accent, 2.5f * s, center)
        }
    }
}

private fun DrawScope.drawBodyPoints(
    values: List<Double?>,
    n: Int,
    s: Float,
    y: (Double) -> Float,
    selIdx: Int,
    c: AppColors,
) {
    values.forEachIndexed { i, v ->
        if (v == null) return@forEachIndexed
        val center = Offset(x(i, n) * s, y(v))
        val half = if (i == selIdx) 4.2f * s else 2.5f * s
        val pad = 1.2f * s
        drawRect(
            c.surface,
            topLeft = Offset(center.x - half - pad, center.y - half - pad),
            size = Size((half + pad) * 2, (half + pad) * 2),
        )
        drawRect(
            if (i == selIdx) c.warm else c.body,
            topLeft = Offset(center.x - half, center.y - half),
            size = Size(half * 2, half * 2),
        )
    }
}

/** 睡眠の棒(夜＋二度寝)。showAvg なら平均の点線 */
@Composable
fun SleepChart(
    days: List<LocalDate>,
    data: AppData,
    selected: LocalDate?,
    onSelect: ((LocalDate) -> Unit)?,
    showAvg: Boolean,
    today: LocalDate = LocalDate.now(),
) {
    if (days.isEmpty()) return
    val c = AppTheme.colors
    val tm = rememberTextMeasurer()
    val selIdx = selected?.let { days.indexOf(it) } ?: -1
    val avg = periodAverage(days, data).sleepMin
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(W / H2).daySelect(days, today, onSelect)
            .semantics { contentDescription = "睡眠時間の推移" },
    ) {
        val s = size.width / W
        fun y2(min: Int) = (T + (10 - minOf(10f, min / 60f)) * (H2 - T - B2) / 10) * s
        val label = TextStyle(fontSize = (9.5f * s).toSp(), color = c.muted)
        listOf(0, 4, 8).forEach { h ->
            drawLine(c.line, Offset(L * s, y2(h * 60)), Offset((W - R) * s, y2(h * 60)), 1f)
            val r = tm.measure("${h}h", label)
            drawText(r, topLeft = Offset((L - 4) * s - r.size.width, y2(h * 60) - r.size.height / 2f))
        }
        if (selIdx >= 0) selLine(c, x(selIdx, days.size) * s, (T - 4) * s, (H2 - B2) * s)
        val bw = (W - L - R) / maxOf(1, days.size) - 2
        val barW = bw.coerceIn(4f, 7f) * s
        days.forEachIndexed { i, d ->
            val r = data.sleeps[d.key()] ?: return@forEachIndexed
            val cx = x(i, days.size) * s
            val night = r.nightMin
            val nap = r.napMin
            if (night > 0) {
                drawRoundRect(
                    c.accent.copy(alpha = 0.75f), Offset(cx - barW / 2, y2(night)),
                    Size(barW, (H2 - B2) * s - y2(night)), CornerRadius(2f * s),
                )
            }
            if (nap > 0) {
                drawRoundRect(c.nap, Offset(cx - barW / 2, y2(night + nap)), Size(barW, y2(night) - y2(night + nap)), CornerRadius(2f * s))
            }
        }
        if (showAvg && avg != null) {
            drawLine(
                c.warm, Offset(L * s, y2(avg)), Offset((W - R) * s, y2(avg)), 1.5f * s,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * s, 3f * s)),
            )
        }
        axisLabels(tm, days, (H2 - 3) * s, s, c.muted)
    }
}

private fun DrawScope.selLine(c: AppColors, x: Float, top: Float, bottom: Float) {
    drawLine(c.warm, Offset(x, top), Offset(x, bottom), 1.5f * size.width / W, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
}

/** 連続する点列を単調三次曲線でなめらかに結ぶ(1〜5の範囲をはみ出さない) */
private fun buildSegments(values: List<Double?>, n: Int, s: Float, y: (Double) -> Float): List<List<Offset>> {
    val segments = mutableListOf<MutableList<Offset>>()
    var cur: MutableList<Offset>? = null
    values.forEachIndexed { i, v ->
        if (v == null) {
            cur = null
            return@forEachIndexed
        }
        val pt = Offset(x(i, n) * s, y(v))
        if (cur == null) {
            cur = mutableListOf(pt)
            segments.add(cur!!)
        } else {
            cur!!.add(pt)
        }
    }
    return segments
}

private fun Path.addSmoothCurve(pts: List<Offset>) {
    if (pts.isEmpty()) return
    moveTo(pts[0].x, pts[0].y)
    if (pts.size == 1) return
    if (pts.size == 2) {
        lineTo(pts[1].x, pts[1].y)
        return
    }
    val m = FloatArray(pts.size - 1) { i ->
        val dx = pts[i + 1].x - pts[i].x
        if (dx == 0f) 0f else (pts[i + 1].y - pts[i].y) / dx
    }
    val t = FloatArray(pts.size)
    t[0] = m[0]
    t[pts.lastIndex] = m.last()
    for (i in 1 until pts.lastIndex) {
        val prev = m[i - 1]
        val next = m[i]
        if (prev * next <= 0f) {
            t[i] = 0f
        } else {
            val avg = (prev + next) / 2f
            val limit = 3f * minOf(abs(prev), abs(next))
            t[i] = avg.coerceIn(-limit, limit)
        }
    }
    for (i in 0 until pts.lastIndex) {
        val p0 = pts[i]
        val p1 = pts[i + 1]
        val dx = (p1.x - p0.x) / 3f
        cubicTo(
            p0.x + dx, p0.y + t[i] * dx,
            p1.x - dx, p1.y - t[i + 1] * dx,
            p1.x, p1.y,
        )
    }
}

private fun DrawScope.linePath(
    values: List<Double?>,
    n: Int,
    s: Float,
    y: (Double) -> Float,
    color: Color,
    dashed: Boolean = false,
) {
    val p = Path()
    buildSegments(values, n, s, y).forEach { p.addSmoothCurve(it) }
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5f * s, 3.5f * s)) else null
    drawPath(p, color, style = Stroke(2f * s, join = StrokeJoin.Round, pathEffect = effect))
}

private fun DrawScope.areaFill(
    values: List<Double?>,
    n: Int,
    s: Float,
    y: (Double) -> Float,
    bottomY: Float,
    color: Color,
) {
    buildSegments(values, n, s, y).forEach { pts ->
        if (pts.size < 2) return@forEach
        val p = Path()
        p.addSmoothCurve(pts)
        p.lineTo(pts.last().x, bottomY)
        p.lineTo(pts.first().x, bottomY)
        p.close()
        drawPath(p, color)
    }
}

/** 縦軸の両端の小さな顔(上が楽、下がつらい) */
private fun DrawScope.miniFace(c: AppColors, v: Int, cy: Float, s: Float) {
    val ox = 5f * s
    val oy = cy - 8f * s
    val curve = if (v == 1) -3f else 3f
    drawCircle(c.muted, 7f * s, Offset(ox + 8f * s, oy + 8f * s), style = Stroke(1.2f * s))
    drawCircle(c.muted, 1f * s, Offset(ox + 5.5f * s, oy + 6.5f * s))
    drawCircle(c.muted, 1f * s, Offset(ox + 10.5f * s, oy + 6.5f * s))
    val p = Path().apply {
        moveTo(ox + 5f * s, oy + 10.5f * s)
        quadraticTo(ox + 8f * s, oy + (10.5f + curve) * s, ox + 11f * s, oy + 10.5f * s)
    }
    drawPath(p, c.muted, style = Stroke(1.2f * s))
}

/** 下の日付の目盛り(最初・真ん中・最後) */
private fun DrawScope.axisLabels(tm: TextMeasurer, days: List<LocalDate>, baseline: Float, s: Float, color: Color) {
    val style = TextStyle(fontSize = (9.5f * s).toSp(), color = color)
    val first = tm.measure(days.first().md(), style)
    val mid = tm.measure(days[days.size / 2].md(), style)
    val last = tm.measure(days.last().md(), style)
    drawText(first, topLeft = Offset(L * s, baseline - first.size.height))
    drawText(mid, topLeft = Offset((L + W - R) / 2 * s - mid.size.width / 2f, baseline - mid.size.height))
    drawText(last, topLeft = Offset((W - R) * s - last.size.width, baseline - last.size.height))
}
