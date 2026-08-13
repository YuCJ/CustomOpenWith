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
        findViewById<android.widget.Button>(R.id.button_check_update).setOnClickListener {
            checkForUpdate()
        }
    }

    private fun checkForUpdate() {
        Thread {
            val result = runCatching { UpdateManager.checkForUpdate() }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.fold(
                    onSuccess = { info ->
                        if (info == null) {
                            android.widget.Toast.makeText(
                                this,
                                "Already up to date (${BuildConfig.VERSION_NAME})",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            AlertDialog.Builder(this)
                                .setTitle("Update available")
                                .setMessage("Version ${info.version} is available (installed: ${BuildConfig.VERSION_NAME}).")
                                .setPositiveButton("Download & install") { _, _ -> downloadAndInstall(info) }
                                .setNegativeButton("Later", null)
                                .show()
                        }
                    },
                    onFailure = { e ->
                        android.widget.Toast.makeText(
                            this,
                            "Update check failed: ${e.message}",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    },
                )
            }
        }.start()
    }

    private fun downloadAndInstall(info: ReleaseInfo) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Downloading update…")
            .setMessage("0%")
            .setCancelable(false)
            .create()
        dialog.show()
        Thread {
            try {
                val apk = UpdateManager.downloadApk(this, info.apkUrl) { progress ->
                    runOnUiThread { dialog.setMessage("$progress%") }
                }
                runOnUiThread {
                    dialog.dismiss()
                    UpdateManager.installApk(this, apk)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dialog.dismiss()
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    AlertDialog.Builder(this)
                        .setTitle("Update failed")
                        .setMessage(e.message ?: e.toString())
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }.start()
    }

    private fun displayLogs() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_logs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val logs = LogUtils.getUrlLogs(this)
        val logEntries = logs.map { log ->
            val parts = log.split(" - ")
            LogEntry(parts[1], parts[0])
        }
        recyclerView.adapter = LogAdapter(
            logEntries,
            onUrlClick = { url -> showBrowserChooser(url) },
            onUrlLongClick = { url -> copyToClipboard(url) },
        )
    }

    private fun copyToClipboard(url: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", url))
        // Android 13+ shows its own clipboard confirmation overlay
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            android.widget.Toast.makeText(this, "Copied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleIncomingUrlIntent() {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val url = UrlCleaner.clean(intent.dataString ?: return)
                showBrowserChooser(url)
                LogUtils.logUrl(this, url)
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                    val url = UrlCleaner.clean(sharedText)
                    showBrowserChooser(url)
                    LogUtils.logUrl(this, url)
                } else {
                    // Handle cases where shared text is not a URL (optional)
                }
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun showBrowserChooser(url: String) {
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