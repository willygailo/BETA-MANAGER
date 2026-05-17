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
import beta.manager.utils.ShizukuShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class BetaService : Service() {

    companion object {
        const val TAG = "BetaService"
        const val SERVER_VERSION = 10004
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
    private val hasElevatedPrivileges: Boolean
        get() = Shell.isRootAvailableSync() || ShizukuShell.hasPermissionSync()
    private val effectiveBaseDir: String by lazy {
        if (hasElevatedPrivileges) BASE_DIR
        else filesDir.absolutePath + "/beta/"
    }
    private val effectivePluginsDir: String by lazy { "$effectiveBaseDir/plugins/" }
    private val effectiveBinDir: String by lazy { "$effectiveBaseDir/bin/" }
    private val effectiveLogsDir: String by lazy { "$effectiveBaseDir/logs/" }
    private val pluginManager by lazy { PluginManager(effectivePluginsDir) }
    private val pluginInstaller by lazy { PluginInstaller(effectivePluginsDir, effectiveBaseDir) }

    private val binder = object : IBetaService.Stub() {
        override fun getVersion(): Int = SERVER_VERSION

        override fun executeCommand(command: String): String {
            return try {
                val result = Shell.executeSyncWithElevation(command)
                when (result) {
                    is Shell.Result.Success -> result.output
                    is Shell.Result.Error -> "ERROR: ${result.message}"
                }
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
        }

        override fun getPluginStatus(pluginId: String): String {
            return pluginManager.getPluginStatus(pluginId)
        }

        override fun installPlugin(zipPath: String): Boolean {
            return try {
                pluginInstaller.installSync(zipPath)
            } catch (_: Exception) {
                false
            }
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
            return try {
                pluginManager.runActionSync(pluginId)
            } catch (_: Exception) {
                false
            }
        }

        override fun listPlugins(): Array<String> {
            return pluginManager.listPlugins().toTypedArray()
        }

        override fun isRunning(): Boolean = true

        override fun getServerPath(): String = effectiveBaseDir

        override fun getApiLevel(): Int = Build.VERSION.SDK_INT

        override fun getArchitecture(): String {
            val abis = Build.SUPPORTED_ABIS
            return if (abis.isNotEmpty()) abis[0] else "unknown"
        }

        override fun fixUpdateAll(): String {
            return try {
                val sb = StringBuilder()
                val plugins = pluginManager.scanPluginsSync()
                sb.appendLine("Found ${plugins.size} plugins")

                var fixed = 0
                var updated = 0

                for (plugin in plugins) {
                    if (pluginInstaller.fixPluginSync(plugin.id)) {
                        fixed++
                        sb.appendLine("Fixed: ${plugin.name}")
                    }
                }

                val updates = pluginInstaller.checkForUpdatesSync(plugins)
                for (update in updates) {
                    if (update.needsUpdate) {
                        sb.appendLine("Update available: ${update.name} (${update.currentVersionCode} -> ${update.newVersionCode})")
                        updated++
                    }
                }

                sb.appendLine("Fixed $fixed plugins, $updated updates available")
                sb.toString().trim()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        override fun flashModule(zipPath: String): String {
            return try {
                val sb = StringBuilder()
                val file = File(zipPath)
                if (!file.exists()) {
                    "ERROR: ZIP not found: $zipPath"
                } else {
                    val result = pluginInstaller.installToMagiskSync(zipPath)
                    if (result.success) {
                        sb.appendLine("SUCCESS: ${result.message}")
                        sb.appendLine("Module ID: ${result.pluginId}")
                        sb.appendLine("Reboot to apply module")
                    } else {
                        val ksuResult = pluginInstaller.installToKSUSync(zipPath)
                        if (ksuResult.success) {
                            sb.appendLine("SUCCESS (KSU): ${ksuResult.message}")
                        } else {
                            sb.appendLine("FAILED: ${result.message}")
                        }
                    }
                    sb.toString().trim()
                }
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
        }

        override fun getRootType(): String {
            return try {
                Shell.detectRootTypeSync().name
            } catch (_: Exception) {
                "unknown"
            }
        }

        override fun getDebugInfo(): String {
            return try {
                Shell.getDebugInfoSync()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        override fun getPluginCount(): Int {
            return pluginManager.listPlugins().size
        }

        override fun getEnabledCount(): Int {
            return pluginManager.scanPluginsSync().count { it.isEnabled }
        }

        override fun getModulePath(): String = effectivePluginsDir

        override fun getMagiskVersion(): String {
            return try { Shell.getMagiskVersionSync() } catch (_: Exception) { "N/A" }
        }

        override fun getKernelSUVersion(): String {
            return try { Shell.getKernelSUVersionSync() } catch (_: Exception) { "N/A" }
        }

        override fun getAPatchVersion(): String {
            return try { Shell.getAPatchVersionSync() } catch (_: Exception) { "N/A" }
        }

        override fun getAxeronVersion(): String {
            return try { Shell.getAxeronVersionSync() } catch (_: Exception) { "N/A" }
        }
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
        if (hasElevatedPrivileges) {
            val result = Shell.executeWithElevation(
                "mkdir -p ${Shell.quote(effectivePluginsDir)} ${Shell.quote(effectiveBinDir)} ${Shell.quote(effectiveLogsDir)}"
            )
            if (result is Shell.Result.Error) {
                android.util.Log.e(TAG, "Failed to create elevated directories: ${result.message}")
            }
        } else {
            File(effectivePluginsDir).mkdirs()
            File(effectiveBinDir).mkdirs()
            File(effectiveLogsDir).mkdirs()
            android.util.Log.i(TAG, "Using app-private directory: $effectiveBaseDir")
        }
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
