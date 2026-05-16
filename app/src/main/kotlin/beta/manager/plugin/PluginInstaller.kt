package beta.manager.plugin

import beta.manager.utils.Shell
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PluginInstaller(
    private val pluginsDir: String,
    private val baseDir: String
) {

    data class InstallResult(
        val success: Boolean,
        val message: String,
        val pluginId: String? = null
    )

    suspend fun install(zipPath: String): Boolean {
        val result = installInternal(zipPath, flashToMagisk = false)
        return result.success
    }

    suspend fun installToMagisk(zipPath: String): InstallResult {
        val magiskModules = listOf(
            "/data/adb/modules/",
            "/data/adb/modules_update/"
        )
        val targetDir = magiskModules.firstOrNull { File(it).exists() }
            ?: return InstallResult(false, "Magisk modules directory not found")

        val result = installInternal(zipPath, flashToMagisk = true, magiskTarget = targetDir)
        return result
    }

    suspend fun installToKSU(zipPath: String): InstallResult {
        val ksuModules = listOf(
            "/data/adb/ksu/modules/",
            "/data/adb/modules/"
        )
        val targetDir = ksuModules.firstOrNull { java.io.File(it).exists() }
            ?: return InstallResult(false, "KernelSU modules directory not found")

        val result = installInternal(zipPath, flashToMagisk = true, magiskTarget = targetDir)
        return result
    }

    suspend fun installToAPatch(zipPath: String): InstallResult {
        val apatchModules = listOf(
            "/data/adb/apatch/modules/",
            "/data/adb/modules/"
        )
        val targetDir = apatchModules.firstOrNull { java.io.File(it).exists() }
            ?: return InstallResult(false, "APatch modules directory not found")
        return installInternal(zipPath, flashToMagisk = true, magiskTarget = targetDir)
    }

    suspend fun installToAxeron(zipPath: String): InstallResult {
        val axeronModules = listOf(
            "/data/adb/axeron/modules/",
            "/data/adb/modules/"
        )
        val targetDir = axeronModules.firstOrNull { java.io.File(it).exists() }
            ?: return InstallResult(false, "Axeron Manager modules directory not found")
        return installInternal(zipPath, flashToMagisk = true, magiskTarget = targetDir)
    }

    private suspend fun installInternal(
        zipPath: String,
        flashToMagisk: Boolean = false,
        magiskTarget: String? = null
    ): InstallResult {
        val zipFile = File(zipPath)
        if (!zipFile.exists()) {
            return InstallResult(false, "ZIP file not found: $zipPath")
        }

        val tempRoot = File(baseDir).takeIf { it.exists() && it.canWrite() }
            ?: zipFile.parentFile
            ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        val tempDir = File(tempRoot, ".tmp_install_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            extractZip(zipFile, tempDir)

            val propFile = File(tempDir, "module.prop")
            if (!propFile.exists()) {
                return InstallResult(false, "module.prop not found in ZIP")
            }

            val props = parseModuleProp(propFile)
            val pluginId = props["id"] ?: return InstallResult(false, "module.prop missing 'id' field")

            val betaVersion = props["betaPlugin"]?.toIntOrNull()
            val axeronVersion = props["axeronPlugin"]?.toIntOrNull()
            val minVersion = betaVersion ?: axeronVersion ?: 0
            val newVersionCode = props["versionCode"]?.toIntOrNull() ?: 1

            if (!flashToMagisk && minVersion > PluginManager.SERVER_VERSION) {
                return InstallResult(
                    false,
                    "Plugin requires newer version (need ${minVersion}, have ${PluginManager.SERVER_VERSION})"
                )
            }

            if (!pluginId.matches(Regex("^[a-zA-Z_][a-zA-Z0-9._-]+$"))) {
                return InstallResult(false, "Invalid plugin ID format: $pluginId")
            }

            val targetDir = if (flashToMagisk && magiskTarget != null) {
                File(magiskTarget, pluginId).absolutePath
            } else {
                File(pluginsDir, pluginId).absolutePath
            }

            val removeResult = Shell.executeWithElevation("rm -rf ${Shell.quote(targetDir)}")
            if (removeResult is Shell.Result.Error) {
                return InstallResult(false, "Unable to clean target directory: ${removeResult.message}")
            }

            val mkdirResult = Shell.executeWithElevation("mkdir -p ${Shell.quote(targetDir)}")
            if (mkdirResult is Shell.Result.Error) {
                return InstallResult(false, "Unable to create target directory: ${mkdirResult.message}")
            }

            val copyResult = Shell.executeWithElevation(
                "cp -rf ${Shell.quote(tempDir.absolutePath)}/. ${Shell.quote(targetDir)}/"
            )
            if (copyResult is Shell.Result.Error) {
                return InstallResult(false, "Unable to copy module files: ${copyResult.message}")
            }

            val chmodResult = Shell.executeWithElevation("chmod -R 755 ${Shell.quote(targetDir)}")
            if (chmodResult is Shell.Result.Error) {
                return InstallResult(false, "Unable to set module permissions: ${chmodResult.message}")
            }

            val customizeSh = File(tempDir, "customize.sh")
            if (customizeSh.exists()) {
                val installedCustomizeSh = File(targetDir, "customize.sh").absolutePath
                val env: Map<String, String> = mapOf(
                    "BETA" to "true",
                    "BETAVER" to "${PluginManager.SERVER_VERSION}",
                    "AXERON" to "true",
                    "AXERONVER" to "${PluginManager.SERVER_VERSION}",
                    "MODPATH" to targetDir,
                    "MODDIR" to targetDir,
                    "ARCH" to (System.getProperty("os.arch") ?: "arm64"),
                    "API" to android.os.Build.VERSION.SDK_INT.toString(),
                    "IS64BIT" to if (android.os.Process.is64Bit()) "true" else "false",
                    "BOOTMODE" to "true",
                    "SKIPUNZIP" to ""
                )
                val envStr = env.entries.joinToString(" ") { (k, v) -> "${k}=${Shell.quote(v)}" }
                val customizeResult = Shell.executeWithElevation(
                    "$envStr sh ${Shell.quote(installedCustomizeSh)}"
                )
                if (customizeResult is Shell.Result.Error) {
                    return InstallResult(false, "customize.sh failed: ${customizeResult.message}")
                }
            }

            Shell.executeWithElevation("rm -f ${Shell.quote("$targetDir/disable")} 2>/dev/null")

            val action = if (flashToMagisk) "Flashed to ${File(targetDir).parentFile?.name}" else "Installed"
            return InstallResult(true, "$action successfully: ${props["name"] ?: pluginId} (v${props["version"] ?: "1.0"})", pluginId)

        } catch (e: Exception) {
            return InstallResult(false, "Installation failed: ${e.message}")
        } finally {
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        }
    }

    suspend fun checkForUpdates(plugins: List<PluginInfo>): List<UpdateInfo> {
        val updates = mutableListOf<UpdateInfo>()
        for (plugin in plugins) {
            val pluginDir = File(pluginsDir, plugin.id)
            val propFile = File(pluginDir, "module.prop")
            if (!propFile.exists()) continue

            val props = parseModuleProp(propFile)
            val versionCode = props["versionCode"]?.toIntOrNull() ?: 1
            val name = props["name"] ?: plugin.id

            updates.add(UpdateInfo(
                id = plugin.id,
                name = name,
                currentVersion = plugin.version,
                currentVersionCode = plugin.versionCode,
                newVersionCode = versionCode,
                needsUpdate = versionCode > plugin.versionCode
            ))
        }
        return updates
    }

    suspend fun fixPlugin(pluginId: String): Boolean {
        val pluginDir = File(pluginsDir, pluginId)
        if (!pluginDir.exists() || !pluginDir.canRead()) {
            val pluginPath = pluginDir.absolutePath
            val exists = Shell.executeWithElevation(
                "[ -f ${Shell.quote("$pluginPath/module.prop")} ] && echo ok",
                timeout = 10000L
            )
            if (exists !is Shell.Result.Success || exists.output.trim() != "ok") return false

            val result = Shell.executeWithElevation(
                "rm -f ${Shell.quote("$pluginPath/disable")} ${Shell.quote("$pluginPath/remove")}; " +
                    "find ${Shell.quote(pluginPath)} -type f -name '*.sh' -exec chmod 755 {} \\; 2>/dev/null; " +
                    "[ -d ${Shell.quote("$pluginPath/webroot")} ] && chmod -R 755 ${Shell.quote("$pluginPath/webroot")} || true",
                timeout = 15000L
            )
            return result is Shell.Result.Success
        }

        val propFile = File(pluginDir, "module.prop")
        if (!propFile.exists()) return false

        File(pluginDir, "disable").delete()
        File(pluginDir, "remove").delete()

        val shFiles = pluginDir.listFiles { f -> f.extension == "sh" } ?: emptyArray()
        for (sh in shFiles) {
            sh.setExecutable(true)
        }

        val webrootDir = File(pluginDir, "webroot")
        if (webrootDir.exists()) {
            webrootDir.listFiles()?.forEach { it.setExecutable(true) }
        }

        return true
    }

    suspend fun fixAllPlugins(plugins: List<PluginInfo>): Int {
        var fixed = 0
        for (plugin in plugins) {
            if (fixPlugin(plugin.id)) fixed++
        }
        return fixed
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        val canonicalTarget = targetDir.canonicalFile
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outputFile = File(targetDir, entry.name)
                val canonicalOutput = outputFile.canonicalFile
                if (!canonicalOutput.path.startsWith(canonicalTarget.path + File.separator)) {
                    throw SecurityException("Blocked unsafe ZIP entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    outputFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                    if (!entry.name.startsWith("webroot/")) {
                        outputFile.setExecutable(true)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun parseModuleProp(file: File): Map<String, String> {
        val props = mutableMapOf<String, String>()
        try {
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val eqIndex = trimmed.indexOf('=')
                    if (eqIndex > 0) {
                        props[trimmed.substring(0, eqIndex).trim()] =
                            trimmed.substring(eqIndex + 1).trim()
                    }
                }
            }
        } catch (_: Exception) {}
        return props
    }
}
