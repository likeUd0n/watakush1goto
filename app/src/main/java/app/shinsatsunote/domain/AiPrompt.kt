package app.shinsatsunote.domain

import app.shinsatsunote.data.AppData
import app.shinsatsunote.data.Kind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// 外部のAIアプリに渡す文を作り、返ってきた答えを相手ごとに分ける。
// アプリの中でAIは呼ばない(INTERNET 権限なし)。文を作る処理は、ここに独立させておく

/** 渡す文と、危機を示す語があって外した件数 */
data class AiPrompt(val text: String, val excluded: Int)

private const val FOOTER_GUIDE =
    "前置きや挨拶・励ましなどの余計な文章は書かず、要約だけを出力してください。" +
        "最後に1行あけて、次の1行だけをそのまま添えてください：\n" +
        "※この返答をコピーして、アプリの「答えを貼る」に貼り付けてください。"

private const val ASK_BOTH =
    "次の精神科・心療内科の診察（5〜10分）とカウンセリングに向けて、下のメモと記録を整理してください。" +
        "まず各メモを【診察】か【カウンセリング】に振り分け、それぞれ大事な順に3つまで1〜2文で要約してください。" +
        "【必ず診察】【必ずカウンセリング】が付いているメモは、指定された相手から動かさず必ず要約に含めてください。" +
        "診察は睡眠・調子の変化・薬のこと、カウンセリングは出来事と気持ちを中心にしてください。診断や断定はしないでください。\n" +
        "$FOOTER_GUIDE\n\n" +
        "出力形式：\n【診察】\nメモ: #番号, #番号\n1. ...\n\n【カウンセリング】\nメモ: #番号\n1. ..."

private const val ASK_DOC =
    "次の精神科・心療内科の診察（5〜10分）で、医師に伝えたいことを、大事な順に3つまで、1つ1〜2文で挙げてください。" +
        "【必ず相談したい】が付いているメモは、必ず要約に含めてください。" +
        "睡眠と調子の変化、薬について気になることを優先してください。診断や断定はしないでください。\n" +
        FOOTER_GUIDE

private const val ASK_COUN =
    "次のカウンセリングで話したいことを、大事な順に3つまで挙げてください。" +
        "【必ず相談したい】が付いているメモは、必ず要約に含めてください。" +
        "出来事と、そのときの気持ちを中心にしてください。診断や断定はしないでください。\n" +
        FOOTER_GUIDE

/**
 * 渡す文を作る。指示文＋前回のアドバイス＋未相談のメモ＋前回からの記録。
 * 両方使うときは、前回から長いほうの期間をまとめて渡し、メモに #番号 を付けて振り分けてもらう
 */
fun buildAiPrompt(
    data: AppData,
    kinds: List<String>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): AiPrompt {
    require(kinds.isNotEmpty())
    val both = kinds.size >= 2
    val days = kinds.map { daysSince(data.lastVisit(it)?.date, today) }.maxBy { it.size }
    var excluded = 0

    val memoLines = data.memos.filter {
        if (looksLikeCrisis(it.text)) {
            excluded++
            false
        } else {
            true
        }
    }.joinToString("\n") { m ->
        val d = Instant.ofEpochMilli(m.createdAt).atZone(zone).toLocalDate().md()
        val pin = when {
            !m.pinned -> ""
            both && m.kind != null -> "【必ず${Kind.label(m.kind)}】 "
            else -> "【必ず相談したい】 "
        }
        if (both) "[#${m.id}] $pin$d ${m.text}" else "$pin$d ${m.text}"
    }.ifEmpty { "(なし)" }

    fun avg(values: List<Int?>) = values.filterNotNull().takeIf { it.isNotEmpty() }?.average()?.let { "%.1f".format(it) } ?: "-"
    val recordLines = days.reversed().mapNotNull { d ->
        val k = d.key()
        val sr = data.sleeps[k]
        val conds = data.conditionsOn(k)
        if (sr == null && conds.isEmpty()) return@mapNotNull null
        val sleep = sr?.totalMin?.takeIf { it > 0 }?.let(::durJ) ?: "-"
        val notes = conds.map { it.note }.filter { n ->
            when {
                n.isEmpty() -> false
                looksLikeCrisis(n) -> {
                    excluded++
                    false
                }
                else -> true
            }
        }.joinToString(" / ")
        "${d.md()} ココロ${avg(conds.map { it.mind })} カラダ${avg(conds.map { it.body })} 睡眠 $sleep" +
            if (notes.isNotEmpty()) " $notes" else ""
    }.joinToString("\n")

    val prevs = kinds.mapNotNull { k ->
        data.lastVisit(k)?.let { v -> "― 前回の${Kind.label(k)}（${parseDate(v.date).mdw()}）でのアドバイス ―\n${v.feedback}" }
    }.joinToString("\n\n")

    val ask = when {
        both -> ASK_BOTH
        kinds[0] == Kind.DOC -> ASK_DOC
        else -> ASK_COUN
    }
    val text = buildString {
        append(ask)
        if (prevs.isNotEmpty()) append("\n\n").append(prevs)
        append("\n\n― メモ ―\n").append(memoLines)
        append("\n\n― 記録（${days.first().md()}〜${days.last().md()}） ―\n").append(recordLines)
    }
    return AiPrompt(text, excluded)
}

/** AIの答えを分けたもの。summaries は相手ごとの要約、tags は「メモ: #番号」で分かったメモの相手 */
data class ParsedAi(val summaries: Map<String, String>, val tags: Map<Long, String>)

// 【診察】 / [カウンセリング] / 診察向け: / **【診察】** / ## 診察 などの見出し（後ろに「メモ:」が同行で続く場合も含む）
private val SECTION = Regex("""(?m)^[ \t#*]*(?:【|\[)?(診察|カウンセリング)(?:向け)?(?:】|\])?[*]*[：:]?(?=[ \t]*(?:$|[*・-]*メモ[*]*[：:]))""")
private val MEMO_LINE = Regex("""^[ \t*・-]*メモ[*]*[：:]""")
private val MEMO_ID = Regex("""#(\d+)""")
private val FOOTER_LINE = Regex("""^[ \t*・※-]*この返答をコピーして.*""")
private val HR_LINE = Regex("""^[ \t]*[-*_―─]{3,}[ \t]*$""")

private fun stripFooter(text: String): String {
    val lines = text.lines().dropLastWhile { it.isBlank() }.toMutableList()
    while (lines.isNotEmpty() && (FOOTER_LINE.containsMatchIn(lines.last()) || HR_LINE.matches(lines.last()))) {
        lines.removeAt(lines.lastIndex)
        while (lines.isNotEmpty() && lines.last().isBlank()) lines.removeAt(lines.lastIndex)
    }
    return lines.joinToString("\n").trim()
}

/**
 * 答えから【診察】【カウンセリング】の要約と、「メモ: #番号」の振り分けを取り出す。
 * 末尾に付けた貼り付け案内行は自動で取り除く。
 * 見出しがなければ全体を1つの要約として、両方使うときは診察へ、片方ならその相手へ入れる
 */
fun parseAiAnswer(raw: String, kinds: List<String>): ParsedAi {
    val text = stripFooter(raw.replace("\r\n", "\n").trim())
    val hits = SECTION.findAll(text).toList()
    if (hits.isEmpty()) {
        val target = if (kinds.size >= 2) Kind.DOC else kinds.first()
        return ParsedAi(mapOf(target to text), emptyMap())
    }
    val summaries = linkedMapOf<String, String>()
    val tags = linkedMapOf<Long, String>()
    hits.forEachIndexed { i, h ->
        val kind = if (h.groupValues[1] == "診察") Kind.DOC else Kind.COUN
        val end = if (i + 1 < hits.size) hits[i + 1].range.first else text.length
        val body = text.substring(h.range.last + 1, end)
        val kept = body.lines().filter { line ->
            if (MEMO_LINE.containsMatchIn(line)) {
                MEMO_ID.findAll(line).forEach { tags[it.groupValues[1].toLong()] = kind }
                false
            } else {
                true
            }
        }.joinToString("\n").trim()
        if (kept.isNotEmpty()) summaries[kind] = kept
    }
    return ParsedAi(summaries, tags)
}
