package beta.manager.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class RootType {
    MAGISK, KERNELSU, APATCH, AXERON, SU, NONE, SHIZUKU
}

object Shell {

    @Volatile
    private var cachedRootType: RootType? = null

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

    fun executeSyncWithElevation(command: String, timeout: Long = 30000L): Result {
        try {
            val process = ProcessBuilder("su", "-c", "id").start()
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val finished = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            val hasRoot = finished && line != null && (line.contains("uid=0") || line.contains("root")) && process.exitValue() == 0

            if (hasRoot) {
                return executeSync(command, timeout, useRoot = true)
            }
        } catch (_: Exception) {}

        if (ShizukuShell.hasPermissionSync()) {
            return ShizukuShell.executeSync(command, timeout)
        }

        return Result.Error("No root or Shizuku permission available")
    }

    suspend fun execute(command: String, timeout: Long = 30000L, useRoot: Boolean = false): Result = withContext(Dispatchers.IO) {
        return@withContext executeSync(command, timeout, useRoot)
    }

    fun executeSync(command: String, timeout: Long = 30000L, useRoot: Boolean = false): Result {
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
                return Result.Error("Command timed out after ${timeout}ms")
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
                return Result.Success(output)
            } else {
                return Result.Error(
                    message = stderr.toString().trim().ifEmpty { output },
                    exitCode = exitCode
                )
            }
        } catch (e: Exception) {
            if (debugMode) android.util.Log.e("Shell", "Error: ${e.message}")
            return Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("su", "-c", "id").start()
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext false
            }
            if (line != null && (line.contains("uid=0") || line.contains("root")) && process.exitValue() == 0) {
                return@withContext true
            }
        } catch (_: Exception) {}

        try {
            val process = ProcessBuilder("su", "-c", "echo ok").start()
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val finished = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext false
            }
            if (line?.trim() == "ok" && process.exitValue() == 0) {
                return@withContext true
            }
        } catch (_: Exception) {}

        false
    }

    suspend fun detectRootType(): RootType = withContext(Dispatchers.IO) {
        if (!isRootAvailable()) {
            return@withContext when {
                ShizukuShell.hasPermission() -> RootType.SHIZUKU
                else -> RootType.NONE
            }
        }

        return@withContext when {
            checkMagisk() -> RootType.MAGISK
            checkKernelSU() -> RootType.KERNELSU
            checkAPatch() -> RootType.APATCH
            checkAxeron() -> RootType.AXERON
            else -> RootType.SU
        }
    }

    private suspend fun checkMagisk(): Boolean {
        val byBinary = execute("magisk -v 2>/dev/null && echo ok", useRoot = true)
        if (byBinary is Result.Success && byBinary.output.contains("ok")) return true
        val bySocket = execute("[ -S /dev/.magisk.unblock ] && echo ok 2>/dev/null", useRoot = true)
        if (bySocket is Result.Success && bySocket.output.trim() == "ok") return true
        val byProc = execute("grep -q magisk /proc/net/unix 2>/dev/null && echo ok", useRoot = true)
        return byProc is Result.Success && byProc.output.trim() == "ok"
    }

    private suspend fun checkKernelSU(): Boolean {
        val byBinary = execute("ksud --version 2>/dev/null && echo ok", useRoot = true)
        if (byBinary is Result.Success && byBinary.output.contains("ok")) return true
        val byProp = execute("getprop sys.kernelsu.version 2>/dev/null || getprop ro.kernelsu.version 2>/dev/null", useRoot = true)
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        val byPkg = execute("pm list packages 2>/dev/null | grep -q 'me.weishu.kernelsu\\|com.kernelsu' && echo ok", useRoot = true)
        return byPkg is Result.Success && byPkg.output.trim() == "ok"
    }

    private suspend fun checkAPatch(): Boolean {
        val byBinary = execute("apd --version 2>/dev/null && echo ok", useRoot = true)
        if (byBinary is Result.Success && byBinary.output.contains("ok")) return true
        val byProp = execute("getprop ro.apatch.version 2>/dev/null", useRoot = true)
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        val byPath = execute("[ -d /data/adb/ap ] && echo ok 2>/dev/null", useRoot = true)
        return byPath is Result.Success && byPath.output.trim() == "ok"
    }

    private suspend fun checkAxeron(): Boolean {
        val byProp = execute("getprop ro.axeron.version 2>/dev/null", useRoot = true)
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        val byPath = execute("[ -d /data/adb/axeron ] && echo ok 2>/dev/null", useRoot = true)
        if (byPath is Result.Success && byPath.output.trim() == "ok") return true
        val byPkg = execute("pm list packages 2>/dev/null | grep -q 'com.axeron.manager\\|io.axeron' && echo ok", useRoot = true)
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
        return@withContext getDebugInfoSync()
    }

    fun getDebugInfoSync(): String {
        val rootType = detectRootTypeSync()
        return buildString {
            appendLine("=== Beta Manager Debug Info ===")
            appendLine("Version: 1.3.0 (10004)")
            appendLine("Root: ${rootType.name}")
            appendLine("ADB Shell: ${isAdbShellSync()}")
            appendLine("SELinux: ${if (hasSELinuxEnforceSync()) "Enforcing" else "Permissive"}")
            appendLine("Shizuku: ${isShizukuAvailableSync()}")
            appendLine("Arch: ${System.getProperty("os.arch")}")
            appendLine("API: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
            when (rootType) {
                RootType.MAGISK -> {
                    val v = getMagiskVersionSync()
                    if (v != "N/A") appendLine("Magisk: $v")
                }
                RootType.KERNELSU -> {
                    val v = getKernelSUVersionSync()
                    if (v != "N/A") appendLine("KernelSU: $v")
                }
                RootType.APATCH -> {
                    val v = getAPatchVersionSync()
                    if (v != "N/A") appendLine("APatch: $v")
                }
                RootType.AXERON -> {
                    val v = getAxeronVersionSync()
                    if (v != "N/A") appendLine("Axeron: $v")
                }
                else -> {}
            }
        }
    }

    fun detectRootTypeSync(): RootType {
        val type = if (!isRootAvailableSync()) {
            when {
                ShizukuShell.hasPermissionSync() -> RootType.SHIZUKU
                else -> RootType.NONE
            }
        } else {
            when {
                checkMagiskSync() -> RootType.MAGISK
                checkKernelSUSync() -> RootType.KERNELSU
                checkAPatchSync() -> RootType.APATCH
                checkAxeronSync() -> RootType.AXERON
                else -> RootType.SU
            }
        }
        cachedRootType = type
        return type
    }

    fun getCachedRootType(): RootType {
        return cachedRootType ?: detectRootTypeSync()
    }

    suspend fun refreshRootType(): RootType = withContext(Dispatchers.IO) {
        val type = detectRootType()
        cachedRootType = type
        return@withContext type
    }

    fun clearCachedRootType() {
        cachedRootType = null
    }

    fun isRootAvailableSync(): Boolean {
        try {
            val process = ProcessBuilder("su", "-c", "id").start()
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return false
            }
            if (line != null && (line.contains("uid=0") || line.contains("root")) && process.exitValue() == 0) {
                return true
            }
        } catch (_: Exception) {}

        try {
            val process = ProcessBuilder("su", "-c", "echo ok").start()
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val finished = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return false
            }
            if (line?.trim() == "ok" && process.exitValue() == 0) {
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    private fun checkMagiskSync(): Boolean {
        val byBinary = executeSync("magisk -v 2>/dev/null && echo ok", useRoot = true)
        if (byBinary is Result.Success && byBinary.output.contains("ok")) return true
        val bySocket = executeSync("[ -S /dev/.magisk.unblock ] && echo ok 2>/dev/null", useRoot = true)
        if (bySocket is Result.Success && bySocket.output.trim() == "ok") return true
        val byProc = executeSync("grep -q magisk /proc/net/unix 2>/dev/null && echo ok", useRoot = true)
        return byProc is Result.Success && byProc.output.trim() == "ok"
    }

    private fun checkKernelSUSync(): Boolean {
        val byBinary = executeSync("ksud --version 2>/dev/null && echo ok", useRoot = true)
        if (byBinary is Result.Success && byBinary.output.contains("ok")) return true
        val byProp = executeSync("getprop sys.kernelsu.version 2>/dev/null || getprop ro.kernelsu.version 2>/dev/null", useRoot = true)
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        val byPkg = executeSync("pm list packages 2>/dev/null | grep -q 'me.weishu.kernelsu\\|com.kernelsu' && echo ok", useRoot = true)
        return byPkg is Result.Success && byPkg.output.trim() == "ok"
    }

    private fun checkAPatchSync(): Boolean {
        val byBinary = executeSync("apd --version 2>/dev/null && echo ok", useRoot = true)
        if (byBinary is Result.Success && byBinary.output.contains("ok")) return true
        val byProp = executeSync("getprop ro.apatch.version 2>/dev/null", useRoot = true)
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        val byPath = executeSync("[ -d /data/adb/ap ] && echo ok 2>/dev/null", useRoot = true)
        return byPath is Result.Success && byPath.output.trim() == "ok"
    }

    private fun checkAxeronSync(): Boolean {
        val byProp = executeSync("getprop ro.axeron.version 2>/dev/null", useRoot = true)
        if (byProp is Result.Success && byProp.output.isNotBlank()) return true
        val byPath = executeSync("[ -d /data/adb/axeron ] && echo ok 2>/dev/null", useRoot = true)
        if (byPath is Result.Success && byPath.output.trim() == "ok") return true
        val byPkg = executeSync("pm list packages 2>/dev/null | grep -q 'com.axeron.manager\\|io.axeron' && echo ok", useRoot = true)
        return byPkg is Result.Success && byPkg.output.trim() == "ok"
    }

    fun getMagiskVersionSync(): String {
        when (val res = executeSync("magisk -v 2>/dev/null")) {
            is Result.Success -> return res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> return "N/A"
        }
    }

    fun getKernelSUVersionSync(): String {
        val byBin = executeSync("ksud --version 2>/dev/null")
        if (byBin is Result.Success && byBin.output.isNotBlank()) {
            return byBin.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
        }
        when (val res = executeSync("getprop sys.kernelsu.version 2>/dev/null || getprop ro.kernelsu.version 2>/dev/null")) {
            is Result.Success -> return res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> return "N/A"
        }
    }

    fun getAPatchVersionSync(): String {
        val byBin = executeSync("apd --version 2>/dev/null")
        if (byBin is Result.Success && byBin.output.isNotBlank()) {
            return byBin.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
        }
        when (val res = executeSync("getprop ro.apatch.version 2>/dev/null")) {
            is Result.Success -> return res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> return "N/A"
        }
    }

    fun getAxeronVersionSync(): String {
        when (val res = executeSync("getprop ro.axeron.version 2>/dev/null")) {
            is Result.Success -> return res.output.lines().firstOrNull { it.isNotBlank() } ?: "N/A"
            is Result.Error -> return "N/A"
        }
    }

    fun isAdbShellSync(): Boolean {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "whoami"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val user = reader.readLine()
            process.waitFor()
            return user == "shell" || user == "root"
        } catch (e: Exception) {
            return false
        }
    }

    fun isShizukuAvailableSync(): Boolean {
        try {
            return rikka.shizuku.Shizuku.pingBinder()
        } catch (_: Exception) {
            return false
        }
    }

    fun hasSELinuxEnforceSync(): Boolean {
        when (val res = executeSync("getenforce 2>/dev/null")) {
            is Result.Success -> return res.output.trim().equals("Enforcing", ignoreCase = true)
            is Result.Error -> return true
        }
    }
}
