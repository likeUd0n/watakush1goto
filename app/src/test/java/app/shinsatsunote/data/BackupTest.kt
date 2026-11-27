package app.shinsatsunote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {
    private val data = BackupData(
        sleeps = listOf(SleepRecord("2026-11-13", 40, 425, 780, 870, 1L)),
        conditions = listOf(ConditionRecord(5, "2026-11-13", 560, 3, null, "朝は眠い", 2L)),
        memos = listOf(ConsultMemo(7, "夜中に目が覚める", Kind.DOC, 3L, null), ConsultMemo(8, "涙が出た", null, 4L, 9L)),
        visits = listOf(Visit(1, "2026-10-30", Kind.COUN, "緊張する", "書き出す", 5L)),
        appointments = listOf(Appointment(Kind.DOC, "2026-12-11", 630), Appointment(Kind.COUN, "2026-12-01", null)),
        aiSummaries = listOf(AiSummary(Kind.DOC, "1. 睡眠", 6L)),
    )

    @Test
    fun roundTrip() {
        val back = parseBackup(data.toJson())
        assertEquals(data.sleeps, back.sleeps)
        assertEquals(data.appointments, back.appointments)
        assertEquals(data.aiSummaries, back.aiSummaries)
        // id は入れ直すときに振り直すので持たない
        assertEquals(data.conditions.map { it.copy(id = 0) }, back.conditions)
        assertEquals(data.memos.map { it.copy(id = 0) }, back.memos)
        assertNull(back.conditions[0].body)
        assertEquals(9L, back.memos[1].talkedAt)
    }

    @Test
    fun rejectsOtherFiles() {
        assertThrows(BackupFormatException::class.java) { parseBackup("not json") }
        assertThrows(BackupFormatException::class.java) { parseBackup("""{"format":"other"}""") }
        assertThrows(BackupFormatException::class.java) { parseBackup("""{"format":"shinsatsu-note","version":1,"sleep_records":[{"date":1}]}""") }
    }

    @Test
    fun mergeTwiceDoesNotGrow() {
        val incoming = parseBackup(data.toJson())
        val first = planMerge(BackupData(), incoming)
        assertEquals(data.size, first.count)
        // 入れたあとの状態にもう一度読み込んでも、足すものはない
        assertEquals(0, planMerge(data, incoming).count)
    }

    @Test
    fun mergeOverwritesByKeyAndSkipsDuplicatesInFile() {
        val current = BackupData(sleeps = listOf(SleepRecord("2026-11-13", 0, 400)), appointments = listOf(Appointment(Kind.DOC, "2026-11-20", null)))
        val incoming = data.copy(conditions = data.conditions + data.conditions)
        val plan = planMerge(current, incoming)
        assertEquals(1, plan.sleeps.size)
        assertEquals(1, plan.conditions.size)
        assertEquals(2, plan.appointments.size)
    }
}
