package beta.manager.adb

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import beta.manager.service.BetaService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdbClient(private val context: Context) {

    suspend fun isServiceRunning(): Boolean {
        return BetaService.isStarted
    }

    suspend fun startService(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, BetaService::class.java)
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun stopService(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                context.stopService(Intent(context, BetaService::class.java))
                true
            } catch (_: Exception) {
                false
            }
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
            |# Bring Beta Manager to the foreground; the app starts its own local service.
            |monkey -p beta.manager 1 >/dev/null 2>&1
            |
            |echo "Beta Manager started on port $port"
        """.trimMargin()
    }
}
