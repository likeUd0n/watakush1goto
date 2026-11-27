package app.shinsatsunote.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 画面が使うデータ一式。記録は個人の数年分でも小さいので、まとめて持つ */
data class AppData(
    val sleeps: Map<String, SleepRecord> = emptyMap(),
    val conditions: Map<String, List<ConditionRecord>> = emptyMap(),
    /** 未相談の話すこと(新しい順) */
    val memos: List<ConsultMemo> = emptyList(),
    /** 話したこと(話した日の新しい順) */
    val talked: List<ConsultMemo> = emptyList(),
    /** 診察・カウンセリングの記録(新しい順) */
    val visits: List<Visit> = emptyList(),
    val appointments: Map<String, Appointment> = emptyMap(),
    val aiSummaries: Map<String, AiSummary> = emptyMap(),
) {
    /** その相手の最新の記録 */
    fun lastVisit(kind: String): Visit? = visits.firstOrNull { it.kind == kind }

    fun conditionsOn(date: String): List<ConditionRecord> = conditions[date].orEmpty()
}

class Repository(private val db: AppDatabase) {
    private val sleepDao = db.sleepDao()
    private val conditionDao = db.conditionDao()
    private val memoDao = db.memoDao()
    private val visitDao = db.visitDao()
    private val appointmentDao = db.appointmentDao()
    private val aiDao = db.aiSummaryDao()

    val data: Flow<AppData> = combine(
        sleepDao.observeAll(),
        conditionDao.observeAll(),
        memoDao.observeAll(),
        visitDao.observeAll(),
        combine(appointmentDao.observeAll(), aiDao.observeAll()) { a, s -> a to s },
    ) { sleeps, conds, memos, visits, (appts, ai) ->
        AppData(
            sleeps = sleeps.associateBy { it.date },
            conditions = conds.groupBy { it.date },
            memos = memos.filter { it.talkedAt == null },
            talked = memos.filter { it.talkedAt != null }.sortedByDescending { it.talkedAt },
            visits = visits,
            appointments = appts.associateBy { it.kind },
            aiSummaries = ai.associateBy { it.kind },
        )
    }

    // ---- 睡眠 ----
    suspend fun latestSleep(): SleepRecord? = sleepDao.latest()
    suspend fun saveSleep(record: SleepRecord) = sleepDao.upsert(record.copy(updatedAt = System.currentTimeMillis()))
    suspend fun restoreSleep(record: SleepRecord) = sleepDao.upsert(record)

    /** 消して、元に戻すための記録を返す */
    suspend fun deleteSleep(date: String): SleepRecord? {
        val old = sleepDao.get(date)
        sleepDao.delete(date)
        return old
    }

    // ---- 調子 ----
    suspend fun addCondition(record: ConditionRecord): Long = conditionDao.insert(record.copy(id = 0))
    suspend fun updateCondition(record: ConditionRecord) =
        conditionDao.upsert(record.copy(updatedAt = System.currentTimeMillis()))
    suspend fun restoreCondition(record: ConditionRecord) = conditionDao.upsert(record)
    suspend fun deleteCondition(record: ConditionRecord) = conditionDao.delete(record)

    // ---- 話すこと ----
    suspend fun addMemo(text: String, kind: String? = null, pinned: Boolean = false): Long =
        memoDao.insert(ConsultMemo(text = text, kind = kind, pinned = pinned))
    suspend fun updateMemo(memo: ConsultMemo) = memoDao.upsert(memo)
    suspend fun restoreMemo(memo: ConsultMemo) = memoDao.upsert(memo)
    suspend fun deleteMemo(memo: ConsultMemo) = memoDao.delete(memo)

    /** 「話したこと」を「話すこと」へ戻す */
    suspend fun returnTalked(memo: ConsultMemo) = memoDao.upsert(memo.copy(talkedAt = null))

    /** AIの答えの「メモ: #番号」で、話すことの相手を付け替える。本人が固定指定(pinned)したメモは上書きしない */
    suspend fun tagMemos(tags: Map<Long, String>): Int = db.withTransaction {
        val open = memoDao.getAll().filter { it.talkedAt == null }.associateBy { it.id }
        val changed = tags.mapNotNull { (id, kind) -> open[id]?.takeIf { !it.pinned }?.copy(kind = kind) }
        memoDao.upsertAll(changed)
        changed.size
    }

    // ---- 診察・カウンセリングの記録 ----
    /**
     * 記録を残す。チェックした話すことは「話したこと」へ移し、その相手の「AIの整理」を消す。
     * 記録日以前の予定は済んだものとして消し、次の予定が入っていれば置き換える
     */
    suspend fun recordVisit(
        kind: String,
        date: String,
        talkedIds: Set<Long>,
        feedback: String,
        next: Appointment?,
    ) = db.withTransaction {
        val now = System.currentTimeMillis()
        val done = memoDao.getAll().filter { it.talkedAt == null && it.id in talkedIds }
        memoDao.upsertAll(done.map { it.copy(talkedAt = now) })
        visitDao.insert(
            Visit(
                date = date,
                kind = kind,
                talkedSummary = done.joinToString(" / ") { it.text }.ifEmpty { "-" },
                feedback = feedback.ifBlank { "-" },
                createdAt = now,
            ),
        )
        aiDao.delete(kind)
        val current = appointmentDao.getAll().firstOrNull { it.kind == kind }
        if (current != null && current.date <= date) appointmentDao.delete(kind)
        if (next != null) appointmentDao.upsert(next.copy(kind = kind))
    }

    // ---- 次の予定 ----
    /** 相手ごとに置き換える。null なら消す */
    suspend fun setAppointments(values: Map<String, Appointment?>) = db.withTransaction {
        values.forEach { (kind, a) -> if (a == null) appointmentDao.delete(kind) else appointmentDao.upsert(a.copy(kind = kind)) }
    }

    // ---- AIの整理 ----
    suspend fun saveAiSummaries(texts: Map<String, String>) = db.withTransaction {
        aiDao.deleteAll()
        texts.forEach { (kind, text) -> aiDao.upsert(AiSummary(kind = kind, text = text)) }
    }

    /** 全部消して、元に戻すための一覧を返す */
    suspend fun clearAiSummaries(): List<AiSummary> {
        val old = aiDao.getAll()
        aiDao.deleteAll()
        return old
    }

    suspend fun restoreAiSummaries(list: List<AiSummary>) = db.withTransaction { list.forEach { aiDao.upsert(it) } }

    // ---- バックアップ ----
    suspend fun snapshot(): BackupData = BackupData(
        sleeps = sleepDao.getAll(),
        conditions = conditionDao.getAll(),
        memos = memoDao.getAll(),
        visits = visitDao.getAll(),
        appointments = appointmentDao.getAll(),
        aiSummaries = aiDao.getAll(),
    )

    /** 読み込んだものを今のデータに足す(消すことはしない)。足した・置き換えた件数を返す */
    suspend fun merge(incoming: BackupData): Int = db.withTransaction {
        val plan = planMerge(snapshot(), incoming)
        plan.sleeps.forEach { sleepDao.upsert(it) }
        plan.appointments.forEach { appointmentDao.upsert(it) }
        plan.aiSummaries.forEach { aiDao.upsert(it) }
        plan.conditions.forEach { conditionDao.insert(it.copy(id = 0)) }
        plan.memos.forEach { memoDao.insert(it.copy(id = 0)) }
        plan.visits.forEach { visitDao.insert(it.copy(id = 0)) }
        plan.count
    }
}
