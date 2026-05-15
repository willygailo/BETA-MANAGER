package beta.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import beta.manager.IBetaService
import beta.manager.plugin.PluginInstaller
import beta.manager.plugin.PluginManager
import beta.manager.plugin.PluginManager.Companion.SERVER_VERSION
import beta.manager.utils.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BetaService : Service() {

    companion object {
        const val TAG = "BetaService"
        const val SERVER_VERSION = 10001
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "beta_service_channel"

        private const val BASE_DIR = "/data/user_de/0/com.android.shell/beta/"
        private const val PLUGINS_DIR = "${BASE_DIR}plugins/"
        private const val BIN_DIR = "${BASE_DIR}bin/"
        private const val LOGS_DIR = "${BASE_DIR}logs/"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pluginManager = PluginManager(PLUGINS_DIR)
    private val pluginInstaller = PluginInstaller(PLUGINS_DIR, BASE_DIR)

    private val binder = object : IBetaService.Stub() {
        override fun getVersion(): Int = SERVER_VERSION

        override fun executeCommand(command: String): String {
            var result = ""
            val job = serviceScope.launch {
                when (val res = Shell.execute(command)) {
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
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        serviceScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        Shell.execute("mkdir -p $PLUGINS_DIR $BIN_DIR $LOGS_DIR")
        pluginManager.scanPlugins()
    }

    override fun onDestroy() {
        super.onDestroy()
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
