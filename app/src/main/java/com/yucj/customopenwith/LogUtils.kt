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
        val logEntry = "$timestamp - $url\n"

        try {
            FileWriter(logFile, true).use { writer ->
                writer.append(logEntry)
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
