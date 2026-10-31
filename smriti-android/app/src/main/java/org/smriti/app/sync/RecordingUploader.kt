package org.smriti.app.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import org.smriti.app.device.RecordingRef
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Sends one recording to our transcriber and waits for the Memos note.
 * Safe to retry: the server de-duplicates on (user, device, recording_id).
 */
class RecordingUploader(
    private val baseUrl: String,
    private val ingestToken: String,
    private val memosToken: String,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).writeTimeout(5, TimeUnit.MINUTES).build(),
) {
    data class Outcome(val jobId: String, val status: String, val memoName: String?, val error: String?)

    suspend fun upload(file: File, ref: RecordingRef, deviceKind: String, language: String? = null): Outcome =
        withContext(Dispatchers.IO) {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .addFormDataPart("recording_id", ref.recordingId)
                .addFormDataPart("device", deviceKind)
                .addFormDataPart("mode", if (ref.isMeeting) "meeting" else "note")
                .addFormDataPart("recorded_at", ref.startedAtEpochMs?.let { Instant.ofEpochMilli(it).toString() } ?: "")
                .addFormDataPart("language", language ?: "")
                .build()
            val req = Request.Builder().url("$baseUrl/v1/recordings").post(body)
                .header("X-Ingest-Token", ingestToken).header("X-Memos-Token", memosToken).build()
            val jobId = http.newCall(req).execute().use { r ->
                check(r.isSuccessful) { "upload failed: HTTP ${r.code}" }
                JSONObject(r.body!!.string()).getString("job_id")
            }
            poll(jobId)
        }

    private suspend fun poll(jobId: String): Outcome {
        repeat(240) { // up to ~20 minutes for a long meeting
            val st = http.newCall(Request.Builder().url("$baseUrl/v1/jobs/$jobId").build()).execute().use {
                JSONObject(it.body!!.string())
            }
            when (val status = st.optString("status")) {
                "done" -> return Outcome(jobId, status, st.getJSONObject("result").optString("memo_name"), null)
                "failed" -> return Outcome(jobId, status, null, st.optString("error"))
            }
            delay(5_000)
        }
        return Outcome(jobId, "timeout", null, "server still processing")
    }
}
