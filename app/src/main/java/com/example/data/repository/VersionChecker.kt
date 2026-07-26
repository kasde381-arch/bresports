package com.example.data.repository

import com.example.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object VersionChecker {
    // Default Hosted GitHub JSON endpoint for Version Checking
    const val DEFAULT_VERSION_JSON_URL = "https://raw.githubusercontent.com/aistudio-build/app-release-config/main/version.json"

    suspend fun checkRemoteVersion(
        configUrl: String = DEFAULT_VERSION_JSON_URL,
        currentVersionCode: Int = 1
    ): AppUpdateInfo = withContext(Dispatchers.IO) {
        val targetUrl = configUrl.ifBlank { DEFAULT_VERSION_JSON_URL }
        try {
            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Android-App-VersionChecker/1.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                
                val versionCode = json.optInt("versionCode", json.optInt("latestVersionCode", currentVersionCode))
                val versionName = json.optString("versionName", json.optString("latestVersionName", "2.0.0"))
                val minSupportedCode = json.optInt("minSupportedVersionCode", 1)
                val apkUrl = json.optString("apkUrl", "https://github.com/kasde381-arch/bresports/releases/download/v1.0.0/app-release.apk")
                val releaseNotes = json.optString("releaseNotes", "• Critical tournament lobby stability fixes\n• Instant wallet deposit & coin sync improvements\n• Anti-cheat integration")
                val isForceUpdate = json.optBoolean("isForceUpdate", versionCode > minSupportedCode)

                AppUpdateInfo(
                    latestVersionCode = versionCode,
                    latestVersionName = versionName,
                    minSupportedVersionCode = minSupportedCode,
                    apkUrl = apkUrl,
                    releaseNotes = releaseNotes,
                    isForceUpdate = isForceUpdate,
                    checkStatus = "SUCCESS",
                    errorMessage = null
                )
            } else {
                AppUpdateInfo(
                    latestVersionCode = currentVersionCode,
                    checkStatus = "ERROR",
                    errorMessage = "Server returned status HTTP ${connection.responseCode}"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return error status with detailed message
            AppUpdateInfo(
                latestVersionCode = currentVersionCode,
                checkStatus = "ERROR",
                errorMessage = e.localizedMessage ?: "Failed to connect to update server"
            )
        }
    }
}
