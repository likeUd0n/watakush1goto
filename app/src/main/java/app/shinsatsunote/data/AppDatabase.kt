package app.shinsatsunote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SleepRecord::class,
        ConditionRecord::class,
        ConsultMemo::class,
        Visit::class,
        Appointment::class,
        AiSummary::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun conditionDao(): ConditionDao
    abstract fun memoDao(): MemoDao
    abstract fun visitDao(): VisitDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun aiSummaryDao(): AiSummaryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE consult_memos ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "shinsatsu.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
