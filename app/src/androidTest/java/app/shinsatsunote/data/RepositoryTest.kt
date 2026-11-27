package app.shinsatsunote.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = Repository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun sleepIsOnePerDayAndUndoable() = runBlocking {
        repo.saveSleep(SleepRecord("2026-11-13", sleepMin = 40, wakeMin = 425))
        repo.saveSleep(SleepRecord("2026-11-13", sleepMin = 60, wakeMin = 430, napSleepMin = 780, napWakeMin = 870))
        var data = repo.data.first()
        assertEquals(1, data.sleeps.size)
        assertEquals(60, data.sleeps.getValue("2026-11-13").sleepMin)

        val old = repo.deleteSleep("2026-11-13")!!
        assertTrue(repo.data.first().sleeps.isEmpty())
        repo.restoreSleep(old)
        data = repo.data.first()
        assertEquals(870, data.sleeps.getValue("2026-11-13").napWakeMin)
        assertEquals(old, repo.latestSleep())
    }

    @Test
    fun conditionsAreManyPerDayInTimeOrder() = runBlocking {
        repo.addCondition(ConditionRecord(date = "2026-11-12", timeMin = 19 * 60, mind = 4))
        val firstId = repo.addCondition(ConditionRecord(date = "2026-11-12", timeMin = 9 * 60, mind = 2, body = 3, note = "朝"))
        val list = repo.data.first().conditionsOn("2026-11-12")
        assertEquals(listOf(9 * 60, 19 * 60), list.map { it.timeMin })

        val rec = list.first { it.id == firstId }
        repo.deleteCondition(rec)
        assertEquals(1, repo.data.first().conditionsOn("2026-11-12").size)
        repo.restoreCondition(rec)
        assertEquals("朝", repo.data.first().conditionsOn("2026-11-12").first().note)
    }

    @Test
    fun recordVisitMovesMemosClearsAiAndReplacesAppointment() = runBlocking {
        val a = repo.addMemo("夜中に目が覚める")
        val b = repo.addMemo("職場の人と話すと緊張する", Kind.COUN)
        repo.saveAiSummaries(mapOf(Kind.DOC to "1. 睡眠", Kind.COUN to "1. 緊張"))
        repo.setAppointments(mapOf(Kind.DOC to Appointment(Kind.DOC, "2026-11-13", 630)))

        repo.recordVisit(
            kind = Kind.DOC,
            date = "2026-11-13",
            talkedIds = setOf(a),
            feedback = "",
            next = Appointment(Kind.DOC, "2026-12-11", null),
        )

        val data = repo.data.first()
        assertEquals(listOf(b), data.memos.map { it.id })
        assertEquals(listOf(a), data.talked.map { it.id })
        val v = data.lastVisit(Kind.DOC)!!
        assertEquals("夜中に目が覚める", v.talkedSummary)
        assertEquals("-", v.feedback)
        assertNull(data.aiSummaries[Kind.DOC])
        assertEquals("1. 緊張", data.aiSummaries[Kind.COUN]?.text)
        assertEquals("2026-12-11", data.appointments[Kind.DOC]?.date)
        assertNull(data.appointments[Kind.DOC]?.timeMin)

        repo.returnTalked(data.talked.first())
        assertEquals(2, repo.data.first().memos.size)
    }

    @Test
    fun recordVisitKeepsFutureAppointment() = runBlocking {
        repo.setAppointments(mapOf(Kind.COUN to Appointment(Kind.COUN, "2026-12-01", 900)))
        repo.recordVisit(Kind.COUN, "2026-11-20", emptySet(), "書き出してみる", next = null)
        assertEquals("2026-12-01", repo.data.first().appointments[Kind.COUN]?.date)
    }

    @Test
    fun tagMemosOnlyTouchesOpenMemos() = runBlocking {
        val a = repo.addMemo("薬のあと眠い")
        val b = repo.addMemo("涙が出た")
        repo.recordVisit(Kind.DOC, "2026-11-01", setOf(b), "", null)
        val n = repo.tagMemos(mapOf(a to Kind.DOC, b to Kind.COUN, 999L to Kind.DOC))
        assertEquals(1, n)
        val data = repo.data.first()
        assertEquals(Kind.DOC, data.memos.single().kind)
        assertNull(data.talked.single().kind)
    }

    @Test
    fun clearAndRestoreAiSummaries() = runBlocking {
        repo.saveAiSummaries(mapOf(Kind.DOC to "x"))
        val old = repo.clearAiSummaries()
        assertTrue(repo.data.first().aiSummaries.isEmpty())
        repo.restoreAiSummaries(old)
        assertEquals("x", repo.data.first().aiSummaries[Kind.DOC]?.text)
    }
}
