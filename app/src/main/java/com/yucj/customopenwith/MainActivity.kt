package com.yucj.customopenwith
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handleIncomingUrlIntent()
        displayLogs()
    }

    private fun displayLogs() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_logs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val logs = LogUtils.getUrlLogs(this)
        val logEntries = logs.map { log ->
            val parts = log.split(" - ")
            LogEntry(parts[1], parts[0])
        }
        recyclerView.adapter = LogAdapter(logEntries) { url ->
            showBrowserChooser(url)
        }
    }

    private fun handleIncomingUrlIntent() {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val url = intent.dataString ?: return
                showBrowserChooser(url)
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                    showBrowserChooser(sharedText)
                } else {
                    // Handle cases where shared text is not a URL (optional)
                }
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun showBrowserChooser(url: String) {
        LogUtils.logUrl(this, url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val browserOptions = mutableListOf<CharSequence>()
        val browserIntents = mutableListOf<Intent>()

        for (activity in activities) {
            if (activity.activityInfo.packageName != packageName) {
                val browserIntent = Intent(intent)
                browserIntent.setPackage(activity.activityInfo.packageName)
                browserOptions.add(activity.loadLabel(packageManager))
                browserIntents.add(browserIntent)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Choose a browser")
            .setItems(browserOptions.toTypedArray()) { _, which ->
                startActivity(browserIntents[which])
            }
            .show()
    }
}