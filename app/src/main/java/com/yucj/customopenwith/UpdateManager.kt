package com.yucj.customopenwith

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class ReleaseInfo(
    /** Version string without the "v" prefix, e.g. "1.0.7". */
    val version: String,
    val apkUrl: String,
)

/**
 * Checks GitHub Releases for a newer build, downloads the APK, and hands it
 * to the system installer (no Play Store involved). Callers run the blocking
 * functions off the main thread.
 */
object UpdateManager {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/YuCJ/CustomOpenWith/releases/latest"

    /** Returns the latest release if it is newer than the installed build, else null. Blocking. */
    fun checkForUpdate(): ReleaseInfo? {
        val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        val body = try {
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }

        val json = JSONObject(body)
        val version = json.getString("tag_name").removePrefix("v")
        val assets = json.getJSONArray("assets")
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                apkUrl = asset.getString("browser_download_url")
                break
            }
        }
        if (apkUrl == null) return null
        return if (isNewer(version, BuildConfig.VERSION_NAME)) ReleaseInfo(version, apkUrl) else null
    }

    /** True when remote is newer than local. A -dev suffix sorts before the same release version. */
    fun isNewer(remote: String, local: String): Boolean {
        val localIsDev = "-" in local
        val r = remote.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val l = local.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return localIsDev
    }

    /** Downloads the APK into the app cache. Blocking. */
    fun downloadApk(context: Context, url: String, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val outFile = File(dir, "update.apk")

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        try {
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) onProgress((copied * 100 / total).toInt())
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return outFile
    }

    /**
     * Opens the system installer. The first run sends the user to the
     * "install unknown apps" permission screen. In-place updates require the
     * same signing key as the installed build (hence the fixed CI keystore).
     */
    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
