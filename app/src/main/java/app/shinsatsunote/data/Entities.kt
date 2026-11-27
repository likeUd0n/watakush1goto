package app.shinsatsunote.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 日付は "YYYY-MM-DD"、時刻は 0:00 からの分(0〜1439)で持つ。相手は "doc"(診察)/"coun"(カウンセリング)

/** 睡眠(1日1件)。日付は起きた日。二度寝・昼寝は無ければ null */
@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey val date: String,
    val sleepMin: Int,
    val wakeMin: Int,
    val napSleepMin: Int? = null,
    val napWakeMin: Int? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** 調子(1日に何件でも)。ココロ・カラダは 1(つらい)〜5(楽)。選ばなければ null */
@Entity(tableName = "condition_records", indices = [Index("date")])
data class ConditionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val timeMin: Int,
    val mind: Int? = null,
    val body: Int? = null,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

/** 話すこと。talkedAt が null なら未相談、入っていれば「話したこと」。pinned は本人が相手や必須を固定指定した印 */
@Entity(tableName = "consult_memos")
data class ConsultMemo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val kind: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val talkedAt: Long? = null,
    val pinned: Boolean = false,
)

/** 診察・カウンセリングの記録 */
@Entity(tableName = "visits", indices = [Index("date")])
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val kind: String,
    val talkedSummary: String,
    val feedback: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 次の予定(相手ごとに最大1件) */
@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey val kind: String,
    val date: String,
    val timeMin: Int? = null,
)

/** AIの整理(相手ごとに0〜1件) */
@Entity(tableName = "ai_summaries")
data class AiSummary(
    @PrimaryKey val kind: String,
    val text: String,
    val savedAt: Long = System.currentTimeMillis(),
)

object Kind {
    const val DOC = "doc"
    const val COUN = "coun"
    val all = listOf(DOC, COUN)

    fun label(kind: String?): String = when (kind) {
        DOC -> "診察"
        COUN -> "カウンセリング"
        else -> "未分類"
    }
}
