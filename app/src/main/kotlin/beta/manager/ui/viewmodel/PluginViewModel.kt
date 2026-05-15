package beta.manager.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import beta.manager.plugin.PluginInfo
import beta.manager.plugin.PluginInstaller
import beta.manager.plugin.PluginManager
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
    val actionOutput: String? = null
)

class PluginViewModel : ViewModel() {

    private val pluginManager = PluginManager("/data/user_de/0/com.android.shell/beta/plugins/")
    private val pluginInstaller = PluginInstaller(
        "/data/user_de/0/com.android.shell/beta/plugins/",
        "/data/user_de/0/com.android.shell/beta/"
    )

    private val _uiState = MutableStateFlow(PluginUiState())
    val uiState: StateFlow<PluginUiState> = _uiState.asStateFlow()

    init {
        loadPlugins()
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
                pluginManager.runAction(id)
                _uiState.value = _uiState.value.copy(actionOutput = "Action completed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(actionOutput = "Action failed: ${e.message}")
            }
        }
    }

    fun installZip(zipPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInstalling = true, installResult = null)
            try {
                val result = pluginInstaller.install(zipPath)
                _uiState.value = _uiState.value.copy(
                    isInstalling = false,
                    installResult = if (result) "Plugin installed successfully" else "Installation failed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isInstalling = false,
                    installResult = "Error: ${e.message}"
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
}
