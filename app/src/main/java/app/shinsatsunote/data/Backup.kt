package app.shinsatsunote.data

import org.json.JSONArray
import org.json.JSONObject

// バックアップの JSON。書き出したファイルから、別の端末や入れ直したアプリに戻すために使う

private const val FORMAT = "shinsatsu-note"
private const val VERSION = 1

data class BackupData(
    val sleeps: List<SleepRecord> = emptyList(),
    val conditions: List<ConditionRecord> = emptyList(),
    val memos: List<ConsultMemo> = emptyList(),
    val visits: List<Visit> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val aiSummaries: List<AiSummary> = emptyList(),
) {
    val size: Int get() = sleeps.size + conditions.size + memos.size + visits.size + appointments.size + aiSummaries.size
}

class BackupFormatException(message: String) : Exception(message)

fun BackupData.toJson(exportedAt: Long = System.currentTimeMillis()): String = JSONObject().apply {
    put("format", FORMAT)
    put("version", VERSION)
    put("exportedAt", exportedAt)
    put("sleep_records", JSONArray(sleeps.map {
        JSONObject().put("date", it.date).put("sleepMin", it.sleepMin).put("wakeMin", it.wakeMin)
            .putOpt("napSleepMin", it.napSleepMin).putOpt("napWakeMin", it.napWakeMin).put("updatedAt", it.updatedAt)
    }))
    put("condition_records", JSONArray(conditions.map {
        JSONObject().put("date", it.date).put("timeMin", it.timeMin).putOpt("mind", it.mind).putOpt("body", it.body)
            .put("note", it.note).put("updatedAt", it.updatedAt)
    }))
    put("consult_memos", JSONArray(memos.map {
        JSONObject().put("text", it.text).putOpt("kind", it.kind).put("createdAt", it.createdAt)
            .putOpt("talkedAt", it.talkedAt).put("pinned", it.pinned)
    }))
    put("visits", JSONArray(visits.map {
        JSONObject().put("date", it.date).put("kind", it.kind).put("talkedSummary", it.talkedSummary)
            .put("feedback", it.feedback).put("createdAt", it.createdAt)
    }))
    put("appointments", JSONArray(appointments.map {
        JSONObject().put("kind", it.kind).put("date", it.date).putOpt("timeMin", it.timeMin)
    }))
    put("ai_summaries", JSONArray(aiSummaries.map {
        JSONObject().put("kind", it.kind).put("text", it.text).put("savedAt", it.savedAt)
    }))
}.toString(1)

/** このアプリが書き出した JSON を読む。形が違えば BackupFormatException */
fun parseBackup(json: String): BackupData {
    val root = try {
        JSONObject(json)
    } catch (e: Exception) {
        throw BackupFormatException("JSON ではありません")
    }
    if (root.optString("format") != FORMAT) throw BackupFormatException("このアプリのバックアップではありません")
    if (root.optInt("version") > VERSION) throw BackupFormatException("新しい版のアプリで書き出したファイルです")
    fun JSONObject.intOrNull(name: String) = if (has(name) && !isNull(name)) getInt(name) else null
    fun JSONObject.longOrNull(name: String) = if (has(name) && !isNull(name)) getLong(name) else null
    fun JSONObject.strOrNull(name: String) = if (has(name) && !isNull(name)) getString(name) else null
    fun <T> list(name: String, f: (JSONObject) -> T): List<T> {
        val a = root.optJSONArray(name) ?: return emptyList()
        return (0 until a.length()).map { f(a.getJSONObject(it)) }
    }
    return try {
        BackupData(
            sleeps = list("sleep_records") {
                SleepRecord(it.getString("date"), it.getInt("sleepMin"), it.getInt("wakeMin"),
                    it.intOrNull("napSleepMin"), it.intOrNull("napWakeMin"), it.optLong("updatedAt"))
            },
            conditions = list("condition_records") {
                ConditionRecord(0, it.getString("date"), it.getInt("timeMin"), it.intOrNull("mind"), it.intOrNull("body"),
                    it.optString("note"), it.optLong("updatedAt"))
            },
            memos = list("consult_memos") {
                ConsultMemo(
                    0,
                    it.getString("text"),
                    it.strOrNull("kind"),
                    it.getLong("createdAt"),
                    it.longOrNull("talkedAt"),
                    it.optBoolean("pinned", false),
                )
            },
            visits = list("visits") {
                Visit(0, it.getString("date"), it.getString("kind"), it.optString("talkedSummary"), it.optString("feedback"),
                    it.optLong("createdAt"))
            },
            appointments = list("appointments") { Appointment(it.getString("kind"), it.getString("date"), it.intOrNull("timeMin")) },
            aiSummaries = list("ai_summaries") { AiSummary(it.getString("kind"), it.getString("text"), it.optLong("savedAt")) },
        )
    } catch (e: BackupFormatException) {
        throw e
    } catch (e: Exception) {
        throw BackupFormatException("中身の形が違います")
    }
}

/** 読み込みで足すもの。count は足した・置き換えた件数 */
data class MergePlan(
    val sleeps: List<SleepRecord>,
    val conditions: List<ConditionRecord>,
    val memos: List<ConsultMemo>,
    val visits: List<Visit>,
    val appointments: List<Appointment>,
    val aiSummaries: List<AiSummary>,
) {
    val count: Int get() = sleeps.size + conditions.size + memos.size + visits.size + appointments.size + aiSummaries.size
}

/**
 * 今のデータに読み込んだものを足す計画。何度読み込んでも増えないようにする。
 * 睡眠・予定・AIの整理はキー(日付・相手)で置き換え(同じ内容なら何もしない)、
 * 調子・話すこと・診察の記録は日時＋内容が同じものを飛ばす
 */
fun planMerge(current: BackupData, incoming: BackupData): MergePlan {
    val sleepNow = current.sleeps.associateBy { it.date }
    val apptNow = current.appointments.associateBy { it.kind }
    val aiNow = current.aiSummaries.associateBy { it.kind }
    fun SleepRecord.same(o: SleepRecord) =
        sleepMin == o.sleepMin && wakeMin == o.wakeMin && napSleepMin == o.napSleepMin && napWakeMin == o.napWakeMin
    fun ConditionRecord.sig() = listOf(date, timeMin, mind, body, note)
    fun ConsultMemo.sig() = listOf(createdAt, text)
    fun Visit.sig() = listOf(date, kind, talkedSummary, feedback)

    val condSeen = current.conditions.map { it.sig() }.toMutableSet()
    val memoSeen = current.memos.map { it.sig() }.toMutableSet()
    val visitSeen = current.visits.map { it.sig() }.toMutableSet()
    return MergePlan(
        sleeps = incoming.sleeps.filter { s -> sleepNow[s.date]?.same(s) != true },
        conditions = incoming.conditions.filter { condSeen.add(it.sig()) },
        memos = incoming.memos.filter { memoSeen.add(it.sig()) },
        visits = incoming.visits.filter { visitSeen.add(it.sig()) },
        appointments = incoming.appointments.filter { apptNow[it.kind] != it },
        aiSummaries = incoming.aiSummaries.filter { a -> aiNow[a.kind]?.text != a.text },
    )
}
