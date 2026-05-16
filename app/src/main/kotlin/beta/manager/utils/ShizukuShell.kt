package beta.manager.utils

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuShell {

    const val REQUEST_PERMISSION_CODE = 6201

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.Main) {
        try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getVersion(): Int = withContext(Dispatchers.Main) {
        try {
            Shizuku.getVersion()
        } catch (_: Exception) {
            -1
        }
    }

    suspend fun hasPermission(): Boolean = withContext(Dispatchers.Main) {
        try {
            Shizuku.pingBinder() &&
                !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    suspend fun requestPermission(requestCode: Int = REQUEST_PERMISSION_CODE): Boolean = withContext(Dispatchers.Main) {
        try {
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return@withContext false
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return@withContext true
            if (Shizuku.shouldShowRequestPermissionRationale()) return@withContext false
            Shizuku.requestPermission(requestCode)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isShellUid(): Boolean = withContext(Dispatchers.Main) {
        try {
            hasPermission() && Shizuku.getUid() == 2000
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isRootUid(): Boolean = withContext(Dispatchers.Main) {
        try {
            hasPermission() && Shizuku.getUid() == 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun execute(command: String, timeout: Long = 30000L): Shell.Result = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Shell.Result.Error("Shizuku permission is not granted")
            }

            val proc = newShizukuProcess(arrayOf("sh", "-c", command))
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val stdoutReader = Thread {
                BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdout.appendLine(line)
                    }
                }
            }
            val stderrReader = Thread {
                BufferedReader(InputStreamReader(proc.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderr.appendLine(line)
                    }
                }
            }
            stdoutReader.start()
            stderrReader.start()

            val finished = proc.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return@withContext Shell.Result.Error("Command timed out after ${timeout}ms")
            }
            stdoutReader.join(1000)
            stderrReader.join(1000)

            val exitCode = proc.exitValue()
            val output = stdout.toString().trim()

            if (exitCode == 0) {
                Shell.Result.Success(output)
            } else {
                Shell.Result.Error(
                    message = stderr.toString().trim().ifEmpty { output },
                    exitCode = exitCode
                )
            }
        } catch (e: Exception) {
            Shell.Result.Error(e.message ?: "Shizuku execution failed")
        }
    }

    private fun newShizukuProcess(cmd: Array<String>): java.lang.Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(null, cmd, null, null) as java.lang.Process
    }

    suspend fun isServiceRunning(): Boolean {
        return when (execute("pgrep -f 'beta.manager.service.BetaService'")) {
            is Shell.Result.Success -> true
            is Shell.Result.Error -> false
        }
    }
}
