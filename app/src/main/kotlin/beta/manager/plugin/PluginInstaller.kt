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
        val result = installInternal(zipPath)
        return result.success
    }

    private suspend fun installInternal(zipPath: String): InstallResult {
        val zipFile = File(zipPath)
        if (!zipFile.exists()) {
            return InstallResult(false, "ZIP file not found: $zipPath")
        }

        val tempDir = File(baseDir, ".tmp_install_${System.currentTimeMillis()}")
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

            if (minVersion > PluginManager.SERVER_VERSION) {
                return InstallResult(
                    false,
                    "Plugin requires newer version (need ${minVersion}, have ${PluginManager.SERVER_VERSION})"
                )
            }

            if (!pluginId.matches(Regex("^[a-zA-Z_][a-zA-Z0-9._-]+$"))) {
                return InstallResult(false, "Invalid plugin ID format: $pluginId")
            }

            val customizeSh = File(tempDir, "customize.sh")
            if (customizeSh.exists()) {
            val env: Map<String, String> = mapOf(
                "BETA" to "true",
                "BETAVER" to "${PluginManager.SERVER_VERSION}",
                "AXERON" to "true",
                "AXERONVER" to "${PluginManager.SERVER_VERSION}",
                "MODPATH" to "${pluginsDir}${pluginId}",
                "MODDIR" to "${pluginsDir}${pluginId}",
                "ARCH" to (System.getProperty("os.arch") ?: "arm64"),
                "API" to android.os.Build.VERSION.SDK_INT.toString(),
                "IS64BIT" to if (android.os.Process.is64Bit()) "true" else "false",
                "BOOTMODE" to "true",
                "SKIPUNZIP" to ""
            )
            val envStr = env.entries.joinToString(" ") { (k, v) -> "${k}=${v}" }
            Shell.execute("$envStr sh '${customizeSh.absolutePath}'")
            }

            val pluginDir = File(pluginsDir, pluginId)
            if (pluginDir.exists()) {
                pluginDir.deleteRecursively()
            }
            tempDir.copyRecursively(pluginDir, overwrite = true)
            File(pluginDir, "disable")?.delete()

            return InstallResult(true, "Plugin installed successfully", pluginId)

        } catch (e: Exception) {
            return InstallResult(false, "Installation failed: ${e.message}")
        } finally {
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        }
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outputFile = File(targetDir, entry.name)
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
