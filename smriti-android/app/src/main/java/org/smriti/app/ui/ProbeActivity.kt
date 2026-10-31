package org.smriti.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.smriti.app.device.chipletring.ChipletRingDevice
import java.io.File
import java.time.LocalDateTime

/**
 * Milestone-1 ring probe in plain English. It runs the same checks as the
 * vendor demo, but writes every result to a log file you can send back.
 * (Second path for test 1; the vendor demo remains the reference.)
 */
class ProbeActivity : AppCompatActivity() {
    private lateinit var out: TextView
    private val log by lazy { File(getExternalFilesDir(null), "ring-probe-${System.currentTimeMillis()}.txt") }
    private var ring: ChipletRingDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        out = TextView(this).apply { setTextIsSelectable(true) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun btn(label: String, action: suspend () -> Unit) = col.addView(Button(this).apply {
            text = label
            setOnClickListener { lifecycleScope.launch { runCatching { action() }.onFailure { say("FAILED: $it") } } }
        })
        btn("1. Find ring and connect") { scanAndConnect() }
        btn("2. Show capabilities") { say("capabilities: ${ring!!.capabilities()}") }
        btn("3. Read ALL history") { history() }
        btn("4. Raw PPG for 30 s") { ppg() }
        btn("Disconnect") { ring?.disconnect(); say("disconnected") }
        col.addView(out)
        setContentView(ScrollView(this).apply { addView(col) })
        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS), 1)
        say("Log file: ${log.absolutePath}")
    }

    private fun say(s: String) {
        val line = "${LocalDateTime.now()}  $s"
        runOnUiThread { out.append(line + "\n") }
        log.appendText(line + "\n")
    }

    @SuppressLint("MissingPermission")
    private suspend fun scanAndConnect() {
        val scanner = getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        say("scanning 8 s… hold the ring near the phone")
        val found = mutableMapOf<String, ScanResult>()
        val cb = object : ScanCallback() {
            override fun onScanResult(type: Int, r: ScanResult) { found[r.device.address] = r }
        }
        scanner.startScan(cb); kotlinx.coroutines.delay(8_000); scanner.stopScan(cb)
        found.values.sortedByDescending { it.rssi }.forEach { say("  seen ${it.device.name} ${it.device.address} rssi=${it.rssi}") }
        // Ring names seen so far contain these; adjust after first scan if needed.
        val pick = found.values.filter { (it.device.name ?: "").contains(Regex("ring|BCL|LM|Chiplet", RegexOption.IGNORE_CASE)) }
            .maxByOrNull { it.rssi } ?: found.values.maxByOrNull { it.rssi } ?: return say("no devices found")
        say("connecting to ${pick.device.name} ${pick.device.address}")
        ring = ChipletRingDevice(this, pick.device)
        val info = ring!!.connect()
        say("FIRMWARE=${info.firmwareVersion} HARDWARE=${info.hardwareVersion} BATTERY=${info.batteryPercent}%")
        say("flags=${info.raw}")
    }

    private suspend fun history() {
        val rows = ring!!.getHealthHistory(0)
        say("history rows: ${rows.size}")
        fun count(f: (org.smriti.app.device.HealthRecord) -> Any?) = rows.count { f(it) != null }
        say("  with HR=${count { it.heartRate }} SpO2=${count { it.spo2 }} HRV=${count { it.hrvMs }} " +
            "temp=${count { it.skinTempC }} RR=${rows.count { it.rrIntervalsMs.isNotEmpty() }}")
        rows.takeLast(10).forEach { say("  $it") }
    }

    private suspend fun ppg() {
        say("raw PPG: keep the ring on and your hand still for 30 s")
        val batches = ring!!.streamRawPPG(30, 25).toList()
        say("PPG batches=${batches.size} samples=${batches.sumOf { it.samples.size }}")
        batches.take(3).forEach { say("  seq=${it.sequence} first=${it.samples.firstOrNull()}") }
    }
}
