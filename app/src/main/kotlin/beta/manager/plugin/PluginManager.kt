package beta.manager.plugin

import beta.manager.utils.Shell
import kotlinx.coroutines.runBlocking
import java.io.File

enum class PluginSource { BETA, AXMANAGER, MAGISK, KSU }

data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int = 1,
    val author: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val hasAction: Boolean = false,
    val hasWebUI: Boolean = false,
    val installedAt: Long = System.currentTimeMillis(),
    val source: PluginSource = PluginSource.BETA
)

data class UpdateInfo(
    val id: String,
    val name: String,
    val currentVersion: String,
    val currentVersionCode: Int,
    val newVersionCode: Int,
    val needsUpdate: Boolean
)

class PluginManager(private val pluginsDir: String) {

    companion object {
        const val SERVER_VERSION = 10001
        private val AXMANAGER_PLUGINS_DIR = "/data/user_de/0/com.android.shell/axeron/plugins/"
        private val MAGISK_MODULES_DIR = "/data/adb/modules/"
        private val KSU_MODULES_DIR = "/data/adb/ksu/modules/"
    }

    private val plugins = mutableMapOf<String, PluginInfo>()

    fun scanPlugins(): List<PluginInfo> {
        plugins.clear()
        scanSingleDir(pluginsDir, PluginSource.BETA)
        scanSingleDir(AXMANAGER_PLUGINS_DIR, PluginSource.AXMANAGER)
        scanModulesDir(MAGISK_MODULES_DIR, PluginSource.MAGISK)
        scanModulesDir(KSU_MODULES_DIR, PluginSource.KSU)
        return plugins.values.toList()
    }

    private fun scanSingleDir(dirPath: String, source: PluginSource) {
        val dir = File(dirPath)
        if (!dir.exists()) return
        dir.listFiles()?.forEach { pluginDir ->
            if (pluginDir.isDirectory) scanPluginDir(pluginDir, source)
        }
    }

    private fun scanModulesDir(dirPath: String, source: PluginSource) {
        val dir = File(dirPath)
        if (!dir.exists()) return
        dir.listFiles()?.forEach { pluginDir ->
            if (pluginDir.isDirectory) {
                val propFile = File(pluginDir, "module.prop")
                if (!propFile.exists()) return@forEach
                val props = parseModuleProp(propFile)
                val id = props["id"] ?: pluginDir.name
                if (plugins.containsKey(id)) return@forEach
                plugins[id] = buildPluginInfo(pluginDir, props, id, source)
            }
        }
    }

    private fun scanPluginDir(pluginDir: File, source: PluginSource) {
        val propFile = File(pluginDir, "module.prop")
        if (!propFile.exists()) return
        val props = parseModuleProp(propFile)
        val id = props["id"] ?: pluginDir.name
        if (plugins.containsKey(id)) return
        if (File(pluginDir, "remove").exists()) {
            runBlocking { cleanupPlugin(pluginDir) }
            return
        }
        plugins[id] = buildPluginInfo(pluginDir, props, id, source)
    }

    private fun buildPluginInfo(dir: File, props: Map<String, String>, id: String, source: PluginSource): PluginInfo {
        val isDisabled = File(dir, "disable").exists()
        return PluginInfo(
            id = id,
            name = props["name"] ?: id,
            version = props["version"] ?: "v1.0",
            versionCode = props["versionCode"]?.toIntOrNull() ?: 1,
            author = props["author"] ?: "",
            description = props["description"] ?: "",
            isEnabled = !isDisabled,
            hasAction = File(dir, "action.sh").exists(),
            hasWebUI = File(dir, "webroot/index.html").exists(),
            installedAt = dir.lastModified(),
            source = source
        )
    }

    fun getPlugin(id: String): PluginInfo? = plugins[id]

    fun getPluginStatus(id: String): String {
        val plugin = plugins[id] ?: return "NOT_FOUND"
        return buildString {
            appendLine("id=${plugin.id}")
            appendLine("name=${plugin.name}")
            appendLine("version=${plugin.version}")
            appendLine("enabled=${plugin.isEnabled}")
            appendLine("hasAction=${plugin.hasAction}")
            appendLine("hasWebUI=${plugin.hasWebUI}")
        }
    }

    fun listPlugins(): List<String> {
        return plugins.values.map { it.id }
    }

    fun cleanAllMarked(): Int {
        var cleaned = 0
        val dirs = listOf(pluginsDir, AXMANAGER_PLUGINS_DIR, MAGISK_MODULES_DIR, KSU_MODULES_DIR)
        for (dirPath in dirs) {
            val dir = File(dirPath)
            if (!dir.exists()) continue
            dir.listFiles()?.forEach { pluginDir ->
                if (pluginDir.isDirectory && File(pluginDir, "remove").exists()) {
                    runBlocking { cleanupPlugin(pluginDir) }
                    cleaned++
                }
            }
        }
        return cleaned
    }

    fun disable(id: String): Boolean {
        val pluginDir = findPluginDir(id) ?: return false
        return try {
            File(pluginDir, "disable").createNewFile()
            val current = plugins[id] ?: return false
            plugins[id] = current.copy(isEnabled = false)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun enable(id: String): Boolean {
        val pluginDir = findPluginDir(id) ?: return false
        return try {
            File(pluginDir, "disable").delete()
            val current = plugins[id] ?: return false
            plugins[id] = current.copy(isEnabled = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun markForRemoval(id: String): Boolean {
        val pluginDir = findPluginDir(id) ?: return false
        return try {
            File(pluginDir, "remove").createNewFile()
            plugins.remove(id)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun findPluginDir(id: String): File? {
        val candidates = listOf(
            File(pluginsDir, id),
            File(AXMANAGER_PLUGINS_DIR, id),
            File(MAGISK_MODULES_DIR, id),
            File(KSU_MODULES_DIR, id)
        )
        return candidates.firstOrNull { it.exists() }
    }

    suspend fun runAction(id: String): Boolean {
        val plugin = plugins[id] ?: return false
        val pluginDir = findPluginDir(id) ?: return false
        val actionScript = File(pluginDir, "action.sh")
        if (!actionScript.exists()) return false
        val modDir = actionScript.parentFile?.absolutePath ?: return false

        val env: Map<String, String> = mapOf(
            "BETA" to "true",
            "BETAVER" to "$SERVER_VERSION",
            "AXERON" to "true",
            "AXERONVER" to "$SERVER_VERSION",
            "MODDIR" to modDir,
            "MODPATH" to modDir,
            "ARCH" to (System.getProperty("os.arch") ?: "arm64"),
            "API" to android.os.Build.VERSION.SDK_INT.toString()
        )

        val envStr = env.entries.joinToString(" ") { (k, v) -> "${k}=${v}" }
        val result = Shell.executeWithElevation("$envStr sh '${actionScript.absolutePath}'")
        return result is Shell.Result.Success
    }

    suspend fun runBootScripts() {
        for (plugin in plugins.values.filter { it.isEnabled }) {
            val dir = findPluginDir(plugin.id) ?: continue
            val postFsData = File(dir, "post-fs-data.sh")
            val serviceSh = File(dir, "service.sh")

            val env: Map<String, String> = mapOf(
                "BETA" to "true",
                "BETAVER" to "$SERVER_VERSION",
                "AXERON" to "true",
                "AXERONVER" to "$SERVER_VERSION",
                "MODDIR" to dir.absolutePath,
                "MODPATH" to dir.absolutePath,
                "ARCH" to (System.getProperty("os.arch") ?: "arm64"),
                "API" to android.os.Build.VERSION.SDK_INT.toString(),
                "IS64BIT" to if (android.os.Process.is64Bit()) "true" else "false",
                "BOOTMODE" to "true"
            )
            val envStr = env.entries.joinToString(" ") { (k, v) -> "${k}=${v}" }

            if (postFsData.exists()) {
                Shell.executeWithElevation("$envStr sh '${postFsData.absolutePath}'")
            }
            if (serviceSh.exists()) {
                Shell.executeWithElevation("$envStr sh '${serviceSh.absolutePath}' &")
            }
        }
    }

    private suspend fun cleanupPlugin(dir: File) {
        val uninstallScript = File(dir, "uninstall.sh")
        if (uninstallScript.exists()) {
            Shell.executeWithElevation("sh '${uninstallScript.absolutePath}'")
        }
        dir.deleteRecursively()
    }

    private fun parseModuleProp(file: File): Map<String, String> {
        val props = mutableMapOf<String, String>()
        try {
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val eqIndex = trimmed.indexOf('=')
                    if (eqIndex > 0) {
                        val key = trimmed.substring(0, eqIndex).trim()
                        val value = trimmed.substring(eqIndex + 1).trim()
                        props[key] = value
                    }
                }
            }
        } catch (_: Exception) {}
        return props
    }
}
