package org.smriti.app.device

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceRegistryTest {
    private class Fake(override val id: String) : Wearable {
        override val vendor = id
        override val state: Flow<ConnectionState> = emptyFlow()
        override suspend fun connect() = DeviceInfo("1", "1", null)
        override suspend fun disconnect() {}
        override suspend fun capabilities() = emptySet<Capability>()
    }

    @Test fun todaysSplit() {
        val r = DeviceRegistry()
        r.register(Fake("es100"), setOf(Capability.VOICE_RECORDINGS))
        r.register(Fake("ring"), setOf(Capability.HEALTH_HISTORY, Capability.RAW_PPG))
        assertEquals("es100", r.voiceSource()?.id)
        assertEquals("ring", r.healthSource()?.id)
    }

    @Test fun futureComboRingTakesOverBothJobs() {
        val r = DeviceRegistry()
        r.register(Fake("es100"), setOf(Capability.VOICE_RECORDINGS))
        r.register(Fake("ring"), setOf(Capability.HEALTH_HISTORY))
        r.register(Fake("combo"), setOf(Capability.VOICE_RECORDINGS, Capability.HEALTH_HISTORY))
        assertEquals("combo", r.voiceSource()?.id)
        assertEquals("combo", r.healthSource()?.id)
        r.prefer(Capability.VOICE_RECORDINGS, "es100")
        assertEquals("es100", r.voiceSource()?.id)
        r.unregister("combo")
        assertEquals("ring", r.healthSource()?.id)
    }

    @Test fun nothingPaired() {
        assertNull(DeviceRegistry().voiceSource())
    }
}
