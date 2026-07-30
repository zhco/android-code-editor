package com.marvis.editor.build

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object GradleBuilder {

    private var buildJob: Job? = null

    fun build(context: Context, projectDir: File, onOutput: (String) -> Unit) {
        buildJob?.cancel()
        buildJob = CoroutineScope(Dispatchers.IO).launch {
            val gradlew = File(projectDir, "gradlew")
            if (!gradlew.exists()) {
                withContext(Dispatchers.Main) { onOutput("ERROR: gradlew not found in ${projectDir.absolutePath}") }
                return@launch
            }
            gradlew.setExecutable(true)

            val process = ProcessBuilder()
                .directory(projectDir)
                .command("sh", gradlew.absolutePath, "assembleDebug")
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.Main) { onOutput(line!!) }
            }
            val exitCode = process.waitFor()
            withContext(Dispatchers.Main) {
                if (exitCode == 0) {
                    onOutput("\nBUILD SUCCESSFUL")
                    val apk = findApk(projectDir)
                    if (apk != null) {
                        onOutput("APK: ${apk.absolutePath}")
                        installApk(context, apk)
                    } else {
                        onOutput("WARNING: APK not found")
                    }
                } else {
                    onOutput("\nBUILD FAILED (exit=$exitCode)")
                }
            }
        }
    }

    fun cancel() {
        buildJob?.cancel()
    }

    private fun findApk(projectDir: File): File? {
        val debugDir = File(projectDir, "app/build/outputs/apk/debug")
        return debugDir.listFiles { f -> f.name.endsWith(".apk") }?.firstOrNull()
    }

    private fun installApk(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun showOutputDialog(context: Context, projectDir: File) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Gradle Build")
            .setView(TextView(context).apply {
                setPadding(32, 32, 32, 32)
                textSize = 12f
                setTextIsSelectable(true)
            })
            .setPositiveButton("Close", null)
            .setNegativeButton("Cancel") { _, _ -> cancel() }
            .create()

        dialog.show()
        build(context, projectDir) { line ->
            (dialog.findViewById(android.R.id.content) as? TextView)?.append("$line\n")
        }
    }
}
