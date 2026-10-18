package org.parkjw.apps.pdfbook.update

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun persistsLastCheckDayAndSnooze() = runBlocking {
        val store = DataStoreUpdateStore(
            PreferenceDataStoreFactory.create(produceFile = { File(tmp.root, "update.preferences_pb") }),
        )
        assertNull(store.lastAutoCheckDay())
        assertNull(store.snooze())
        store.setLastAutoCheckDay(20_000)
        store.setSnooze(UpdateSnooze("0.3.1", 20_000))
        assertEquals(20_000L, store.lastAutoCheckDay())
        assertEquals(UpdateSnooze("0.3.1", 20_000), store.snooze())
    }
}
