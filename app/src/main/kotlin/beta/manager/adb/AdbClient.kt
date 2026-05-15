package beta.manager.adb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import beta.manager.utils.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdbClient(private val context: Context) {

    suspend fun isServiceRunning(): Boolean {
        return when (val res = Shell.execute("pgrep -f 'beta.manager.service.BetaService'")) {
            is Shell.Result.Success -> res.output.isNotBlank()
            is Shell.Result.Error -> false
        }
    }

    suspend fun startService(): Boolean {
        val cmd = listOf(
            "export CLASSPATH=$(pm path beta.manager | grep base)",
            "app_process /system/bin beta.manager.service.BetaService &"
        ).joinToString(" && ")

        return when (Shell.execute(cmd)) {
            is Shell.Result.Success -> true
            is Shell.Result.Error -> false
        }
    }

    suspend fun stopService(): Boolean {
        return when (Shell.execute("pgrep -f 'beta.manager.service.BetaService' | xargs kill -9 2>/dev/null")) {
            is Shell.Result.Success -> true
            is Shell.Result.Error -> false
        }
    }

    fun openWirelessDebugSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openDeveloperSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    companion object {
        fun generateStartScript(port: Int = 5555): String = """
            |#!/system/bin/sh
            |# Beta Manager Start Script
            |BASE=/data/user_de/0/com.android.shell/beta
            |mkdir -p ${'$'}BASE/bin ${'$'}BASE/plugins ${'$'}BASE/logs
            |
            |# ADB over TCP
            |adb tcpip $port 2>/dev/null
            |
            |# Start BetaService
            |export CLASSPATH=${'$'}(pm path beta.manager | grep base)
            |exec app_process /system/bin beta.manager.service.BetaService &
            |
            |echo "Beta Manager started on port $port"
        """.trimMargin()
    }
}
