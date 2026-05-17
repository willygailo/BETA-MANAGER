package beta.manager.utils

import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuShell {

    const val REQUEST_PERMISSION_CODE = 6201
    private const val TAG = "ShizukuShell"

    @Volatile
    private var binderAlive = false

    @Volatile
    private var permissionGranted = false

    private val binderDeathRecipient = IBinder.DeathRecipient {
        binderAlive = false
        permissionGranted = false
        Log.w(TAG, "Shizuku binder died")
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_PERMISSION_CODE) {
            permissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Shizuku permission granted: $permissionGranted")
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        binderAlive = true
        refreshPermission()
        Log.d(TAG, "Shizuku binder received")
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        binderAlive = false
        permissionGranted = false
        Log.w(TAG, "Shizuku binder dead")
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (_: Exception) {}
    }

    private fun refreshPermission() {
        try {
            permissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            permissionGranted = false
        }
    }

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.Main) {
        try {
            binderAlive && Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getVersion(): Int = withContext(Dispatchers.Main) {
        try {
            if (!Shizuku.pingBinder()) return@withContext -1
            Shizuku.getVersion()
        } catch (_: Exception) {
            -1
        }
    }

    suspend fun hasPermission(): Boolean = withContext(Dispatchers.Main) {
        try {
            if (!Shizuku.pingBinder()) return@withContext false
            refreshPermission()
            permissionGranted
        } catch (_: Exception) {
            false
        }
    }

    suspend fun requestPermission(requestCode: Int = REQUEST_PERMISSION_CODE): Boolean =
        withContext(Dispatchers.Main) {
            try {
                if (!Shizuku.pingBinder()) return@withContext false
                refreshPermission()
                if (permissionGranted) return@withContext true
                Shizuku.requestPermission(requestCode)
                false
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

    suspend fun execute(command: String, timeout: Long = 30000L): Shell.Result =
        withContext(Dispatchers.IO) {
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
                    return@withContext Shell.Result.Success(output)
                } else {
                    return@withContext Shell.Result.Error(
                        message = stderr.toString().trim().ifEmpty { output },
                        exitCode = exitCode
                    )
                }
            } catch (e: Exception) {
                return@withContext Shell.Result.Error(e.message ?: "Shizuku execution failed")
            }
        }

    /**
     * Creates a new process via Shizuku.
     *
     * Shizuku 13+ exposes newProcess() as a proper public API. For older versions,
     * we fall back to reflection. Compatible with Shizuku 11+ on Android 8-16.
     */
    private fun newShizukuProcess(cmd: Array<String>): Process {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            return method.invoke(null, cmd, null, null) as Process
        } catch (e: Exception) {
            throw RuntimeException("Failed to create Shizuku process", e)
        }
    }

    fun hasPermissionSync(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun executeSync(command: String, timeout: Long = 30000L): Shell.Result {
        try {
            if (!hasPermissionSync()) {
                return Shell.Result.Error("Shizuku permission is not granted")
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
                return Shell.Result.Error("Command timed out after ${timeout}ms")
            }
            stdoutReader.join(1000)
            stderrReader.join(1000)

            val exitCode = proc.exitValue()
            val output = stdout.toString().trim()

            if (exitCode == 0) {
                return Shell.Result.Success(output)
            } else {
                return Shell.Result.Error(
                    message = stderr.toString().trim().ifEmpty { output },
                    exitCode = exitCode
                )
            }
        } catch (e: Exception) {
            return Shell.Result.Error(e.message ?: "Shizuku execution failed")
        }
    }

    suspend fun isServiceRunning(): Boolean {
        return when (execute("pgrep -f 'beta.manager' 2>/dev/null | head -1")) {
            is Shell.Result.Success -> true
            is Shell.Result.Error -> false
        }
    }
}
