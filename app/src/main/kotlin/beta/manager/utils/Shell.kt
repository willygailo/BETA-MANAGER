package beta.manager.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class RootType {
    MAGISK, KERNELSU, APATCH, AXERON, SU, NONE, SHIZUKU
}

object Shell {

    sealed class Result {
        data class Success(val output: String, val exitCode: Int = 0) : Result()
        data class Error(val message: String, val exitCode: Int = -1) : Result()
    }

    private var debugMode = false

    fun setDebugMode(enabled: Boolean) { debugMode = enabled }

    fun quote(value: String): String = "'${value.replace("'", "'\\''")}'"

    suspend fun executeWithElevation(command: String, timeout: Long = 30000L): Result {
        if (isRootAvailable()) {
            return execute(command, timeout, useRoot = true)
        }

        if (ShizukuShell.isAvailable()) {
            if (!ShizukuShell.hasPermission()) {
                return Result.Error("Shizuku is running but permission is not granted")
            }
            return ShizukuShell.execute(command, timeout)
        }

        return Result.Error("No root or Shizuku permission available")
    }

    suspend fun execute(command: String, timeout: Long = 30000L, useRoot: Boolean = false): Result = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val process = if (useRoot) {
                ProcessBuilder("su", "-c", command).start()
            } else {
                ProcessBuilder("sh", "-c", command).start()
            }

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
        // First try: check if su binary exists and responds
        try {
            val process = ProcessBuilder("su", "-c", "id").start()
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val finished = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext false
            }
            return@withContext line != null && (line.contains("uid=0") || line.contains("root")) && process.exitValue() == 0
        } catch (_: Exception) {}
        // Fallback: which su
        try {
            val p = ProcessBuilder("which", "su").start()
            val line = BufferedReader(InputStreamReader(p.inputStream)).readLine()
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            return@withContext !line.isNullOrBlank()
        } catch (_: Exception) {}
        false
    }

    suspend fun detectRootType(): RootType = withContext(Dispatchers.IO) {
        return@withContext when {
            checkMagisk() -> RootType.MAGISK
            checkKernelSU() -> RootType.KERNELSU
            checkAPatch() -> RootType.APATCH
            checkAxeron() -> RootType.AXERON
            isRootAvailable() -> RootType.SU
            ShizukuShell.hasPermission() -> RootType.SHIZUKU
            else -> RootType.NONE
        }
    }

    private suspend fun checkMagisk(): Boolean {
        // Try magisk binary
        val byBinary = execute("magisk --version 2>/dev/null || magisk -v 2>/dev/null")
        if (byBinary is Result.Success && byBinary.output.isNotBlank()) return true
        // Try checking magisk daemon socket
        val bySocket = execute("[ -S /dev/.magisk.unblock ] && echo ok 2>/dev/null")
        if (bySocket is Result.Success && bySocket.output.trim() == "ok") return true
        // Try /proc/net/unix
        val byProc = execute("grep -q magisk /proc/net/unix 2>/dev/null && echo ok")
        return byProc is Result.Success && byProc.output.trim() == "ok"
    }

    private suspend fun checkKernelSU(): Boolean {
        // Try ksud binary
        val byBinary = execute("ksud --version 2>/dev/null || ksud version 2>/dev/null")
        if (byBinary is Result.Success && byBinary.output.isNotBlank()) return true
        // Try KernelSU kernel property
        val byProp = execute("getprop sys.kernelsu.version 2>/dev/null || getprop ro.kernelsu.version 2>/dev/null")
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        // Check for KernelSU manager package
        val byPkg = execute("pm list packages 2>/dev/null | grep -q 'me.weishu.kernelsu\\|com.kernelsu' && echo ok")
        return byPkg is Result.Success && byPkg.output.trim() == "ok"
    }

    private suspend fun checkAPatch(): Boolean {
        val byBinary = execute("apd --version 2>/dev/null || apd version 2>/dev/null")
        if (byBinary is Result.Success && byBinary.output.isNotBlank()) return true
        val byProp = execute("getprop ro.apatch.version 2>/dev/null")
        return byProp is Result.Success && byProp.output.isNotBlank()
    }

    private suspend fun checkAxeron(): Boolean {
        // Axeron Manager (AxeronManager) uses its own daemon
        val byProp = execute("getprop ro.axeron.version 2>/dev/null")
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        val byPath = execute("[ -d /data/adb/axeron ] && echo ok 2>/dev/null")
        if (byPath is Result.Success && byPath.output.trim() == "ok") return true
        val byPkg = execute("pm list packages 2>/dev/null | grep -q 'com.axeron.manager\\|io.axeron' && echo ok")
        return byPkg is Result.Success && byPkg.output.trim() == "ok"
    }

    suspend fun getMagiskVersion(): String = withContext(Dispatchers.IO) {
        when (val res = execute("magisk --version 2>/dev/null || magisk -v 2>/dev/null")) {
            is Result.Success -> res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> "N/A"
        }
    }

    suspend fun getKernelSUVersion(): String = withContext(Dispatchers.IO) {
        val byBin = execute("ksud --version 2>/dev/null")
        if (byBin is Result.Success && byBin.output.isNotBlank()) return@withContext byBin.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
        when (val res = execute("getprop sys.kernelsu.version 2>/dev/null || getprop ro.kernelsu.version 2>/dev/null")) {
            is Result.Success -> res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> "N/A"
        }
    }

    suspend fun getAPatchVersion(): String = withContext(Dispatchers.IO) {
        val byBin = execute("apd --version 2>/dev/null")
        if (byBin is Result.Success && byBin.output.isNotBlank()) return@withContext byBin.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
        when (val res = execute("getprop ro.apatch.version 2>/dev/null")) {
            is Result.Success -> res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> "N/A"
        }
    }

    suspend fun getAxeronVersion(): String = withContext(Dispatchers.IO) {
        when (val res = execute("getprop ro.axeron.version 2>/dev/null")) {
            is Result.Success -> res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> "N/A"
        }
    }

    suspend fun getSuVersion(): String = withContext(Dispatchers.IO) {
        when (val res = execute("su --version 2>/dev/null || echo legacy")) {
            is Result.Success -> res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
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
            is Result.Success -> res.output.trim().equals("Enforcing", ignoreCase = true)
            is Result.Error -> true
        }
    }

    suspend fun getDebugInfo(): String = withContext(Dispatchers.IO) {
        val rootType = detectRootType()
        buildString {
            appendLine("=== Beta Manager Debug Info ===")
            appendLine("Version: 1.3.0 (10004)")
            appendLine("Root: ${rootType.name}")
            appendLine("ADB Shell: ${isAdbShell()}")
            appendLine("SELinux: ${if (hasSELinuxEnforce()) "Enforcing" else "Permissive"}")
            appendLine("Shizuku: ${isShizukuAvailable()}")
            appendLine("Arch: ${System.getProperty("os.arch")}")
            appendLine("API: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
            when (rootType) {
                RootType.MAGISK -> {
                    val v = getMagiskVersion()
                    if (v != "N/A") appendLine("Magisk: $v")
                }
                RootType.KERNELSU -> {
                    val v = getKernelSUVersion()
                    if (v != "N/A") appendLine("KernelSU: $v")
                }
                RootType.APATCH -> {
                    val v = getAPatchVersion()
                    if (v != "N/A") appendLine("APatch: $v")
                }
                RootType.AXERON -> {
                    val v = getAxeronVersion()
                    if (v != "N/A") appendLine("Axeron: $v")
                }
                else -> {}
            }
        }
    }
}
