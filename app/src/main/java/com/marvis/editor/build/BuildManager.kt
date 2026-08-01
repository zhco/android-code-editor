package com.marvis.editor.build

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStreamReader

class BuildManager(private val context: Context) {

    companion object {
        private const val TAG = "BuildManager"
    }

    // SDK extracted to internal storage
    private val sdkDir get() = File(context.filesDir, "android-toolchain")
    private val gradleHome get() = File(sdkDir, "gradle-8.5")
    private val platformDir get() = File(sdkDir, "platforms/android-35")
    private val buildToolsDir get() = File(sdkDir, "build-tools")

    val gradleUserHome get() = File(context.filesDir, ".gradle")
    val projectsDir get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "projects")

    fun isToolchainReady(): Boolean {
        return File(platformDir, "android.jar").exists() &&
               File(gradleHome, "bin/gradle").exists()
    }

    fun extractToolchain(onProgress: (String) -> Unit): Result<Unit> {
        return try {
            if (isToolchainReady()) {
                onProgress("Toolchain already extracted")
                return Result.success(Unit)
            }

            sdkDir.mkdirs()

            // 1. Extract Gradle distribution
            onProgress("Extracting Gradle...")
            extractAssetDir("gradle-toolchain", sdkDir)

            // 2. Extract Android platform
            onProgress("Extracting Android platform...")
            extractAssetDir("android-platform", sdkDir.apply {
                File(this, "platforms/android-35").mkdirs()
            })

            // Move platform files to correct location
            val extractedPlatform = File(sdkDir, "android-platform")
            if (extractedPlatform.exists()) {
                val targetPlatform = File(sdkDir, "platforms/android-35")
                extractedPlatform.listFiles()?.forEach { file ->
                    file.renameTo(File(targetPlatform, file.name))
                }
                extractedPlatform.deleteRecursively()
            }

            // 3. Extract bundled build-tools (no Termux needed)
            onProgress("Extracting build-tools...")
            val btDir = File(sdkDir, "build-tools/35.0.0")
            btDir.mkdirs()
            extractAssetDir("build-tools", btDir)

            // Ensure executables after extraction (aapt/aapt2/aidl/zipalign/d8/apksigner)
            val toolsToFix = listOf("aapt", "aapt2", "aidl", "zipalign", "d8", "apksigner")
            for (tool in toolsToFix) {
                val f = File(btDir, tool)
                if (f.exists()) f.setExecutable(true, false)
            }

            onProgress("Toolchain extracted (build-tools included)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract toolchain", e)
            Result.failure(e)
        }
    }

    private fun extractAssetDir(assetPath: String, destDir: File) {
        destDir.mkdirs()
        val children = context.assets.list(assetPath) ?: return
        for (child in children) {
            val childPath = "$assetPath/$child"
            val childDest = File(destDir, child)
            val sub = context.assets.list(childPath)
            if (sub != null && sub.isNotEmpty()) {
                childDest.mkdirs()
                extractAssetDir(childPath, childDest)
            } else {
                try {
                    context.assets.open(childPath).use { input ->
                        childDest.parentFile?.mkdirs()
                        childDest.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Skip asset: $childPath — ${e.message}")
                }
            }
        }
    }

    suspend fun build(
        projectDir: File,
        task: String = "assembleDebug",
        onOutput: (String) -> Unit
    ): BuildResult = withContext(Dispatchers.IO) {
        val gradlew = File(projectDir, "gradlew")
        if (!gradlew.exists()) {
            return@withContext BuildResult.Failure("gradlew not found in ${projectDir.absolutePath}", -1)
        }
        gradlew.setExecutable(true, false)

        // Build environment
        val env = HashMap(System.getenv())
        env["ANDROID_HOME"] = sdkDir.absolutePath
        env["ANDROID_SDK_ROOT"] = sdkDir.absolutePath
        env["GRADLE_USER_HOME"] = gradleUserHome.absolutePath
        env["GRADLE_HOME"] = gradleHome.absolutePath

        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("sh", gradlew.absolutePath, task, "--no-daemon", "--stacktrace"),
                env.toTypedArray(),
                projectDir
            )

            val reader = InputStreamReader(process.inputStream).buffered()
            val sb = StringBuilder()

            // Use coroutine-based reading
            var line: String?
            var curChar: Int
            val charBuf = CharArray(256)
            while (reader.read(charBuf).also { curChar = it } != -1) {
                val text = String(charBuf, 0, curChar)
                sb.append(text)
                withContext(Dispatchers.Main) { onOutput(text) }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val apk = findApk(projectDir)
                BuildResult.Success(apk, exitCode)
            } else {
                BuildResult.Failure("Build failed (exit=$exitCode)", exitCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Build error", e)
            BuildResult.Failure(e.message ?: "Unknown error", -1)
        }
    }

    private fun findApk(projectDir: File): File? {
        val debugDir = File(projectDir, "app/build/outputs/apk/debug")
        return debugDir.listFiles { f -> f.name.endsWith(".apk") }?.firstOrNull()
    }

    /**
     * Returns true if bundled build-tools are available (extracted from assets).
     * No Termux dependency — all tools shipped inside the APK.
     */
    fun isBuildToolsReady(): Boolean {
        val btDir = File(sdkDir, "build-tools/35.0.0")
        return File(btDir, "aapt2").exists() && File(btDir, "d8").exists()
    }
}

sealed class BuildResult {
    val isSuccess get() = this is Success
    data class Success(val apkFile: File?, val exitCode: Int) : BuildResult()
    data class Failure(val message: String, val exitCode: Int) : BuildResult()
}

// Helper to convert Map to Array for ProcessBuilder/Runtime.exec
private fun Map<String, String>.toTypedArray(): Array<String> {
    return entries.map { "${it.key}=${it.value}" }.toTypedArray()
}
