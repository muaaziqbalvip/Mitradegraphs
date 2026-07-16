package com.muslimislam.mitradeanalyzer

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks to the license endpoints on the backend (/license/verify).
 * One key = one device: the backend binds a key to the first device that
 * verifies successfully, and rejects verification from any other device.
 */
object LicenseClient {

    data class VerifyResult(
        val valid: Boolean,
        val message: String,
        val userName: String?
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun verify(
        context: Context,
        backendUrl: String,
        licenseKey: String,
        onResult: (VerifyResult) -> Unit
    ) {
        val url = backendUrl.trimEnd('/') + "/license/verify"
        val deviceId = AppStore.getDeviceId(context)

        val body = JSONObject().apply {
            put("key", licenseKey.trim())
            put("device_id", deviceId)
        }
        val requestBody = body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        Thread {
            val result = try {
                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: ""
                    val json = try { JSONObject(responseText) } catch (_: Exception) { JSONObject() }
                    VerifyResult(
                        valid = json.optBoolean("valid", false),
                        message = json.optString("message", "Unknown error"),
                        userName = json.optString("name", null)
                    )
                }
            } catch (e: Exception) {
                VerifyResult(valid = false, message = "Network error: ${e.message}", userName = null)
            }

            android.os.Handler(context.mainLooper).post {
                onResult(result)
            }
        }.start()
    }
}
