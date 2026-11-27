package app.shinsatsunote.domain

import app.shinsatsunote.data.Appointment
import app.shinsatsunote.data.ConditionRecord
import app.shinsatsunote.data.SleepRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// 日付・時間の書式と、記録からの計算。画面にもAIに渡す文にも使うので、Android に頼らない形で置く

private const val WEEKDAYS = "月火水木金土日"

fun LocalDate.key(): String = toString()
fun parseDate(key: String): LocalDate = LocalDate.parse(key)

/** 曜日1文字(日曜は「日」) */
fun LocalDate.weekdayJ(): Char = WEEKDAYS[dayOfWeek.value - 1]

/** 11/13 */
fun LocalDate.md(): String = "$monthValue/$dayOfMonth"

/** 11/13(金) */
fun LocalDate.mdw(): String = "${md()}(${weekdayJ()})"

/** 0:00 からの分 → 07:05 */
fun hm(min: Int): String {
    val m = ((min % 1440) + 1440) % 1440
    return "%02d:%02d".format(m / 60, m % 60)
}

/** 分 → 6時間6分 */
fun durJ(min: Int): String = "${min / 60}時間${min % 60}分"

/** 分 → 6h06(週の行の平均睡眠) */
fun durShort(min: Int): String = "${min / 60}h%02d".format(min % 60)

/** a から b まで(日をまたいでもよい)の分 */
fun diffMin(a: Int, b: Int): Int = ((b - a) % 1440 + 1440) % 1440

val SleepRecord.nightMin: Int get() = diffMin(sleepMin, wakeMin)
val SleepRecord.napMin: Int
    get() = if (napSleepMin != null && napWakeMin != null) diffMin(napSleepMin, napWakeMin) else 0
val SleepRecord.totalMin: Int get() = nightMin + napMin

/** その日のココロ(またはカラダ)の平均。記録が無ければ null */
fun List<ConditionRecord>.avgOf(field: (ConditionRecord) -> Int?): Double? =
    mapNotNull(field).takeIf { it.isNotEmpty() }?.average()

fun List<ConditionRecord>.avgMind(): Double? = avgOf { it.mind }
fun List<ConditionRecord>.avgBody(): Double? = avgOf { it.body }

/** 平均を1〜5に丸める */
fun Double.level(): Int = roundToInt().coerceIn(1, 5)

private val LEVEL_WORDS = listOf("とてもつらい", "つらい", "ふつう", "少し楽", "楽")

/** ココロ・カラダの段階(1〜5)を言葉にする */
fun levelWord(level: Int): String = LEVEL_WORDS[(level.coerceIn(1, 5)) - 1]

/** 期間 [from, to] の日付(古い順) */
fun daysBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
    if (from > to) return emptyList()
    return (0..ChronoUnit.DAYS.between(from, to)).map { from.plusDays(it) }
}

/** 直近30日(今日を含む) */
fun last30(today: LocalDate): List<LocalDate> = daysBetween(today.minusDays(29), today)

/**
 * 前回の記録の翌日から今日まで。前回が無ければ直近30日。
 * 前回が今日なら今日だけ(グラフが空にならないように)
 */
fun daysSince(lastVisitDate: String?, today: LocalDate): List<LocalDate> {
    if (lastVisitDate == null) return last30(today)
    return daysBetween(parseDate(lastVisitDate).plusDays(1), today).ifEmpty { listOf(today) }
}

/** その月の日付(1日〜末日) */
fun monthDays(year: Int, month: Int): List<LocalDate> {
    val first = LocalDate.of(year, month, 1)
    return (0 until first.lengthOfMonth()).map { first.plusDays(it.toLong()) }
}

/** その週の月曜 */
fun LocalDate.monday(): LocalDate = minusDays((dayOfWeek.value - 1).toLong())

/** 予定まであと何日(当日 0、過ぎていれば負) */
fun Appointment.daysLeft(today: LocalDate): Long = ChronoUnit.DAYS.between(today, parseDate(date))

/** あと12日 / 今日 / 記録待ち */
fun Appointment.leftText(today: LocalDate): String {
    val left = daysLeft(today)
    return when {
        left > 0 -> "あと${left}日"
        left == 0L -> "今日"
        else -> "記録待ち"
    }
}

/**
 * 記録タブの通知バナーに出す相手。予定日当日か、過ぎて未記録の予定があり、
 * 選んでいる日が「今日」か「その予定日」のときだけ出す
 */
fun dueKinds(
    selected: LocalDate,
    today: LocalDate,
    kinds: List<String>,
    appointments: Map<String, Appointment>,
): List<String> = kinds.filter { k ->
    val a = appointments[k] ?: return@filter false
    a.daysLeft(today) <= 0 && (selected == today || selected.key() == a.date)
}
