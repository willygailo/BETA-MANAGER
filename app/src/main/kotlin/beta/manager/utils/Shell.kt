package beta.manager.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class RootType {
    MAGISK, KERNELSU, APATCH, SU, NONE, SHIZUKU
}

object Shell {

    sealed class Result {
        data class Success(val output: String, val exitCode: Int = 0) : Result()
        data class Error(val message: String, val exitCode: Int = -1) : Result()
    }

    private var debugMode = false

    fun setDebugMode(enabled: Boolean) { debugMode = enabled }

    suspend fun execute(command: String, timeout: Long = 30000L, useRoot: Boolean = false): Result = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val shellCmd = if (useRoot) {
                "su -c '$command'"
            } else {
                command
            }
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(arrayOf("sh", "-c", shellCmd))

            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val stdoutReader = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdout.appendLine(line)
                    }
                }
            }

            val stderrReader = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderr.appendLine(line)
                    }
                }
            }

            stdoutReader.start()
            stderrReader.start()

            val finished = process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext Result.Error("Command timed out after ${timeout}ms")
            }

            stdoutReader.join(1000)
            stderrReader.join(1000)

            val exitCode = process.exitValue()
            val output = stdout.toString().trim()
            val elapsed = System.currentTimeMillis() - startTime

            if (debugMode) {
                android.util.Log.d("Shell", "cmd: $command, exit: $exitCode, time: ${elapsed}ms")
            }

            if (exitCode == 0) {
                Result.Success(output)
            } else {
                Result.Error(
                    message = stderr.toString().trim().ifEmpty { output },
                    exitCode = exitCode
                )
            }
        } catch (e: Exception) {
            if (debugMode) android.util.Log.e("Shell", "Error: ${e.message}")
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            line == "root"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun detectRootType(): RootType = withContext(Dispatchers.IO) {
        return@withContext when {
            checkMagisk() -> RootType.MAGISK
            checkKernelSU() -> RootType.KERNELSU
            checkAPatch() -> RootType.APATCH
            isRootAvailable() -> RootType.SU
            else -> RootType.NONE
        }
    }

    private suspend fun checkMagisk(): Boolean {
        return when (execute("magisk -v 2>/dev/null")) {
            is Result.Success -> true
            is Result.Error -> false
        }
    }

    private suspend fun checkKernelSU(): Boolean {
        return when (execute("ksud --version 2>/dev/null")) {
            is Result.Success -> true
            is Result.Error -> false
        }
    }

    private suspend fun checkAPatch(): Boolean {
        return when (execute("apd --version 2>/dev/null")) {
            is Result.Success -> true
            is Result.Error -> false
        }
    }

    suspend fun getMagiskVersion(): String = withContext(Dispatchers.IO) {
        when (val res = execute("magisk -v 2>/dev/null")) {
            is Result.Success -> res.output
            is Result.Error -> "N/A"
        }
    }

    suspend fun getKernelSUVersion(): String = withContext(Dispatchers.IO) {
        when (val res = execute("ksud --version 2>/dev/null")) {
            is Result.Success -> res.output
            is Result.Error -> "N/A"
        }
    }

    suspend fun getSuVersion(): String = withContext(Dispatchers.IO) {
        when (val res = execute("su --version 2>/dev/null || echo legacy")) {
            is Result.Success -> res.output
            is Result.Error -> "N/A"
        }
    }

    suspend fun isAdbShell(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "whoami"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val user = reader.readLine()
            process.waitFor()
            user == "shell" || user == "root"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isShizukuAvailable(): Boolean = withContext(Dispatchers.Main) {
        try {
            rikka.shizuku.Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun hasSELinuxEnforce(): Boolean = withContext(Dispatchers.IO) {
        when (val res = execute("getenforce 2>/dev/null")) {
            is Result.Success -> res.output == "Enforcing"
            is Result.Error -> true
        }
    }

    suspend fun getDebugInfo(): String = withContext(Dispatchers.IO) {
        buildString {
            appendLine("=== Beta Manager Debug Info ===")
            appendLine("Root: ${detectRootType().name}")
            appendLine("ADB Shell: ${isAdbShell()}")
            appendLine("SELinux: ${if (hasSELinuxEnforce()) "Enforcing" else "Permissive"}")
            appendLine("Shizuku: ${isShizukuAvailable()}")
            appendLine("Arch: ${System.getProperty("os.arch")}")
            appendLine("API: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
            when (val magisk = getMagiskVersion()) {
                "N/A" -> {}
                else -> appendLine("Magisk: $magisk")
            }
            when (val ksu = getKernelSUVersion()) {
                "N/A" -> {}
                else -> appendLine("KernelSU: $ksu")
            }
        }
    }
}
