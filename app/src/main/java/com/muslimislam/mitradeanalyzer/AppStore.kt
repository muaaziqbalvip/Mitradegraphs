package com.muslimislam.mitradeanalyzer

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Persistent local storage: license key, saved user name, backend URL,
 * and a stable per-install device ID used for license device-binding.
 */
object AppStore {
    private const val PREFS = "mi_trade_analyzer_prefs"
    private const val KEY_LICENSE = "license_key"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_BACKEND_URL = "backend_url"
    private const val KEY_DEVICE_ID = "device_id"

    const val DEFAULT_BACKEND_URL = "https://muaaznamtosonahoga1-miaibot.hf.space"

    fun saveLicenseKey(ctx: Context, key: String) {
        prefs(ctx).edit().putString(KEY_LICENSE, key).apply()
    }

    fun loadLicenseKey(ctx: Context): String {
        return prefs(ctx).getString(KEY_LICENSE, "") ?: ""
    }

    fun saveUserName(ctx: Context, name: String) {
        prefs(ctx).edit().putString(KEY_USER_NAME, name).apply()
    }

    fun loadUserName(ctx: Context): String {
        return prefs(ctx).getString(KEY_USER_NAME, "") ?: ""
    }

    fun saveBackendUrl(ctx: Context, url: String) {
        prefs(ctx).edit().putString(KEY_BACKEND_URL, url).apply()
    }

    fun loadBackendUrl(ctx: Context): String {
        return prefs(ctx).getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
    }

    fun logout(ctx: Context) {
        prefs(ctx).edit().remove(KEY_LICENSE).remove(KEY_USER_NAME).apply()
    }

    /**
     * A stable ID for this app install, used to bind a license key to
     * "one device". Derived from Android's Settings.Secure.ANDROID_ID
     * (stable per device+app-signing-key combo) with a per-install random
     * fallback stored in prefs, in case ANDROID_ID is unavailable.
     */
    fun getDeviceId(ctx: Context): String {
        val existing = prefs(ctx).getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing

        val androidId = try {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }

        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            androidId
        } else {
            UUID.randomUUID().toString()
        }

        prefs(ctx).edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
