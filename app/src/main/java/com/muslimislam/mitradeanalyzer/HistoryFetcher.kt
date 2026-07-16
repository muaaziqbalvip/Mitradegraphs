package com.muslimislam.mitradeanalyzer

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches this device's license login history for the Account screen, by
 * re-calling /license/verify (same device, so it succeeds harmlessly) and
 * reading the "history" array it returns.
 */
object HistoryFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetchHistory(context: Context, backendUrl: String, key: String, onResult: (JSONArray?) -> Unit) {
        val url = backendUrl.trimEnd('/') + "/license/verify"
        val deviceId = AppStore.getDeviceId(context)

        val body = JSONObject().apply {
            put("key", key)
            put("device_id", deviceId)
        }
        val requestBody = body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        Thread {
            val history = try {
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string() ?: ""
                    val json = JSONObject(text)
                    json.optJSONArray("history")
                }
            } catch (e: Exception) {
                null
            }
            android.os.Handler(context.mainLooper).post { onResult(history) }
        }.start()
    }
}
