package org.smriti.app.device

/**
 * Decides which paired device supplies which job.
 *
 * Today: ES100 → voice, ChipletRing → health.
 * Later: one ring with both capabilities → both jobs, automatically.
 * If two devices can do the same job, the one the user marked preferred wins,
 * otherwise the most recently paired.
 */
class DeviceRegistry {
    private data class Entry(val device: Wearable, val caps: Set<Capability>, val pairedOrder: Int)

    private val entries = mutableMapOf<String, Entry>()
    private val preferred = mutableMapOf<Capability, String>()
    private var counter = 0

    fun register(device: Wearable, caps: Set<Capability>) {
        entries[device.id] = Entry(device, caps, counter++)
    }

    fun unregister(deviceId: String) {
        entries.remove(deviceId)
        preferred.values.removeAll { it == deviceId }
    }

    fun prefer(cap: Capability, deviceId: String) {
        preferred[cap] = deviceId
    }

    fun sourceFor(cap: Capability): Wearable? {
        preferred[cap]?.let { id -> entries[id]?.takeIf { cap in it.caps }?.let { return it.device } }
        return entries.values.filter { cap in it.caps }.maxByOrNull { it.pairedOrder }?.device
    }

    fun voiceSource(): Wearable? = sourceFor(Capability.VOICE_RECORDINGS)
    fun healthSource(): Wearable? = sourceFor(Capability.HEALTH_HISTORY)
    fun all(): List<Wearable> = entries.values.sortedBy { it.pairedOrder }.map { it.device }
}
