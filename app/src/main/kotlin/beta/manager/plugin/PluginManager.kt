package beta.manager.plugin

import beta.manager.utils.Shell
import kotlinx.coroutines.runBlocking
import java.io.File

enum class PluginSource { BETA, AXMANAGER, MAGISK, KSU, APATCH, AXERON }

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
        const val SERVER_VERSION = 10004
        private val AXMANAGER_PLUGINS_DIR = "/data/user_de/0/com.android.shell/axeron/plugins/"
        private val MAGISK_MODULES_DIR = "/data/adb/modules/"
        private val KSU_MODULES_DIR = "/data/adb/ksu/modules/"
        private val APATCH_MODULES_DIR = "/data/adb/apatch/modules/"
        private val AXERON_MODULES_DIR = "/data/adb/axeron/modules/"
    }

    private val plugins = mutableMapOf<String, PluginInfo>()
    private val pluginPaths = mutableMapOf<String, String>()

    fun scanPluginsSync(): List<PluginInfo> {
        plugins.clear()
        pluginPaths.clear()
        scanSingleDirSync(pluginsDir, PluginSource.BETA)
        scanSingleDirSync(AXMANAGER_PLUGINS_DIR, PluginSource.AXMANAGER)
        scanModulesDirSync(MAGISK_MODULES_DIR, PluginSource.MAGISK)
        scanModulesDirSync(KSU_MODULES_DIR, PluginSource.KSU)
        scanModulesDirSync(APATCH_MODULES_DIR, PluginSource.APATCH)
        scanModulesDirSync(AXERON_MODULES_DIR, PluginSource.AXERON)
        return plugins.values.toList()
    }

    private fun scanSingleDirSync(dirPath: String, source: PluginSource) {
        val dir = File(dirPath)
        if (dir.exists() && dir.canRead()) {
            dir.listFiles()?.forEach { pluginDir ->
                if (pluginDir.isDirectory) scanPluginDir(pluginDir, source)
            }
        } else {
            scanElevatedDirSync(dirPath, source)
        }
    }

    private fun scanModulesDirSync(dirPath: String, source: PluginSource) {
        val dir = File(dirPath)
        if (dir.exists() && dir.canRead()) {
            dir.listFiles()?.forEach { pluginDir ->
                if (pluginDir.isDirectory) {
                    val propFile = File(pluginDir, "module.prop")
                    if (!propFile.exists()) return@forEach
                    val props = parseModuleProp(propFile)
                    val id = props["id"] ?: pluginDir.name
                    if (plugins.containsKey(id)) return@forEach
                    pluginPaths[id] = pluginDir.absolutePath
                    plugins[id] = buildPluginInfo(pluginDir, props, id, source)
                }
            }
        } else {
            scanElevatedDirSync(dirPath, source)
        }
    }

    private fun scanElevatedDirSync(dirPath: String, source: PluginSource) {
        val command = "for d in ${Shell.quote(dirPath)}/*; do [ -d \"\$d\" ] && [ -f \"\$d/module.prop\" ] && printf '%s\\n' \"\$d\"; done"
        val paths = when (val result = Shell.executeSyncWithElevation(command, timeout = 10000L)) {
            is Shell.Result.Success -> result.output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            is Shell.Result.Error -> emptyList()
        }

        for (path in paths) {
            val propResult = Shell.executeSyncWithElevation("cat ${Shell.quote("$path/module.prop")}", timeout = 10000L)
            if (propResult !is Shell.Result.Success) continue

            val propOutput = propResult.output
            val props = parseModuleProp(propOutput)
            val id = props["id"] ?: File(path).name
            if (plugins.containsKey(id)) continue

            val flags = when (val result = Shell.executeSyncWithElevation(
                "[ -e ${Shell.quote("$path/remove")} ] && echo remove; " +
                    "[ -e ${Shell.quote("$path/disable")} ] && echo disable; " +
                    "[ -e ${Shell.quote("$path/action.sh")} ] && echo action; " +
                    "[ -e ${Shell.quote("$path/webroot/index.html")} ] && echo webui",
                timeout = 10000L
            )) {
                is Shell.Result.Success -> result.output.lineSequence().map { it.trim() }.toSet()
                is Shell.Result.Error -> emptySet()
            }

            if ("remove" in flags) {
                cleanupPluginSync(path)
                continue
            }

            pluginPaths[id] = path
            plugins[id] = PluginInfo(
                id = id,
                name = props["name"] ?: id,
                version = props["version"] ?: "v1.0",
                versionCode = props["versionCode"]?.toIntOrNull() ?: 1,
                author = props["author"] ?: "",
                description = props["description"] ?: "",
                isEnabled = "disable" !in flags,
                hasAction = "action" in flags,
                hasWebUI = "webui" in flags,
                source = source
            )
        }
    }

    fun runActionSync(id: String): Boolean {
        val plugin = plugins[id] ?: return false
        val pluginPath = findPluginPath(id) ?: return false
        if (!plugin.hasAction) return false

        val env: Map<String, String> = mapOf(
            "BETA" to "true",
            "BETAVER" to "$SERVER_VERSION",
            "AXERON" to "true",
            "AXERONVER" to "$SERVER_VERSION",
            "MODDIR" to pluginPath,
            "MODPATH" to pluginPath,
            "ARCH" to (System.getProperty("os.arch") ?: "arm64"),
            "API" to android.os.Build.VERSION.SDK_INT.toString()
        )

        val envStr = env.entries.joinToString(" ") { (k, v) -> "${k}=${Shell.quote(v)}" }
        val result = Shell.executeSyncWithElevation("$envStr sh ${Shell.quote("$pluginPath/action.sh")}")
        return result is Shell.Result.Success
    }

    private fun cleanupPluginSync(path: String) {
        val dir = File(path)
        if (dir.exists() && dir.canWrite()) {
            val uninstallScript = File(dir, "uninstall.sh")
            if (uninstallScript.exists()) {
                Shell.executeSyncWithElevation("sh ${Shell.quote(uninstallScript.absolutePath)}")
            }
            dir.deleteRecursively()
            return
        }

        Shell.executeSyncWithElevation("[ -f ${Shell.quote("$path/uninstall.sh")} ] && sh ${Shell.quote("$path/uninstall.sh")} || true")
        Shell.executeSyncWithElevation("rm -rf ${Shell.quote(path)}")
    }

    fun scanPlugins(): List<PluginInfo> {
        plugins.clear()
        pluginPaths.clear()
        scanSingleDir(pluginsDir, PluginSource.BETA)
        scanSingleDir(AXMANAGER_PLUGINS_DIR, PluginSource.AXMANAGER)
        scanModulesDir(MAGISK_MODULES_DIR, PluginSource.MAGISK)
        scanModulesDir(KSU_MODULES_DIR, PluginSource.KSU)
        scanModulesDir(APATCH_MODULES_DIR, PluginSource.APATCH)
        scanModulesDir(AXERON_MODULES_DIR, PluginSource.AXERON)
        return plugins.values.toList()
    }

    private fun scanSingleDir(dirPath: String, source: PluginSource) {
        val dir = File(dirPath)
        if (dir.exists() && dir.canRead()) {
            dir.listFiles()?.forEach { pluginDir ->
                if (pluginDir.isDirectory) scanPluginDir(pluginDir, source)
            }
        } else {
            scanElevatedDir(dirPath, source)
        }
    }

    private fun scanModulesDir(dirPath: String, source: PluginSource) {
        val dir = File(dirPath)
        if (dir.exists() && dir.canRead()) {
            dir.listFiles()?.forEach { pluginDir ->
                if (pluginDir.isDirectory) {
                    val propFile = File(pluginDir, "module.prop")
                    if (!propFile.exists()) return@forEach
                    val props = parseModuleProp(propFile)
                    val id = props["id"] ?: pluginDir.name
                    if (plugins.containsKey(id)) return@forEach
                    pluginPaths[id] = pluginDir.absolutePath
                    plugins[id] = buildPluginInfo(pluginDir, props, id, source)
                }
            }
        } else {
            scanElevatedDir(dirPath, source)
        }
    }

    private fun scanPluginDir(pluginDir: File, source: PluginSource) = runBlocking {
        scanPluginDirSuspend(pluginDir, source)
    }

    private suspend fun scanPluginDirSuspend(pluginDir: File, source: PluginSource) {
        val propFile = File(pluginDir, "module.prop")
        if (!propFile.exists()) return
        val props = parseModuleProp(propFile)
        val id = props["id"] ?: pluginDir.name
        if (plugins.containsKey(id)) return
        if (File(pluginDir, "remove").exists()) {
            cleanupPlugin(pluginDir.absolutePath)
            return
        }
        pluginPaths[id] = pluginDir.absolutePath
        plugins[id] = buildPluginInfo(pluginDir, props, id, source)
    }

    private fun scanElevatedDir(dirPath: String, source: PluginSource) = runBlocking {
        scanElevatedDirSuspend(dirPath, source)
    }

    private suspend fun scanElevatedDirSuspend(dirPath: String, source: PluginSource) {
        val command = "for d in ${Shell.quote(dirPath)}/*; do [ -d \"\$d\" ] && [ -f \"\$d/module.prop\" ] && printf '%s\\n' \"\$d\"; done"
        val paths = when (val result = Shell.executeWithElevation(command, timeout = 10000L)) {
            is Shell.Result.Success -> result.output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            is Shell.Result.Error -> emptyList()
        }

        for (path in paths) {
            val propResult = Shell.executeWithElevation("cat ${Shell.quote("$path/module.prop")}", timeout = 10000L)
            if (propResult !is Shell.Result.Success) continue

            val propOutput = propResult.output
            val props = parseModuleProp(propOutput)
            val id = props["id"] ?: File(path).name
            if (plugins.containsKey(id)) continue

            val flags = when (val result = Shell.executeWithElevation(
                "[ -e ${Shell.quote("$path/remove")} ] && echo remove; " +
                    "[ -e ${Shell.quote("$path/disable")} ] && echo disable; " +
                    "[ -e ${Shell.quote("$path/action.sh")} ] && echo action; " +
                    "[ -e ${Shell.quote("$path/webroot/index.html")} ] && echo webui",
                timeout = 10000L
            )) {
                is Shell.Result.Success -> result.output.lineSequence().map { it.trim() }.toSet()
                is Shell.Result.Error -> emptySet()
            }

            if ("remove" in flags) {
                cleanupPlugin(path)
                continue
            }

            pluginPaths[id] = path
            plugins[id] = PluginInfo(
                id = id,
                name = props["name"] ?: id,
                version = props["version"] ?: "v1.0",
                versionCode = props["versionCode"]?.toIntOrNull() ?: 1,
                author = props["author"] ?: "",
                description = props["description"] ?: "",
                isEnabled = "disable" !in flags,
                hasAction = "action" in flags,
                hasWebUI = "webui" in flags,
                source = source
            )
        }
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

    fun cleanAllMarked(): Int = runBlocking {
        cleanAllMarkedSuspend()
    }

    private suspend fun cleanAllMarkedSuspend(): Int {
        var cleaned = 0
        val dirs = listOf(pluginsDir, AXMANAGER_PLUGINS_DIR, MAGISK_MODULES_DIR, KSU_MODULES_DIR, APATCH_MODULES_DIR, AXERON_MODULES_DIR)
        for (dirPath in dirs) {
            val dir = File(dirPath)
            if (dir.exists() && dir.canRead()) {
                dir.listFiles()?.forEach { pluginDir ->
                    if (pluginDir.isDirectory && File(pluginDir, "remove").exists()) {
                        cleanupPlugin(pluginDir.absolutePath)
                        cleaned++
                    }
                }
            } else {
                val command = "for d in ${Shell.quote(dirPath)}/*; do [ -d \"\$d\" ] && [ -f \"\$d/remove\" ] && printf '%s\\n' \"\$d\"; done"
                val paths = when (val result = Shell.executeWithElevation(command, timeout = 10000L)) {
                    is Shell.Result.Success -> result.output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                    is Shell.Result.Error -> emptyList()
                }
                for (path in paths) {
                    cleanupPlugin(path)
                    cleaned++
                }
            }
        }
        return cleaned
    }

    fun disable(id: String): Boolean = runBlocking {
        disableSuspend(id)
    }

    private suspend fun disableSuspend(id: String): Boolean {
        val pluginPath = findPluginPath(id) ?: return false
        return try {
            val disableFile = File(pluginPath, "disable")
            val success = if (disableFile.parentFile?.canWrite() == true) {
                disableFile.exists() || disableFile.createNewFile()
            } else {
                Shell.executeWithElevation("touch ${Shell.quote("$pluginPath/disable")}") is Shell.Result.Success
            }
            if (!success) return false
            val current = plugins[id] ?: return false
            plugins[id] = current.copy(isEnabled = false)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun enable(id: String): Boolean = runBlocking {
        enableSuspend(id)
    }

    private suspend fun enableSuspend(id: String): Boolean {
        val pluginPath = findPluginPath(id) ?: return false
        return try {
            val disableFile = File(pluginPath, "disable")
            val success = if (disableFile.parentFile?.canWrite() == true) {
                !disableFile.exists() || disableFile.delete()
            } else {
                Shell.executeWithElevation("rm -f ${Shell.quote("$pluginPath/disable")}") is Shell.Result.Success
            }
            if (!success) return false
            val current = plugins[id] ?: return false
            plugins[id] = current.copy(isEnabled = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun markForRemoval(id: String): Boolean = runBlocking {
        markForRemovalSuspend(id)
    }

    private suspend fun markForRemovalSuspend(id: String): Boolean {
        val pluginPath = findPluginPath(id) ?: return false
        return try {
            val removeFile = File(pluginPath, "remove")
            val success = if (removeFile.parentFile?.canWrite() == true) {
                removeFile.exists() || removeFile.createNewFile()
            } else {
                Shell.executeWithElevation("touch ${Shell.quote("$pluginPath/remove")}") is Shell.Result.Success
            }
            if (!success) return false
            plugins.remove(id)
            pluginPaths.remove(id)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun findPluginPath(id: String): String? {
        pluginPaths[id]?.let { return it }
        val candidates = listOf(
            File(pluginsDir, id),
            File(AXMANAGER_PLUGINS_DIR, id),
            File(MAGISK_MODULES_DIR, id),
            File(KSU_MODULES_DIR, id),
            File(APATCH_MODULES_DIR, id),
            File(AXERON_MODULES_DIR, id)
        )
        return candidates.firstOrNull { it.exists() }?.absolutePath
    }

    suspend fun runAction(id: String): Boolean {
        val plugin = plugins[id] ?: return false
        val pluginPath = findPluginPath(id) ?: return false
        if (!plugin.hasAction) return false

        val env: Map<String, String> = mapOf(
            "BETA" to "true",
            "BETAVER" to "$SERVER_VERSION",
            "AXERON" to "true",
            "AXERONVER" to "$SERVER_VERSION",
            "MODDIR" to pluginPath,
            "MODPATH" to pluginPath,
            "ARCH" to (System.getProperty("os.arch") ?: "arm64"),
            "API" to android.os.Build.VERSION.SDK_INT.toString()
        )

        val envStr = env.entries.joinToString(" ") { (k, v) -> "${k}=${Shell.quote(v)}" }
        val result = Shell.executeWithElevation("$envStr sh ${Shell.quote("$pluginPath/action.sh")}")
        return result is Shell.Result.Success
    }

    suspend fun runBootScripts() {
        for (plugin in plugins.values.filter { it.isEnabled }) {
            val pluginPath = findPluginPath(plugin.id) ?: continue
            val flags = when (val result = Shell.executeWithElevation(
                "[ -e ${Shell.quote("$pluginPath/post-fs-data.sh")} ] && echo post; " +
                    "[ -e ${Shell.quote("$pluginPath/service.sh")} ] && echo service",
                timeout = 10000L
            )) {
                is Shell.Result.Success -> result.output.lineSequence().map { it.trim() }.toSet()
                is Shell.Result.Error -> emptySet()
            }

            val env: Map<String, String> = mapOf(
                "BETA" to "true",
                "BETAVER" to "$SERVER_VERSION",
                "AXERON" to "true",
                "AXERONVER" to "$SERVER_VERSION",
                "MODDIR" to pluginPath,
                "MODPATH" to pluginPath,
                "ARCH" to (System.getProperty("os.arch") ?: "arm64"),
                "API" to android.os.Build.VERSION.SDK_INT.toString(),
                "IS64BIT" to if (android.os.Process.is64Bit()) "true" else "false",
                "BOOTMODE" to "true"
            )
            val envStr = env.entries.joinToString(" ") { (k, v) -> "${k}=${Shell.quote(v)}" }

            if ("post" in flags) {
                Shell.executeWithElevation("$envStr sh ${Shell.quote("$pluginPath/post-fs-data.sh")}")
            }
            if ("service" in flags) {
                Shell.executeWithElevation("$envStr sh ${Shell.quote("$pluginPath/service.sh")} &")
            }
        }
    }

    private suspend fun cleanupPlugin(path: String) {
        val dir = File(path)
        if (dir.exists() && dir.canWrite()) {
            val uninstallScript = File(dir, "uninstall.sh")
            if (uninstallScript.exists()) {
                Shell.executeWithElevation("sh ${Shell.quote(uninstallScript.absolutePath)}")
            }
            dir.deleteRecursively()
            return
        }

        Shell.executeWithElevation("[ -f ${Shell.quote("$path/uninstall.sh")} ] && sh ${Shell.quote("$path/uninstall.sh")} || true")
        Shell.executeWithElevation("rm -rf ${Shell.quote(path)}")
    }

    private fun parseModuleProp(file: File): Map<String, String> {
        return try {
            parseModuleProp(file.readText())
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseModuleProp(content: String): Map<String, String> {
        val props = mutableMapOf<String, String>()
        try {
            content.lineSequence().forEach { line ->
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
