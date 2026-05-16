package beta.manager.adb

import android.content.Context
import beta.manager.utils.Shell
import beta.manager.utils.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ActivationMode {
    WIRELESS_DEBUG,
    ADB_USB,
    TCP_MODE,
    ROOT_SU,
    SHIZUKU
}

data class ActivationResult(
    val success: Boolean,
    val mode: ActivationMode,
    val message: String
)

class AdbActivator(private val context: Context) {

    private val client = AdbClient(context)

    private companion object {
        const val RUNTIME_DIR = "/data/user_de/0/com.android.shell/beta"
        const val PLUGINS_DIR = "$RUNTIME_DIR/plugins"
        const val BIN_DIR = "$RUNTIME_DIR/bin"
        const val LOGS_DIR = "$RUNTIME_DIR/logs"
    }

    suspend fun activate(mode: ActivationMode): ActivationResult = withContext(Dispatchers.IO) {
        if (client.isServiceRunning()) {
            val ready = when (mode) {
                ActivationMode.ROOT_SU -> Shell.isRootAvailable()
                ActivationMode.SHIZUKU -> ShizukuShell.isAvailable() && ShizukuShell.hasPermission()
                else -> true
            }
            if (ready) {
                return@withContext ActivationResult(true, mode, "Service already running")
            }
        }

        when (mode) {
            ActivationMode.WIRELESS_DEBUG -> activateWireless()
            ActivationMode.ADB_USB -> activateAdbUsb()
            ActivationMode.TCP_MODE -> activateTcp()
            ActivationMode.ROOT_SU -> activateRoot()
            ActivationMode.SHIZUKU -> activateShizuku()
        }
    }

    private suspend fun activateWireless(): ActivationResult {
        try {
            client.openWirelessDebugSettings()
            val success = retryConnect(5, 2000)
            if (success) {
                return ActivationResult(true, ActivationMode.WIRELESS_DEBUG, "Connected via Wireless Debugging")
            }
            return ActivationResult(false, ActivationMode.WIRELESS_DEBUG,
                "Wireless Debug failed. Enable Wireless Debugging in Developer Options, then try again."
            )
        } catch (e: Exception) {
            return ActivationResult(false, ActivationMode.WIRELESS_DEBUG,
                "Error: ${e.message}"
            )
        }
    }

    private suspend fun activateAdbUsb(): ActivationResult {
        try {
            if (Shell.isAdbShell()) {
                val started = client.startService()
                if (started) {
                    return ActivationResult(true, ActivationMode.ADB_USB, "Service started via ADB shell")
                }
            }

            return ActivationResult(false, ActivationMode.ADB_USB,
                "ADB USB Mode:\n\n" +
                "1. Connect phone to computer via USB\n" +
                "2. Enable USB Debugging in Developer Options\n" +
                "3. Run on computer:\n" +
                "   adb shell sh /sdcard/beta_start.sh"
            )
        } catch (e: Exception) {
            return ActivationResult(false, ActivationMode.ADB_USB, "Error: ${e.message}")
        }
    }

    private suspend fun activateTcp(): ActivationResult {
        try {
            when (val res = Shell.execute("adb tcpip 5555")) {
                is Shell.Result.Success -> {
                    val success = retryConnect(3, 1000)
                    if (success) {
                        return ActivationResult(true, ActivationMode.TCP_MODE, "Connected via TCP")
                    }
                }
                is Shell.Result.Error -> {
                    return ActivationResult(false, ActivationMode.TCP_MODE,
                        "TCP Mode:\n\n" +
                        "For Android 10 and below:\n" +
                        "1. Connect via USB once\n" +
                        "2. Run: adb tcpip 5555\n" +
                        "3. Disconnect USB\n" +
                        "4. Run: adb connect <device_ip>:5555"
                    )
                }
            }
            return ActivationResult(false, ActivationMode.TCP_MODE, "TCP connection failed")
        } catch (e: Exception) {
            return ActivationResult(false, ActivationMode.TCP_MODE, "Error: ${e.message}")
        }
    }

    private suspend fun activateRoot(): ActivationResult {
        try {
            if (!Shell.isRootAvailable()) {
                return ActivationResult(false, ActivationMode.ROOT_SU, "Root not available on this device")
            }

            if (!client.startService()) {
                return ActivationResult(false, ActivationMode.ROOT_SU, "Unable to start local Beta service")
            }

            val mkdirCommand = "mkdir -p ${Shell.quote(PLUGINS_DIR)} ${Shell.quote(BIN_DIR)} ${Shell.quote(LOGS_DIR)}"
            when (val result = Shell.executeWithElevation(mkdirCommand)) {
                is Shell.Result.Success -> Unit
                is Shell.Result.Error -> return ActivationResult(
                    false,
                    ActivationMode.ROOT_SU,
                    "Root available, but runtime setup failed: ${result.message}"
                )
            }

            val running = retryConnect(3, 500)
            if (running) {
                val rootType = Shell.detectRootType()
                return ActivationResult(true, ActivationMode.ROOT_SU, "Service ready with ${rootType.name} root")
            }
            return ActivationResult(false, ActivationMode.ROOT_SU, "Root activation failed")
        } catch (e: Exception) {
            return ActivationResult(false, ActivationMode.ROOT_SU, "Error: ${e.message}")
        }
    }

    private suspend fun activateShizuku(): ActivationResult {
        try {
            if (!ShizukuShell.isAvailable()) {
                return ActivationResult(false, ActivationMode.SHIZUKU,
                    "Shizuku not running.\n\n" +
                    "1. Install Shizuku from GitHub\n" +
                    "2. Open Shizuku app\n" +
                    "3. Start Shizuku service\n" +
                    "4. Grant Beta Manager permission"
                )
            }

            if (!ShizukuShell.hasPermission()) {
                ShizukuShell.requestPermission()
                return ActivationResult(false, ActivationMode.SHIZUKU,
                    "Shizuku permission requested. Tap Allow, then activate again."
                )
            }

            if (!client.startService()) {
                return ActivationResult(false, ActivationMode.SHIZUKU, "Unable to start local Beta service")
            }

            val isElevated = ShizukuShell.isShellUid() || ShizukuShell.isRootUid()
            val mkdirCommand = "mkdir -p ${Shell.quote(PLUGINS_DIR)} ${Shell.quote(BIN_DIR)} ${Shell.quote(LOGS_DIR)}"

            when (val result = ShizukuShell.execute(mkdirCommand)) {
                is Shell.Result.Success -> {
                    val running = retryConnectShizuku(5, 1000)
                    if (running) {
                        val level = if (isElevated) "elevated" else "standard"
                        return ActivationResult(true, ActivationMode.SHIZUKU, "Service ready via Shizuku ($level)")
                    }
                }
                is Shell.Result.Error -> {
                    return ActivationResult(false, ActivationMode.SHIZUKU,
                        "Shizuku runtime setup failed: ${result.message}"
                    )
                }
            }
            return ActivationResult(false, ActivationMode.SHIZUKU, "Shizuku activation failed")
        } catch (e: Exception) {
            return ActivationResult(false, ActivationMode.SHIZUKU, "Error: ${e.message}")
        }
    }

    private suspend fun retryConnectShizuku(retries: Int, delayMs: Long): Boolean {
        for (i in 0 until retries) {
            if (client.isServiceRunning()) return true
            kotlinx.coroutines.delay(delayMs)
        }
        return client.isServiceRunning()
    }

    private suspend fun retryConnect(retries: Int, delayMs: Long): Boolean {
        for (i in 0 until retries) {
            if (client.isServiceRunning()) return true
            kotlinx.coroutines.delay(delayMs)
        }
        return client.isServiceRunning()
    }

    suspend fun autoDetectMode(): ActivationMode {
        return when {
            Shell.isRootAvailable() -> ActivationMode.ROOT_SU
            ShizukuShell.isAvailable() -> ActivationMode.SHIZUKU
            Shell.isAdbShell() -> ActivationMode.ADB_USB
            else -> ActivationMode.WIRELESS_DEBUG
        }
    }

    suspend fun autoActivate(): ActivationResult {
        val mode = autoDetectMode()
        return activate(mode)
    }

    suspend fun checkAllModes(): Map<ActivationMode, Boolean> = withContext(Dispatchers.IO) {
        mapOf(
            ActivationMode.ROOT_SU to Shell.isRootAvailable(),
            ActivationMode.ADB_USB to Shell.isAdbShell(),
            ActivationMode.WIRELESS_DEBUG to false,
            ActivationMode.TCP_MODE to false,
            ActivationMode.SHIZUKU to ShizukuShell.isAvailable()
        )
    }
}
