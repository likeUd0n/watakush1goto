package app.shinsatsunote.domain

import app.shinsatsunote.data.Appointment
import app.shinsatsunote.data.Kind
import app.shinsatsunote.data.ScreenEvent
import app.shinsatsunote.data.SleepRecord
import app.shinsatsunote.data.NightGuess
import app.shinsatsunote.data.guessNight
import app.shinsatsunote.data.roundToStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    private val today = LocalDate.of(2026, 11, 13)

    @Test
    fun formats() {
        assertEquals("11/13(金)", today.mdw())
        assertEquals("11/15(日)", today.plusDays(2).mdw())
        assertEquals("07:05", hm(425))
        assertEquals("00:00", hm(1440))
        assertEquals("6時間6分", durJ(366))
        assertEquals("6h06", durShort(366))
        assertEquals("とてもつらい", levelWord(1))
        assertEquals("ふつう", levelWord(3))
        assertEquals("楽", levelWord(5))
    }

    @Test
    fun sleepAcrossMidnight() {
        val r = SleepRecord("2026-11-13", sleepMin = 23 * 60 + 30, wakeMin = 7 * 60, napSleepMin = 13 * 60, napWakeMin = 14 * 60 + 30)
        assertEquals(450, r.nightMin)
        assertEquals(90, r.napMin)
        assertEquals(540, r.totalMin)
        assertEquals(0, r.copy(napWakeMin = null).napMin)
    }

    @Test
    fun periodSinceLastVisit() {
        assertEquals(30, daysSince(null, today).size)
        assertEquals(today.minusDays(29), daysSince(null, today).first())
        val since = daysSince("2026-11-10", today)
        assertEquals(listOf("2026-11-11", "2026-11-12", "2026-11-13"), since.map { it.key() })
        // 前回が今日なら今日だけ
        assertEquals(listOf(today), daysSince("2026-11-13", today))
    }

    @Test
    fun dueBannerOnlyOnTodayOrThatDay() {
        val appts = mapOf(
            Kind.DOC to Appointment(Kind.DOC, "2026-11-10", 630),
            Kind.COUN to Appointment(Kind.COUN, "2026-11-20", null),
        )
        val kinds = listOf(Kind.DOC, Kind.COUN)
        assertEquals(listOf(Kind.DOC), dueKinds(today, today, kinds, appts))
        assertEquals(listOf(Kind.DOC), dueKinds(LocalDate.of(2026, 11, 10), today, kinds, appts))
        assertTrue(dueKinds(LocalDate.of(2026, 11, 11), today, kinds, appts).isEmpty())
        assertTrue(dueKinds(today, today, listOf(Kind.COUN), appts).isEmpty())
        assertEquals("記録待ち", appts.getValue(Kind.DOC).leftText(today))
        assertEquals("あと7日", appts.getValue(Kind.COUN).leftText(today))
    }

    @Test
    fun weeksStartOnMonday() {
        assertEquals(LocalDate.of(2026, 11, 9), today.monday())
        assertEquals(30, monthDays(2026, 11).size)
    }

    @Test
    fun crisisWords() {
        assertTrue(looksLikeCrisis("もう しにたい"))
        assertTrue(looksLikeCrisis("消えたくなる"))
        assertFalse(looksLikeCrisis("散歩できた"))
    }

    @Test
    fun sleepGuessPicksLongestScreenOff() {
        val h = 3_600_000L
        val events = listOf(ScreenEvent(0, on = false), ScreenEvent(h, on = true), ScreenEvent(2 * h, on = false), ScreenEvent(8 * h, on = true))
        assertEquals(NightGuess(2 * h, 8 * h), guessNight(events, 9 * h))
        // まだ画面がついていなければ、起きた時刻は now
        assertEquals(NightGuess(2 * h, 9 * h), guessNight(events.dropLast(1), 9 * h))
        assertNull(guessNight(listOf(ScreenEvent(0, false), ScreenEvent(h, true)), 2 * h))
        assertEquals(5, roundToStep(3))
    }
}
