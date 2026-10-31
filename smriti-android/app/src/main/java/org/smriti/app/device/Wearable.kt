package org.smriti.app.device

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * The swappable device layer.
 *
 * Every wearable — the ES100 band, the ChipletRing, or a future voice+health
 * ring — implements this one interface and says which [Capability]s it has.
 * The rest of the app never talks to a vendor SDK directly; it asks
 * [DeviceRegistry] "who gives me voice?" and "who gives me health?".
 * A single device that does both simply answers both questions. No rebuild.
 */
enum class Capability { VOICE_RECORDINGS, HEALTH_HISTORY, RAW_PPG, RAW_FILES, SKIN_TEMPERATURE }

interface Wearable {
    /** Stable id, e.g. "chipletring:AA:BB:CC:DD:EE:FF". */
    val id: String
    val vendor: String
    val state: Flow<ConnectionState>

    suspend fun connect(): DeviceInfo
    suspend fun disconnect()

    /** What this particular unit, on its current firmware, can actually do. */
    suspend fun capabilities(): Set<Capability>

    // --- voice --------------------------------------------------------------
    suspend fun listRecordings(): List<RecordingRef> = unsupported("listRecordings")
    suspend fun downloadRecording(ref: RecordingRef, into: File): File = unsupported("downloadRecording")
    suspend fun deleteRecording(ref: RecordingRef): Unit = unsupported("deleteRecording")

    // --- health -------------------------------------------------------------
    /** Records newer than [sinceEpochMs] (0 = everything on the device). */
    suspend fun getHealthHistory(sinceEpochMs: Long): List<HealthRecord> = unsupported("getHealthHistory")

    /** Raw optical sensor samples for [seconds]. Emits batches as they arrive. */
    fun streamRawPPG(seconds: Int, sampleHz: Int): Flow<PpgBatch> = unsupported("streamRawPPG")

    private fun unsupported(what: String): Nothing =
        throw UnsupportedOperationException("$vendor does not support $what")
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val info: DeviceInfo) : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}

data class DeviceInfo(
    val firmwareVersion: String,
    val hardwareVersion: String,
    val batteryPercent: Int?,
    /** Everything the device reported about itself, for the findings log. */
    val raw: Map<String, String> = emptyMap(),
)

data class RecordingRef(
    val deviceId: String,
    /** The device's own id for the recording; also used as the upload's recording_id. */
    val recordingId: String,
    val startedAtEpochMs: Long?,
    val durationSec: Int?,
    val sizeBytes: Long?,
    val isMeeting: Boolean = false,
)

/** One history row. Null means the device didn't measure it at that time. */
data class HealthRecord(
    val deviceId: String,
    val epochMs: Long,
    val heartRate: Int?,
    val spo2: Int?,
    val hrvMs: Int?,
    val stress: Int?,
    /** Degrees C. */
    val skinTempC: Double?,
    val steps: Int?,
    val sleepStage: Int?,
    val respiratoryRate: Double?,
    /** Beat-to-beat intervals in ms, when the device sends them. */
    val rrIntervalsMs: List<Int> = emptyList(),
)

data class PpgBatch(
    val deviceId: String,
    val sequence: Int,
    /** Each sample: channel name → value, e.g. {"green": 51234, "ir": 40211, "acc_x": -12}. */
    val samples: List<Map<String, Long>>,
)
