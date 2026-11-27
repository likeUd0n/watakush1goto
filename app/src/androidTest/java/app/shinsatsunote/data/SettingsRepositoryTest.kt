package app.shinsatsunote.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    @Test
    fun defaultsAndToggles() = runBlocking {
        val dir = ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir
        val file = File(dir, "settings-test-${System.nanoTime()}.preferences_pb")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })

        assertEquals(AppSettings(true, true, false), repo.settings.first())
        assertEquals(listOf(Kind.DOC, Kind.COUN), repo.settings.first().kinds)

        repo.setUse(Kind.DOC, false)
        repo.setOnboardingDone()
        val s = repo.settings.first()
        assertEquals(listOf(Kind.COUN), s.kinds)
        assertEquals(true, s.onboardingDone)

        scope.cancel()
        file.delete()
        Unit
    }
}
