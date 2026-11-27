package app.shinsatsunote.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.Instant
import java.time.ZoneId

/**
 * 睡眠記録を開いたときに入れておく時刻の推測。
 * 寝た時刻は、端末の「使用状況へのアクセス」で読める画面のオン・オフから、さかのぼって [GUESS_LOOKBACK_MILLIS] のうち、
 * いちばん長い画面オフ([MIN_SLEEP_MILLIS] 以上)の始まりにする。起きた時刻は、その画面オフの終わり(つぎに画面がついた時刻)。どちらも5分単位に丸める。
 * 許可が無い・記録が無いときは null(呼ぶ側で前回の時刻を使う)。
 * 二度寝・昼寝の始めは、さかのぼって [NAP_LOOKBACK_MILLIS] のうち、いちばん新しい画面オフ([MIN_NAP_MILLIS] 以上)の始まりにする。
 */
private const val GUESS_LOOKBACK_MILLIS = 16 * 60 * 60 * 1000L
private const val MIN_SLEEP_MILLIS = 3 * 60 * 60 * 1000L
private const val NAP_LOOKBACK_MILLIS = 6 * 60 * 60 * 1000L   // 仮置き
private const val MIN_NAP_MILLIS = 20 * 60 * 1000L            // 仮置き
private const val STEP = 5

/** 画面のオン・オフ1回分 */
data class ScreenEvent(val at: Long, val on: Boolean)

/** 0:00からの分を、いちばん近い5分に丸める(24:00 は 0:00 にする) */
fun roundToStep(minutes: Int): Int = (Math.round(minutes / STEP.toFloat()) * STEP) % (24 * 60)

/** epoch millis を、その日の0:00からの分(5分単位)にする */
fun clockMinutes(at: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
    val t = Instant.ofEpochMilli(at).atZone(zone).toLocalTime()
    return roundToStep(t.hour * 60 + t.minute)
}

/** 推測した夜の睡眠(epoch millis) */
data class NightGuess(val bed: Long, val wake: Long)

/**
 * 画面の出来事(古い順でなくてよい)から、寝た時刻と起きた時刻(epoch millis)を推測する。
 * オフからつぎのオン(無ければ now)までを1回の画面オフとし、いちばん長いものの始まりと終わりを返す。
 * [MIN_SLEEP_MILLIS] より短いものしか無ければ null
 */
fun guessNight(events: List<ScreenEvent>, now: Long): NightGuess? {
    var bestStart: Long? = null
    var bestEnd = 0L
    var bestLength = 0L
    var offAt: Long? = null
    fun close(end: Long) {
        val start = offAt ?: return
        if (end - start > bestLength) {
            bestStart = start
            bestEnd = end
            bestLength = end - start
        }
        offAt = null
    }
    for (e in events.sortedBy { it.at }) {
        if (!e.on) {
            if (offAt == null) offAt = e.at
        } else {
            close(e.at)
        }
    }
    close(now)
    val start = bestStart ?: return null
    return if (bestLength >= MIN_SLEEP_MILLIS) NightGuess(start, bestEnd) else null
}

/**
 * 画面の出来事(古い順でなくてよい)から、二度寝・昼寝の始め(epoch millis)を推測する。
 * オフからつぎのオン(無ければ now)までを1回の画面オフとし、[MIN_NAP_MILLIS] 以上のうち、いちばん新しいものの始まりを返す。
 * 無ければ null
 */
fun guessNapStart(events: List<ScreenEvent>, now: Long): Long? {
    var latest: Long? = null
    var offAt: Long? = null
    fun close(end: Long) {
        val start = offAt ?: return
        if (end - start >= MIN_NAP_MILLIS) latest = start
        offAt = null
    }
    for (e in events.sortedBy { it.at }) {
        if (!e.on) {
            if (offAt == null) offAt = e.at
        } else {
            close(e.at)
        }
    }
    close(now)
    return latest
}

/** 「使用状況へのアクセス」を許可しているか */
fun hasUsageAccess(context: Context): Boolean {
    val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/** 寝た時刻と起きた時刻(0:00からの分)を推測する。許可が無い・Android 9 より前・記録が足りないときは null */
fun guessNightMinutes(context: Context, now: Long = System.currentTimeMillis()): Pair<Int, Int>? =
    screenEvents(context, now - GUESS_LOOKBACK_MILLIS, now)?.let { guessNight(it, now) }
        ?.let { clockMinutes(it.bed) to clockMinutes(it.wake) }

/** 二度寝・昼寝の始め(0:00からの分)を推測する。許可が無い・Android 9 より前・記録が足りないときは null */
fun guessNapStartMinutes(context: Context, now: Long = System.currentTimeMillis()): Int? =
    screenEvents(context, now - NAP_LOOKBACK_MILLIS, now)?.let { guessNapStart(it, now) }?.let { clockMinutes(it) }

/** 画面のオン・オフの記録。許可が無い・Android 9 より前は null */
private fun screenEvents(context: Context, from: Long, now: Long): List<ScreenEvent>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasUsageAccess(context)) return null
    val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
    val list = mutableListOf<ScreenEvent>()
    val events = usm.queryEvents(from, now)
    val e = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(e)
        when (e.eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> list += ScreenEvent(e.timeStamp, on = true)
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> list += ScreenEvent(e.timeStamp, on = false)
        }
    }
    return list
}
