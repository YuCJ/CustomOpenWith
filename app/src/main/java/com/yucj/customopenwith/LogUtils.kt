package com.yucj.customopenwith

import android.content.Context
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtils {

    private const val LOG_FILE_NAME = "url_logs.txt"
    private const val MAX_ENTRIES = 200

    /**
     * History lives in filesDir; cacheDir can be wiped by the system at any
     * time. Older builds wrote to cacheDir, so migrate that file if present.
     */
    private fun logFile(context: Context): File {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (!file.exists()) {
            val legacy = File(context.cacheDir, LOG_FILE_NAME)
            if (legacy.exists()) {
                runCatching {
                    legacy.copyTo(file, overwrite = false)
                    legacy.delete()
                }
            }
        }
        return file
    }

    fun logUrl(context: Context, url: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        try {
            val file = logFile(context)
            val existing = if (file.exists()) file.readLines() else emptyList()
            val updated = (listOf("$timestamp - $url") +
                existing.filterNot { it.substringAfter(" - ") == url })
                .take(MAX_ENTRIES)
            file.writeText(updated.joinToString("\n", postfix = "\n"))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getUrlLogs(context: Context): List<String> {
        val file = logFile(context)
        if (!file.exists()) return emptyList()
        return file.readLines()
    }
}
