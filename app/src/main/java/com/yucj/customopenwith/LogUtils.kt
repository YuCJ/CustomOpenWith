package com.yucj.customopenwith

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {

    private const val LOG_FILE_NAME = "url_logs.txt"

    fun logUrl(context: Context, url: String) {
        val logFile = File(context.cacheDir, LOG_FILE_NAME)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newLogEntry = "$timestamp - $url"

        try {
            // Read existing logs
            val existingLogs = if (logFile.exists()) logFile.readLines() else emptyList()

            // Remove previous entries with the same URL
            val updatedLogs = existingLogs.filterNot { it.substringAfter(" - ") == url }

            // Add the new log entry
            val finalLogs = newLogEntry + updatedLogs

            // Write back to the file
            FileWriter(logFile, false).use { writer ->
                finalLogs.forEach { entry ->
                    writer.append(entry).append("\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getUrlLogs(context: Context): List<String> {
        val logFile = File(context.cacheDir, LOG_FILE_NAME)
        if (!logFile.exists()) {
            return emptyList()
        }

        return logFile.readLines()
    }
}
