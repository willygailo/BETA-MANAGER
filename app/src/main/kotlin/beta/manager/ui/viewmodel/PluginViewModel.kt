package beta.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import beta.manager.plugin.PluginInfo
import beta.manager.plugin.PluginInstaller
import beta.manager.plugin.PluginManager
import beta.manager.utils.RootType
import beta.manager.utils.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PluginUiState(
    val plugins: List<PluginInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isInstalling: Boolean = false,
    val installResult: String? = null,
    val actionOutput: String? = null,
    val isFixing: Boolean = false,
    val fixResult: String? = null,
    val selectedPlugin: PluginInfo? = null,
    val flashMode: FlashMode = FlashMode.BETA,
    val detectedRootType: RootType = RootType.NONE,
    val installationProgress: Float = 0f,
    val installationStep: String = ""
)

enum class FlashMode { BETA, MAGISK, KSU, APATCH, AXERON }

class PluginViewModel : ViewModel() {

    private val pluginManager = PluginManager("/data/user_de/0/com.android.shell/beta/plugins/")
    private val pluginInstaller = PluginInstaller(
        "/data/user_de/0/com.android.shell/beta/plugins/",
        "/data/user_de/0/com.android.shell/beta/"
    )

    private val _uiState = MutableStateFlow(PluginUiState())
    val uiState: StateFlow<PluginUiState> = _uiState.asStateFlow()

    init {
        detectRootType()
        loadPlugins()
    }

    private fun detectRootType() {
        viewModelScope.launch {
            val type = Shell.refreshRootType()
            val defaultMode = when (type) {
                RootType.MAGISK -> FlashMode.MAGISK
                RootType.KERNELSU -> FlashMode.KSU
                RootType.APATCH -> FlashMode.APATCH
                RootType.AXERON -> FlashMode.AXERON
                else -> FlashMode.BETA
            }
            _uiState.value = _uiState.value.copy(
                detectedRootType = type,
                flashMode = defaultMode
            )
        }
    }

    fun loadPlugins() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val plugins = pluginManager.scanPlugins()
                _uiState.value = _uiState.value.copy(
                    plugins = plugins,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load plugins: ${e.message}"
                )
            }
        }
    }

    fun selectPlugin(plugin: PluginInfo?) {
        _uiState.value = _uiState.value.copy(selectedPlugin = plugin)
    }

    fun setFlashMode(mode: FlashMode) {
        _uiState.value = _uiState.value.copy(flashMode = mode)
    }

    fun togglePlugin(id: String) {
        val plugin = _uiState.value.plugins.find { it.id == id } ?: return
        viewModelScope.launch {
            try {
                if (plugin.isEnabled) pluginManager.disable(id)
                else pluginManager.enable(id)
            } catch (_: Exception) {}
            loadPlugins()
        }
    }

    fun removePlugin(id: String) {
        viewModelScope.launch {
            try {
                pluginManager.markForRemoval(id)
            } catch (_: Exception) {}
            loadPlugins()
        }
    }

    fun runAction(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionOutput = "Running action...")
            try {
                val success = pluginManager.runAction(id)
                _uiState.value = _uiState.value.copy(
                    actionOutput = if (success) "✓ Action completed successfully" else "✗ Action failed or not available"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(actionOutput = "Action failed: ${e.message}")
            }
        }
    }

    fun installZip(zipPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isInstalling = true,
                installResult = null,
                installationProgress = 0f,
                installationStep = "Validating"
            )
            val progressCallback = object : PluginInstaller.ProgressCallback {
                override fun onProgress(step: Int, total: Int, label: String) {
                    _uiState.value = _uiState.value.copy(
                        installationProgress = step.toFloat() / total,
                        installationStep = label
                    )
                }
            }
            try {
                val mode = _uiState.value.flashMode
                val result = when (mode) {
                    FlashMode.BETA -> {
                        val success = pluginInstaller.install(zipPath, progressCallback)
                        Pair(success, if (success) "Plugin installed successfully" else "Installation failed")
                    }
                    FlashMode.MAGISK -> {
                        val r = pluginInstaller.installToMagisk(zipPath, progressCallback)
                        Pair(r.success, r.message)
                    }
                    FlashMode.KSU -> {
                        val r = pluginInstaller.installToKSU(zipPath, progressCallback)
                        Pair(r.success, r.message)
                    }
                    FlashMode.APATCH -> {
                        val r = pluginInstaller.installToAPatch(zipPath, progressCallback)
                        Pair(r.success, r.message)
                    }
                    FlashMode.AXERON -> {
                        val r = pluginInstaller.installToAxeron(zipPath, progressCallback)
                        Pair(r.success, r.message)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isInstalling = false,
                    installationProgress = 1f,
                    installationStep = "Complete",
                    installResult = "${if (result.first) "✓" else "✗"} ${result.second}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isInstalling = false,
                    installationProgress = 0f,
                    installationStep = "",
                    installResult = "✗ Error: ${e.message}"
                )
            }
            loadPlugins()
        }
    }

    fun fixUpdateAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFixing = true, fixResult = null)
            try {
                val plugins = pluginManager.scanPlugins()
                val fixed = pluginInstaller.fixAllPlugins(plugins)
                val updates = pluginInstaller.checkForUpdates(plugins)
                val pending = updates.count { it.needsUpdate }
                _uiState.value = _uiState.value.copy(
                    isFixing = false,
                    fixResult = "✓ Fixed $fixed plugins, $pending updates available"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFixing = false,
                    fixResult = "✗ Fix failed: ${e.message}"
                )
            }
            loadPlugins()
        }
    }

    fun clearInstallResult() {
        _uiState.value = _uiState.value.copy(installResult = null)
    }

    fun clearActionOutput() {
        _uiState.value = _uiState.value.copy(actionOutput = null)
    }

    fun clearFixResult() {
        _uiState.value = _uiState.value.copy(fixResult = null)
    }
}
