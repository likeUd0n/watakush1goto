package app.shinsatsunote.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.shinsatsunote.data.AppData
import app.shinsatsunote.data.Appointment
import app.shinsatsunote.data.AppDatabase
import app.shinsatsunote.data.AppSettings
import app.shinsatsunote.data.BackupFormatException
import app.shinsatsunote.data.parseBackup
import app.shinsatsunote.data.toJson
import app.shinsatsunote.data.ConditionRecord
import app.shinsatsunote.data.ConsultMemo
import app.shinsatsunote.data.Repository
import app.shinsatsunote.data.SettingsRepository
import app.shinsatsunote.data.SleepRecord
import app.shinsatsunote.domain.looksLikeCrisis
import app.shinsatsunote.domain.parseAiAnswer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** 下に出すお知らせ。undo があれば「元に戻す」を付ける */
data class Toast(val message: String, val undo: (suspend () -> Unit)? = null)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository(AppDatabase.get(app))
    private val settingsRepo = SettingsRepository(app)

    /** null のあいだは読み込み中 */
    val data: StateFlow<AppData?> = repo.data.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<AppSettings?> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val toastChannel = Channel<Toast>(Channel.BUFFERED)
    val toasts = toastChannel.receiveAsFlow()

    /** 相談窓口のシートを出すか */
    private val _crisis = MutableStateFlow(false)
    val crisis: StateFlow<Boolean> = _crisis.asStateFlow()

    /** 記録タブで選んでいる日(サブ画面から戻っても保つ) */
    val selectedDate = MutableStateFlow(LocalDate.now())

    /** 診察タブのモード("prep" = 準備 / "show" = 見せる) */
    val visitMode = MutableStateFlow("prep")

    fun toast(message: String, undo: (suspend () -> Unit)? = null) {
        toastChannel.trySend(Toast(message, undo))
    }

    fun dismissCrisis() {
        _crisis.value = false
    }

    /** 危機を示す語があれば相談窓口を出す。出したら true */
    private fun checkCrisis(text: String): Boolean {
        if (!looksLikeCrisis(text)) return false
        _crisis.value = true
        return true
    }

    fun launchUndo(undo: suspend () -> Unit) {
        viewModelScope.launch { undo() }
    }

    // ---- 睡眠 ----
    suspend fun latestSleep(): SleepRecord? = repo.latestSleep()

    fun saveSleep(record: SleepRecord) = viewModelScope.launch {
        repo.saveSleep(record)
        toast("保存しました")
    }

    fun deleteSleep(date: String) = viewModelScope.launch {
        val old = repo.deleteSleep(date) ?: return@launch
        toast("削除しました") { repo.restoreSleep(old) }
    }

    // ---- 調子 ----
    /** すべて空なら何も増やさない */
    fun saveCondition(editing: ConditionRecord?, date: String, timeMin: Int, mind: Int?, body: Int?, note: String) =
        viewModelScope.launch {
            val text = note.trim()
            if (editing == null && mind == null && body == null && text.isEmpty()) return@launch
            if (editing != null) {
                repo.updateCondition(editing.copy(mind = mind, body = body, note = text))
            } else {
                repo.addCondition(ConditionRecord(date = date, timeMin = timeMin, mind = mind, body = body, note = text))
            }
            checkCrisis(text)
            toast("保存しました")
        }

    fun deleteCondition(record: ConditionRecord) = viewModelScope.launch {
        repo.deleteCondition(record)
        toast("削除しました") { repo.restoreCondition(record) }
    }

    // ---- 話すこと ----
    fun addMemo(text: String, kind: String? = null, pinned: Boolean = false) = viewModelScope.launch {
        val t = text.trim()
        if (t.isEmpty()) return@launch
        repo.addMemo(t, kind, pinned)
        if (!checkCrisis(t)) toast("話すことに追加しました")
    }

    fun updateMemo(memo: ConsultMemo, text: String, kind: String?, pinned: Boolean = memo.pinned) = viewModelScope.launch {
        val t = text.trim()
        if (t.isEmpty() || (t == memo.text && kind == memo.kind && pinned == memo.pinned)) return@launch
        repo.updateMemo(memo.copy(text = t, kind = kind, pinned = pinned))
        if (!checkCrisis(t)) toast("直しました")
    }

    fun deleteMemo(memo: ConsultMemo) = viewModelScope.launch {
        repo.deleteMemo(memo)
        toast("消しました") { repo.restoreMemo(memo) }
    }

    fun returnTalked(memo: ConsultMemo) = viewModelScope.launch {
        repo.returnTalked(memo)
        toast("話すことへ戻しました")
    }

    // ---- 診察・カウンセリング ----
    fun recordVisit(kind: String, date: String, talkedIds: Set<Long>, feedback: String, next: Appointment?) =
        viewModelScope.launch {
            repo.recordVisit(kind, date, talkedIds, feedback.trim(), next)
            toast("記録しました")
        }

    fun setAppointments(values: Map<String, Appointment?>) = viewModelScope.launch {
        repo.setAppointments(values)
        toast("予定を保存しました")
    }

    // ---- AIの整理 ----
    /** 貼られた答えを相手ごとに分けて保存し、「メモ: #番号」の相手を付け替える */
    fun saveAiAnswer(raw: String, kinds: List<String>) = viewModelScope.launch {
        if (raw.isBlank() || kinds.isEmpty()) return@launch
        val parsed = parseAiAnswer(raw, kinds)
        repo.saveAiSummaries(parsed.summaries)
        val n = if (kinds.size >= 2) repo.tagMemos(parsed.tags) else 0
        toast(if (n > 0) "保存し、${n}件を自動で分けました" else "保存しました")
    }

    // ---- 設定 ----
    fun setUse(kind: String, on: Boolean) = viewModelScope.launch { settingsRepo.setUse(kind, on) }

    fun setOnboardingDone() = viewModelScope.launch { settingsRepo.setOnboardingDone() }

    fun setAiGuideDone() = viewModelScope.launch { settingsRepo.setAiGuideDone() }

    /** 選んだ場所(SAF)に JSON を書き出す */
    fun exportBackup(uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            val json = repo.snapshot().toJson()
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }
        }.isSuccess
        toast(if (ok) "書き出しました" else "書き出せませんでした")
    }

    /** JSON を読み込んで今のデータに足す */
    fun importBackup(uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            val json = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            }
            repo.merge(parseBackup(json))
        }
        toast(
            result.fold(
                onSuccess = { if (it > 0) "${it}件を読み込みました" else "新しい記録はありませんでした" },
                onFailure = { "読み込めませんでした" + ((it as? BackupFormatException)?.message?.let { m -> "（$m）" } ?: "") },
            ),
        )
    }

    fun clearAiSummaries() = viewModelScope.launch {
        val old = repo.clearAiSummaries()
        if (old.isNotEmpty()) toast("消しました") { repo.restoreAiSummaries(old) }
    }
}
