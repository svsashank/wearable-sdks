package org.smriti.app.device.chipletring

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.lm.sdk.LmAPILite
import com.lm.sdk.inter.IRealTimePPGListener
import com.lm.sdk.lmApiInter.IBindConnectRefreshListenerLite
import com.lm.sdk.lmApiInter.IHistoryListenerLite
import com.lm.sdk.lmApiInter.IResponseListenerLite
import com.lm.sdk.mode.HistoryDataBean
import com.lm.sdk.mode.SystemControlLiteBean
import com.lm.sdk.utils.BLEUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.smriti.app.device.Capability
import org.smriti.app.device.CommandQueue
import org.smriti.app.device.ConnectionState
import org.smriti.app.device.DeviceInfo
import org.smriti.app.device.HealthRecord
import org.smriti.app.device.PpgBatch
import org.smriti.app.device.Wearable

/**
 * ChipletRing (Bravechip / Yongxin) over the vendor's ChipletRing-1.7.8 AAR.
 *
 * Calls verified against the AAR's classes on 31 Oct 2026:
 *   connect          BLEUtils.connectLockByBLE + IResponseListenerLite.lmBleConnectionSucceeded
 *   device info +    LmAPILite.APP_CONNECT(0, …) → SystemControlLiteBean (firmware, hardware,
 *   capability flags   battery, heartRate/blood/variability/temperature/microphone/fileSystem flags)
 *   history          LmAPILite.READ_HISTORY_AUTO (unread) / READ_HISTORY(1, 0) (everything)
 *   raw PPG          LmAPILite.START_REAL_TIME_PPG(seconds, hz, green, ir, red, progress, wave)
 *
 * Never called (vendor cloud): createToken, *_UPDATE_TO_SERVER, OtaApi.*, GoMore, getSleep*FromService.
 */
class ChipletRingDevice(
    private val context: Context,
    private val bt: BluetoothDevice,
) : Wearable {
    override val id = "chipletring:${bt.address}"
    override val vendor = "ChipletRing"
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: Flow<ConnectionState> = _state
    private val queue = CommandQueue(gapMs = 300)
    private var lastInfo: SystemControlLiteBean? = null

    private val bleListener = object : IResponseListenerLite {
        override fun lmBleConnecting(code: Int) { _state.value = ConnectionState.Connecting }
        override fun lmBleConnectionSucceeded(code: Int) { linkUp.complete(Unit) }
        override fun lmBleConnectionFailed(code: Int) {
            _state.value = ConnectionState.Failed("BLE connection failed ($code)")
            linkUp.completeExceptionally(IllegalStateException("BLE connection failed ($code)"))
        }
        override fun timeOut(msg: String?) {}
        override fun saveData(data: String?) {}
    }
    private var linkUp = CompletableDeferred<Unit>()

    override suspend fun connect(): DeviceInfo {
        linkUp = CompletableDeferred()
        _state.value = ConnectionState.Connecting
        LmAPILite.addWLSCmdListener(context, bleListener)
        BLEUtils.connectLockByBLE(context, bt)
        withTimeout(20_000) { linkUp.await() }
        val bean = queue.send { appConnect() }
        lastInfo = bean
        return bean.toInfo().also { _state.value = ConnectionState.Connected(it) }
    }

    private suspend fun appConnect(): SystemControlLiteBean {
        val done = CompletableDeferred<SystemControlLiteBean>()
        LmAPILite.APP_CONNECT(0, object : IBindConnectRefreshListenerLite {
            override fun appBind(b: SystemControlLiteBean) {}
            override fun appConnect(b: SystemControlLiteBean) { done.complete(b) }
            override fun appRefresh(b: SystemControlLiteBean) {}
        })
        return done.await()
    }

    override suspend fun disconnect() {
        BLEUtils.disconnectBLE(context)
        LmAPILite.removeWLSCmdListener(context)
        _state.value = ConnectionState.Disconnected
    }

    override suspend fun capabilities(): Set<Capability> {
        val b = lastInfo ?: error("connect() first")
        return buildSet {
            if (b.heartRate.toInt() != 0 || b.blood.toInt() != 0) add(Capability.HEALTH_HISTORY)
            if (b.temperature.toInt() != 0) add(Capability.SKIN_TEMPERATURE)
            if (b.fileSystem != 0) add(Capability.RAW_FILES)
            if (b.microphone.toInt() != 0) add(Capability.VOICE_RECORDINGS) // only on voice firmware
            // The SDK has no flag for raw PPG ("for specific rings"); milestone test decides.
            if (RAW_PPG_CONFIRMED) add(Capability.RAW_PPG)
        }
    }

    override suspend fun getHealthHistory(sinceEpochMs: Long): List<HealthRecord> = queue.send(timeoutMs = 120_000) {
        val rows = mutableListOf<HealthRecord>()
        val done = CompletableDeferred<Unit>()
        val l = object : IHistoryListenerLite {
            override fun error(code: Int) { done.completeExceptionally(IllegalStateException("history error $code")) }
            override fun success() { done.complete(Unit) }
            override fun progress(p: Double, b: HistoryDataBean) { b.toRecord()?.let { if (it.epochMs > sinceEpochMs) rows += it } }
            override fun clearHistory() {}
            override fun noNewDataAvailable() { done.complete(Unit) }
        }
        if (sinceEpochMs == 0L) LmAPILite.READ_HISTORY(1, 0, l) else LmAPILite.READ_HISTORY_AUTO(l)
        done.await()
        rows
    }

    override fun streamRawPPG(seconds: Int, sampleHz: Int): Flow<PpgBatch> = callbackFlow {
        LmAPILite.START_REAL_TIME_PPG(seconds, sampleHz, LED_GREEN, LED_IR, LED_RED, 1, 1,
            object : IRealTimePPGListener {
                override fun time(t: Long, zone: Int) {}
                override fun progress(p: Int) {}
                override fun RRIData(n: Int, bytes: ByteArray?) {}
                override fun result(a: Int, b: Int, c: Int, d: Int) { close() }
                override fun waveformData(seq: Int, number: Int, wave: MutableList<Array<String>>) {
                    // Column meaning isn't documented; milestone test 1 records it. Until then: ch0..chN.
                    val samples = wave.map { row -> row.withIndex().associate { (i, v) -> "ch$i" to (v.toLongOrNull() ?: 0L) } }
                    trySend(PpgBatch(id, seq, samples))
                }
            })
        awaitClose { LmAPILite.STOP_REAL_TIME_PPG() }
    }

    private fun SystemControlLiteBean.toInfo() = DeviceInfo(
        firmwareVersion = firmwareVersion.orEmpty(),
        hardwareVersion = hardwareVersion.orEmpty(),
        batteryPercent = battery,
        raw = mapOf(
            "heartRate" to heartRate.toString(), "spo2" to blood.toString(), "hrv" to variability.toString(),
            "stress" to pressure.toString(), "temperature" to temperature.toString(),
            "microphone" to microphone.toString(), "fileSystem" to fileSystem.toString(),
            "collectionIntervalMin" to collectionInterval.toString(),
        ),
    )

    private fun HistoryDataBean.toRecord(): HealthRecord? {
        if (time <= 0) return null
        return HealthRecord(
            deviceId = this@ChipletRingDevice.id,  // the bean has its own `id` (a DB row Long)
            epochMs = if (time < 10_000_000_000L) time * 1000 else time, // SDK sends seconds; be safe
            heartRate = heartRate.takeIf { it in 25..240 },
            spo2 = bloodOxygen.takeIf { it in 70..100 },
            hrvMs = heartRateVariability.takeIf { it > 0 },
            stress = stressIndex.takeIf { it > 0 },
            skinTempC = temperature.takeIf { it > 0 }?.let { it / TEMP_DIVISOR },
            steps = stepCount.takeIf { it >= 0 },
            sleepStage = sleepType,
            respiratoryRate = respiratoryRate?.toDoubleOrNull(),
            rrIntervalsMs = rrBytes?.let(::decodeRr) ?: emptyList(),
        )
    }

    companion object {
        // Defaults from the vendor demo. LED current = value × 392 µA, max 50.
        const val LED_GREEN = 20; const val LED_IR = 20; const val LED_RED = 20
        // TODO(milestone-1): set from findings. Demo shows temperature as an int; confirm
        // whether it is hundredths of °C by comparing with the "测量温度" (measure temp) button.
        const val TEMP_DIVISOR = 100.0
        // TODO(milestone-1): flip to true if real-time PPG streams on our 2.2.4 samples.
        const val RAW_PPG_CONFIRMED = false

        /** RR intervals arrive as little-endian 16-bit ms values. Verify against findings. */
        fun decodeRr(b: ByteArray): List<Int> =
            (0 until b.size / 2).map { i -> (b[2 * i].toInt() and 0xFF) or ((b[2 * i + 1].toInt() and 0xFF) shl 8) }
                .filter { it in 250..2500 }
    }
}
