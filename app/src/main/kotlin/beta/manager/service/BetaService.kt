package beta.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import beta.manager.IBetaService
import beta.manager.plugin.PluginInstaller
import beta.manager.plugin.PluginManager
import beta.manager.utils.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class BetaService : Service() {

    companion object {
        const val TAG = "BetaService"
        const val SERVER_VERSION = 10002
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "beta_service_channel"

        const val BASE_DIR = "/data/user_de/0/com.android.shell/beta/"
        const val PLUGINS_DIR = "${BASE_DIR}plugins/"
        const val BIN_DIR = "${BASE_DIR}bin/"
        const val LOGS_DIR = "${BASE_DIR}logs/"

        @Volatile
        var isStarted: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pluginManager = PluginManager(PLUGINS_DIR)
    private val pluginInstaller = PluginInstaller(PLUGINS_DIR, BASE_DIR)

    private val binder = object : IBetaService.Stub() {
        override fun getVersion(): Int = SERVER_VERSION

        override fun executeCommand(command: String): String {
            var result = ""
            val job = serviceScope.launch {
                when (val res = Shell.executeWithElevation(command)) {
                    is Shell.Result.Success -> result = res.output
                    is Shell.Result.Error -> result = "ERROR: ${res.message}"
                }
            }
            try {
                kotlinx.coroutines.runBlocking { job.join() }
            } catch (e: Exception) {
                result = "ERROR: ${e.message}"
            }
            return result
        }

        override fun getPluginStatus(pluginId: String): String {
            return pluginManager.getPluginStatus(pluginId)
        }

        override fun installPlugin(zipPath: String): Boolean {
            var success = false
            val job = serviceScope.launch {
                success = pluginInstaller.install(zipPath)
            }
            try {
                kotlinx.coroutines.runBlocking { job.join() }
            } catch (_: Exception) {}
            return success
        }

        override fun removePlugin(pluginId: String): Boolean {
            return pluginManager.markForRemoval(pluginId)
        }

        override fun disablePlugin(pluginId: String): Boolean {
            return pluginManager.disable(pluginId)
        }

        override fun enablePlugin(pluginId: String): Boolean {
            return pluginManager.enable(pluginId)
        }

        override fun runPluginAction(pluginId: String): Boolean {
            var success = false
            val job = serviceScope.launch {
                success = pluginManager.runAction(pluginId)
            }
            try {
                kotlinx.coroutines.runBlocking { job.join() }
            } catch (_: Exception) {}
            return success
        }

        override fun listPlugins(): Array<String> {
            return pluginManager.listPlugins().toTypedArray()
        }

        override fun isRunning(): Boolean = true

        override fun getServerPath(): String = BASE_DIR

        override fun getApiLevel(): Int = Build.VERSION.SDK_INT

        override fun getArchitecture(): String {
            val abis = Build.SUPPORTED_ABIS
            return if (abis.isNotEmpty()) abis[0] else "unknown"
        }

        override fun fixUpdateAll(): String {
            val sb = StringBuilder()
            val job = serviceScope.launch {
                try {
                    val plugins = pluginManager.scanPlugins()
                    sb.appendLine("Found ${plugins.size} plugins")

                    var fixed = 0
                    var updated = 0

                    for (plugin in plugins) {
                        if (pluginInstaller.fixPlugin(plugin.id)) {
                            fixed++
                            sb.appendLine("Fixed: ${plugin.name}")
                        }
                    }

                    val updates = pluginInstaller.checkForUpdates(plugins)
                    for (update in updates) {
                        if (update.needsUpdate) {
                            sb.appendLine("Update available: ${update.name} (${update.currentVersionCode} -> ${update.newVersionCode})")
                            updated++
                        }
                    }

                    sb.appendLine("Fixed $fixed plugins, $updated updates available")
                } catch (e: Exception) {
                    sb.appendLine("Error: ${e.message}")
                }
            }
            try {
                kotlinx.coroutines.runBlocking { job.join() }
            } catch (e: Exception) {
                sb.appendLine("Error: ${e.message}")
            }
            return sb.toString().trim()
        }

        override fun flashModule(zipPath: String): String {
            val sb = StringBuilder()
            val job = serviceScope.launch {
                try {
                    val file = File(zipPath)
                    if (!file.exists()) {
                        sb.appendLine("ERROR: ZIP not found: $zipPath")
                        return@launch
                    }

                    val result = pluginInstaller.installToMagisk(zipPath)
                    if (result.success) {
                        sb.appendLine("SUCCESS: ${result.message}")
                        sb.appendLine("Module ID: ${result.pluginId}")
                        sb.appendLine("Reboot to apply module")
                    } else {
                        val ksuResult = pluginInstaller.installToKSU(zipPath)
                        if (ksuResult.success) {
                            sb.appendLine("SUCCESS (KSU): ${ksuResult.message}")
                        } else {
                            sb.appendLine("FAILED: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    sb.appendLine("ERROR: ${e.message}")
                }
            }
            try {
                kotlinx.coroutines.runBlocking { job.join() }
            } catch (e: Exception) {
                sb.appendLine("ERROR: ${e.message}")
            }
            return sb.toString().trim()
        }

        override fun getRootType(): String {
            var result = "unknown"
            val job = serviceScope.launch {
                result = Shell.detectRootType().name
            }
            try {
                kotlinx.coroutines.runBlocking { job.join() }
            } catch (_: Exception) {}
            return result
        }

        override fun getDebugInfo(): String {
            var result = ""
            val job = serviceScope.launch {
                result = Shell.getDebugInfo()
            }
            try {
                kotlinx.coroutines.runBlocking { job.join() }
            } catch (e: Exception) {
                result = "Error: ${e.message}"
            }
            return result
        }

        override fun getPluginCount(): Int {
            return pluginManager.listPlugins().size
        }

        override fun getEnabledCount(): Int {
            return pluginManager.scanPlugins().count { it.isEnabled }
        }

        override fun getModulePath(): String = PLUGINS_DIR
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        isStarted = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        serviceScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        Shell.executeWithElevation(
            "mkdir -p ${Shell.quote(PLUGINS_DIR)} ${Shell.quote(BIN_DIR)} ${Shell.quote(LOGS_DIR)}"
        )
        pluginManager.scanPlugins()
    }

    override fun onDestroy() {
        super.onDestroy()
        isStarted = false
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Beta Manager Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Running Beta Manager background service"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Beta Manager")
            .setContentText("Service is running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(
                Notification.FOREGROUND_SERVICE_IMMEDIATE
            )
        }

        return builder.build()
    }
}
