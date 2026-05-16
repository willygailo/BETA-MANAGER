package beta.manager.adb

import android.content.Context
import beta.manager.utils.RootType
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

    suspend fun activate(mode: ActivationMode): ActivationResult = withContext(Dispatchers.IO) {
        if (client.isServiceRunning()) {
            return@withContext ActivationResult(true, mode, "Service already running")
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

            val cmd = listOf(
                "export CLASSPATH=\\\$(pm path beta.manager | grep base)",
                "app_process /system/bin beta.manager.service.BetaService &"
            ).joinToString(" && ")

            when (Shell.execute("su -c '$cmd'")) {
                is Shell.Result.Success -> {
                    val running = retryConnect(3, 500)
                    if (running) {
                        val rootType = Shell.detectRootType()
                        return ActivationResult(true, ActivationMode.ROOT_SU, "Service started with ${rootType.name} root")
                    }
                }
                is Shell.Result.Error -> {
                    return ActivationResult(false, ActivationMode.ROOT_SU, "Root execution failed")
                }
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

            val isElevated = ShizukuShell.isShellUid() || ShizukuShell.isRootUid()
            val cmd = listOf(
                "export CLASSPATH=\$(pm path beta.manager | grep base)",
                "nohup app_process /system/bin beta.manager.service.BetaService > /dev/null 2>&1 &"
            ).joinToString(" && ")

            when (ShizukuShell.execute(cmd)) {
                is Shell.Result.Success -> {
                    val running = retryConnectShizuku(5, 1000)
                    if (running) {
                        val level = if (isElevated) "elevated" else "standard"
                        return ActivationResult(true, ActivationMode.SHIZUKU, "Service started via Shizuku ($level)")
                    }
                }
                is Shell.Result.Error -> {
                    return ActivationResult(false, ActivationMode.SHIZUKU,
                        "Shizuku execution failed.\nMake sure Shizuku is running and Beta Manager is granted permission."
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
            if (ShizukuShell.isServiceRunning()) return true
            kotlinx.coroutines.delay(delayMs)
        }
        return ShizukuShell.isServiceRunning()
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
