package app.shinsatsunote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records ORDER BY date")
    fun observeAll(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records ORDER BY date")
    suspend fun getAll(): List<SleepRecord>

    @Query("SELECT * FROM sleep_records WHERE date = :date")
    suspend fun get(date: String): SleepRecord?

    /** いちばん新しい記録(睡眠画面の初期値に使う) */
    @Query("SELECT * FROM sleep_records ORDER BY date DESC LIMIT 1")
    suspend fun latest(): SleepRecord?

    @Upsert
    suspend fun upsert(record: SleepRecord)

    @Query("DELETE FROM sleep_records WHERE date = :date")
    suspend fun delete(date: String)
}

@Dao
interface ConditionDao {
    @Query("SELECT * FROM condition_records ORDER BY date, timeMin, id")
    fun observeAll(): Flow<List<ConditionRecord>>

    @Query("SELECT * FROM condition_records ORDER BY date, timeMin, id")
    suspend fun getAll(): List<ConditionRecord>

    @Query("SELECT * FROM condition_records WHERE id = :id")
    suspend fun get(id: Long): ConditionRecord?

    @Insert
    suspend fun insert(record: ConditionRecord): Long

    @Upsert
    suspend fun upsert(record: ConditionRecord)

    @Delete
    suspend fun delete(record: ConditionRecord)
}

@Dao
interface MemoDao {
    @Query("SELECT * FROM consult_memos ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<ConsultMemo>>

    @Query("SELECT * FROM consult_memos ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<ConsultMemo>

    @Insert
    suspend fun insert(memo: ConsultMemo): Long

    @Upsert
    suspend fun upsert(memo: ConsultMemo)

    @Upsert
    suspend fun upsertAll(memos: List<ConsultMemo>)

    @Delete
    suspend fun delete(memo: ConsultMemo)
}

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<Visit>>

    @Query("SELECT * FROM visits ORDER BY date DESC, createdAt DESC")
    suspend fun getAll(): List<Visit>

    @Insert
    suspend fun insert(visit: Visit): Long
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments")
    fun observeAll(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments")
    suspend fun getAll(): List<Appointment>

    @Upsert
    suspend fun upsert(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE kind = :kind")
    suspend fun delete(kind: String)
}

@Dao
interface AiSummaryDao {
    @Query("SELECT * FROM ai_summaries")
    fun observeAll(): Flow<List<AiSummary>>

    @Query("SELECT * FROM ai_summaries")
    suspend fun getAll(): List<AiSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: AiSummary)

    @Query("DELETE FROM ai_summaries WHERE kind = :kind")
    suspend fun delete(kind: String)

    @Query("DELETE FROM ai_summaries")
    suspend fun deleteAll()
}
