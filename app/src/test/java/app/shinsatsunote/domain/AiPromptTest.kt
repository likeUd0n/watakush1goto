package app.shinsatsunote.domain

import app.shinsatsunote.data.AppData
import app.shinsatsunote.data.ConditionRecord
import app.shinsatsunote.data.ConsultMemo
import app.shinsatsunote.data.Kind
import app.shinsatsunote.data.SleepRecord
import app.shinsatsunote.data.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AiPromptTest {
    private val today = LocalDate.of(2026, 11, 13)
    private val utc = ZoneOffset.UTC
    private fun millis(d: LocalDate) = d.atStartOfDay(utc).toInstant().toEpochMilli()

    private val data = AppData(
        sleeps = mapOf("2026-11-12" to SleepRecord("2026-11-12", 30, 420)),
        conditions = mapOf(
            "2026-11-12" to listOf(
                ConditionRecord(1, "2026-11-12", 9 * 60, 2, 3, "朝がだるい"),
                ConditionRecord(2, "2026-11-12", 19 * 60, 4, 3, "消えたい"),
            ),
        ),
        memos = listOf(
            ConsultMemo(3, "夜中に目が覚める", createdAt = millis(LocalDate.of(2026, 11, 10))),
            ConsultMemo(4, "しにたいと思った", createdAt = millis(LocalDate.of(2026, 11, 11))),
        ),
        visits = listOf(Visit(1, "2026-11-01", Kind.DOC, "-", "睡眠リズム優先")),
    )

    @Test
    fun bothKindsTagsMemosAndExcludesCrisis() {
        val p = buildAiPrompt(data, listOf(Kind.DOC, Kind.COUN), today, utc)
        assertEquals(2, p.excluded)
        assertTrue(p.text.startsWith("次の精神科・心療内科の診察（5〜10分）とカウンセリング"))
        assertTrue(p.text.contains("[#3] 11/10 夜中に目が覚める"))
        assertFalse(p.text.contains("しにたい"))
        assertFalse(p.text.contains("消えたい"))
        assertTrue(p.text.contains("― 前回の診察（11/1(日)）でのアドバイス ―\n睡眠リズム優先"))
        // カウンセリングは前回が無いので30日。長いほうの期間を渡す
        assertTrue(p.text.contains("― 記録（10/15〜11/13） ―"))
        assertTrue(p.text.contains("11/12 ココロ3.0 カラダ3.0 睡眠 6時間30分 朝がだるい"))
    }

    @Test
    fun singleKindHasNoIds() {
        val p = buildAiPrompt(data, listOf(Kind.DOC), today, utc)
        assertTrue(p.text.contains("\n11/10 夜中に目が覚める"))
        assertTrue(p.text.contains("― 記録（11/2〜11/13） ―"))
        assertTrue(p.text.startsWith("次の精神科・心療内科の診察（5〜10分）で、医師に"))
    }

    @Test
    fun parseSections() {
        val raw = "はい、整理しました。\r\n\r\n**【診察】**\r\nメモ: #3, #5\r\n1. 夜中に目が覚める\r\n\r\n【カウンセリング】\nメモ：#4\n1. 涙が出た"
        val r = parseAiAnswer(raw, listOf(Kind.DOC, Kind.COUN))
        assertEquals("1. 夜中に目が覚める", r.summaries[Kind.DOC])
        assertEquals("1. 涙が出た", r.summaries[Kind.COUN])
        assertEquals(mapOf(3L to Kind.DOC, 5L to Kind.DOC, 4L to Kind.COUN), r.tags)
    }

    @Test
    fun parseFreeTextGoesToOneKind() {
        assertEquals(mapOf(Kind.DOC to "1. a"), parseAiAnswer("1. a", listOf(Kind.DOC, Kind.COUN)).summaries)
        assertEquals(mapOf(Kind.COUN to "1. a"), parseAiAnswer(" 1. a ", listOf(Kind.COUN)).summaries)
    }

    @Test
    fun headingInsideSentenceIsNotSection() {
        val r = parseAiAnswer("次の診察では\n眠りのことを話す", listOf(Kind.DOC, Kind.COUN))
        assertEquals(1, r.summaries.size)
        assertTrue(r.tags.isEmpty())
    }

    @Test
    fun parseSectionsWithInlineMemoLine() {
        val raw = "【診察】 メモ: #1, #2, #5, #6\n1. 睡眠リズムの乱れ\n\n**【カウンセリング】** **メモ：** #3, #4\n1. 仕事での不安"
        val r = parseAiAnswer(raw, listOf(Kind.DOC, Kind.COUN))
        assertEquals("1. 睡眠リズムの乱れ", r.summaries[Kind.DOC])
        assertEquals("1. 仕事での不安", r.summaries[Kind.COUN])
        assertEquals(
            mapOf(1L to Kind.DOC, 2L to Kind.DOC, 5L to Kind.DOC, 6L to Kind.DOC, 3L to Kind.COUN, 4L to Kind.COUN),
            r.tags,
        )
    }

    @Test
    fun stripsFooterGuideOnPaste() {
        val raw = "【診察】\nメモ: #3\n1. 夜中に目が覚める\n\n---\n※この返答をコピーして、アプリの「答えを貼る」に貼り付けてください。"
        val r = parseAiAnswer(raw, listOf(Kind.DOC, Kind.COUN))
        assertEquals("1. 夜中に目が覚める", r.summaries[Kind.DOC])
        assertEquals(mapOf(3L to Kind.DOC), r.tags)
    }

    @Test
    fun pinnedMemoIncludesRoutingTag() {
        val d = AppData(
            memos = listOf(
                ConsultMemo(10, "薬の副作用がつらい", kind = Kind.DOC, createdAt = millis(LocalDate.of(2026, 11, 10)), pinned = true),
                ConsultMemo(11, "家族との会話", kind = Kind.COUN, createdAt = millis(LocalDate.of(2026, 11, 11)), pinned = true),
                ConsultMemo(12, "普通のメモ", kind = null, createdAt = millis(LocalDate.of(2026, 11, 12)), pinned = false),
            ),
        )
        val both = buildAiPrompt(d, listOf(Kind.DOC, Kind.COUN), today, utc)
        assertTrue(both.text.contains("[#10] 【必ず診察】 11/10 薬の副作用がつらい"))
        assertTrue(both.text.contains("[#11] 【必ずカウンセリング】 11/11 家族との会話"))
        assertTrue(both.text.contains("[#12] 11/12 普通のメモ"))

        val single = buildAiPrompt(d, listOf(Kind.DOC), today, utc)
        assertTrue(single.text.contains("【必ず相談したい】 11/10 薬の副作用がつらい"))
    }
}
