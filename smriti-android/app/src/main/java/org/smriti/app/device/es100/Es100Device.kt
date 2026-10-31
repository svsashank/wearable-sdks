package org.smriti.app.device.es100

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.smriti.app.device.Capability
import org.smriti.app.device.ConnectionState
import org.smriti.app.device.DeviceInfo
import org.smriti.app.device.RecordingRef
import org.smriti.app.device.Wearable
import java.io.File

/**
 * ES100 band (Actions ATS3085L) over the vendor's "Ute Watch SDK" v1.3.5.
 *
 * STATUS: shape only. The Ute SDK isn't in any repo this build could read
 * (svsashank/wearable-sdks is still empty), so the calls below are TODOs that
 * point at the right part of the vendor manual and demo. Fill them in after
 * milestone test 2, by copying what AIRecorderActivity in the vendor demo does.
 */
class Es100Device(
    private val context: Context,
    private val bt: BluetoothDevice,
) : Wearable {
    override val id = "es100:${bt.address}"
    override val vendor = "ES100"
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: Flow<ConnectionState> = _state

    override suspend fun connect(): DeviceInfo {
        // TODO(milestone-2): manual "connection flow" chapter + demo's connect code.
        TODO("Ute SDK connect")
    }

    override suspend fun disconnect() { TODO("Ute SDK disconnect") }

    override suspend fun capabilities() = setOf(Capability.VOICE_RECORDINGS)

    override suspend fun listRecordings(): List<RecordingRef> {
        // TODO(milestone-2): manual §2.104 "AI recording / meeting minutes":
        // the call that lists recordings stored on the band (id, start time, length, size).
        TODO("Ute SDK list recordings")
    }

    override suspend fun downloadRecording(ref: RecordingRef, into: File): File {
        // TODO(milestone-2): §2.104 + AIRecorderActivity: transfer one recording to the phone.
        // Write it to `into` unchanged. The server decodes any format (see tools/probe_audio.py).
        TODO("Ute SDK download recording")
    }

    override suspend fun deleteRecording(ref: RecordingRef) {
        // Only after the server confirms the note was saved.
        TODO("Ute SDK delete recording")
    }
}
