package app.shinsatsunote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** アプリ設定。「使うもの」をオフにしても記録は消さず、見せないだけにする */
data class AppSettings(
    val useDoc: Boolean = true,
    val useCoun: Boolean = true,
    val onboardingDone: Boolean = false,
    val aiGuideDone: Boolean = false,
) {
    /** いま使っている相手(診察 → カウンセリングの順) */
    val kinds: List<String>
        get() = buildList {
            if (useDoc) add(Kind.DOC)
            if (useCoun) add(Kind.COUN)
        }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsStore)

    private object Keys {
        val USE_DOC = booleanPreferencesKey("useDoc")
        val USE_COUN = booleanPreferencesKey("useCoun")
        val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
        val AI_GUIDE_DONE = booleanPreferencesKey("aiGuideDone")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            useDoc = p[Keys.USE_DOC] ?: true,
            useCoun = p[Keys.USE_COUN] ?: true,
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
            aiGuideDone = p[Keys.AI_GUIDE_DONE] ?: false,
        )
    }

    suspend fun setUse(kind: String, on: Boolean) {
        store.edit { it[if (kind == Kind.DOC) Keys.USE_DOC else Keys.USE_COUN] = on }
    }

    suspend fun setOnboardingDone() {
        store.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setAiGuideDone() {
        store.edit { it[Keys.AI_GUIDE_DONE] = true }
    }
}
