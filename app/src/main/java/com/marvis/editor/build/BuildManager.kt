package com.marvis.editor.build

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object BuildManager {

    private const val TAG = "BuildManager"
    private var buildJob: Job? = null
    private var ctx: Context? = null

    private lateinit var sdkDir: File
    private lateinit var gradleHome: File
    private lateinit var platformDir: File
    private lateinit var gradleUserHome: File

    fun init(context: Context) {
        if (ctx != null) return
        ctx = context.applicationContext
        sdkDir = File(context.filesDir, "android-toolchain")
        gradleHome = File(sdkDir, "gradle-8.5")
        platformDir = File(sdkDir, "platforms/android-35")
        gradleUserHome = File(context.filesDir, ".gradle")
    }

    fun isToolchainReady() =
        File(platformDir, "android.jar").exists() && File(gradleHome, "bin/gradle").exists()

    fun extractToolchain(onProgress: (String) -> Unit) {
        val c = ctx ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isToolchainReady()) {
                    withContext(Dispatchers.Main) { onProgress("Toolchain already ready") }
                    return@launch
                }
                sdkDir.mkdirs()
                withContext(Dispatchers.Main) { onProgress("Extracting Gradle...") }
                extractDir(c, "gradle-toolchain", sdkDir)
                val pd = File(sdkDir, "platforms/android-35")
                pd.mkdirs()
                withContext(Dispatchers.Main) { onProgress("Extracting platform...") }
                extractDir(c, "android-platform", pd)
                withContext(Dispatchers.Main) { onProgress("Toolchain ready") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onProgress("ERROR: ${e.message}") }
            }
        }
    }

    private fun extractDir(context: Context, assetPath: String, destDir: File) {
        destDir.mkdirs()
        val children = context.assets.list(assetPath) ?: return
        for (child in children) {
            val cp = "$assetPath/$child"
            val cd = File(destDir, child)
            val sub = context.assets.list(cp)
            if (sub != null && sub.isNotEmpty()) {
                cd.mkdirs()
                extractDir(context, cp, cd)
            } else {
                try {
                    cd.parentFile?.mkdirs()
                    context.assets.open(cp).use { it.copyTo(cd.outputStream()) }
                } catch (e: Exception) { Log.w(TAG, "Skip $cp: ${e.message}") }
            }
        }
    }

    fun tryGetTermuxBuildTools(): Boolean {
        val tp = "/data/data/com.termux/files/usr"
        if (!File(tp, "bin/aapt2").exists() || !File(tp, "bin/d8").exists()) return false
        val btDir = File(sdkDir, "build-tools/35.0.0")
        btDir.mkdirs()
        listOf("aapt2", "d8", "zipalign", "apksigner").forEach { tool ->
            val src = File(tp, "bin/$tool")
            if (src.exists()) {
                val dst = File(btDir, tool)
                if (!dst.exists()) {
                    try { Runtime.getRuntime().exec(arrayOf("ln", "-sf", src.absolutePath, dst.absolutePath)).waitFor() }
                    catch (e: Exception) { Log.w(TAG, "link $tool: ${e.message}") }
                }
            }
        }
        return true
    }

    fun showOutputDialog(context: Context, projectDir: File) {
        init(context)
        val c = ctx!!
        val tv = TextView(context).apply {
            setPadding(32, 32, 32, 32); textSize = 12f; setTextIsSelectable(true); text = "Initializing...\n"
        }
        val sv = ScrollView(context); sv.addView(tv)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Gradle Build").setView(sv)
            .setPositiveButton("Close", null)
            .setNegativeButton("Cancel") { _, _ -> cancel() }.create()
        dialog.show()

        if (!tryGetTermuxBuildTools()) {
            tv.append("WARNING: Termux build-tools not found.\nInstall Termux and run:\n  pkg install aapt2 d8 zipalign apksigner\n\n")
        }

        buildJob = CoroutineScope(Dispatchers.IO).launch {
            val gw = File(projectDir, "gradlew")
            if (!gw.exists()) {
                withContext(Dispatchers.Main) { tv.append("ERROR: gradlew not found\n") }
                return@launch
            }
            gw.setExecutable(true, false)
            val env = HashMap(System.getenv())
            env["ANDROID_HOME"] = sdkDir.absolutePath
            env["ANDROID_SDK_ROOT"] = sdkDir.absolutePath
            env["GRADLE_USER_HOME"] = gradleUserHome.absolutePath
            env["GRADLE_HOME"] = gradleHome.absolutePath
            val pb = ProcessBuilder()
                .directory(projectDir)
                .command("sh", gw.absolutePath, "assembleDebug", "--no-daemon", "--stacktrace")
                .redirectErrorStream(true)
            pb.environment().putAll(env)
            val p = pb.start()
            val r = BufferedReader(InputStreamReader(p.inputStream))
            var l: String?
            while (r.readLine().also { l = it } != null) {
                withContext(Dispatchers.Main) { tv.append("$l
") }
            }
            val ec = p.waitFor()
            withContext(Dispatchers.Main) {
                if (ec == 0) {
                    tv.append("\nBUILD SUCCESSFUL\n")
                    val apk = File(projectDir, "app/build/outputs/apk/debug").listFiles { f -> f.name.endsWith(".apk") }?.firstOrNull()
                    if (apk != null) {
                        tv.append("APK: ${apk.name}
")
                        try {
                            val uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", apk)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            c.startActivity(intent)
                        } catch (e: Exception) {
                            Runtime.getRuntime().exec(arrayOf("pm", "install", "-r", apk.absolutePath)).waitFor()
                        }
                    }
                } else {
                    tv.append("\nBUILD FAILED (exit=$ec)\n")
                }
            }
        }
    }

    fun cancel() { buildJob?.cancel() }
}
