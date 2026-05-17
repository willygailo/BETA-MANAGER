package beta.manager.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import beta.manager.IBetaService
import beta.manager.adb.ActivationMode
import beta.manager.adb.AdbActivator
import beta.manager.adb.AdbClient
import beta.manager.plugin.PluginInfo
import beta.manager.plugin.PluginManager
import beta.manager.service.BetaService
import beta.manager.utils.PreferencesManager
import beta.manager.utils.Shell
import beta.manager.utils.ShizukuShell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val pluginCount: Int = 0,
    val activeCount: Int = 0,
    val profileCount: Int = 0,
    val activeMode: String = "N/A",
    val availableModes: List<ActivationMode> = emptyList(),
    val isActivating: Boolean = false,
    val activationLog: String = "",
    val isGameBoosted: Boolean = false,
    val isBoosting: Boolean = false,
    val rootType: String = "N/A",
    val autoDetectedMode: String = "",
    val modules: List<PluginInfo> = emptyList(),
    val isLoaded: Boolean = false,
    val isCleaning: Boolean = false,
    val cleanResult: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val activator = AdbActivator(application)
    private val client = AdbClient(application)
    private val prefs = PreferencesManager(application)
    private val hasElevatedPrivileges: Boolean
        get() = Shell.isRootAvailableSync() || ShizukuShell.hasPermissionSync()
    private val pluginDir: String
        get() = if (hasElevatedPrivileges)
            "/data/user_de/0/com.android.shell/beta/plugins/"
        else
            getApplication<Application>().filesDir.absolutePath + "/beta/plugins/"
    private val pluginManager by lazy { PluginManager(pluginDir) }
    private var betaService: IBetaService? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            betaService = IBetaService.Stub.asInterface(service)
            refreshStatus()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            betaService = null
            _uiState.value = _uiState.value.copy(isServiceRunning = false)
        }
    }

    init {
        bindService()
        checkModes()
        viewModelScope.launch {
            prefs.settingsFlow.collect { s ->
                _uiState.value = _uiState.value.copy(isGameBoosted = s.gameBoostActive)
            }
        }
        viewModelScope.launch {
            val rootType = Shell.detectRootType()
            val detectedMode = activator.autoDetectMode()
            _uiState.value = _uiState.value.copy(
                rootType = rootType.name,
                autoDetectedMode = detectedMode.name
            )
            checkService()
            scanModules()
            // Do not auto-activate on first launch - let user choose
            _uiState.value = _uiState.value.copy(isLoaded = true)
        }
    }

    private fun bindService() {
        val app = getApplication<Application>()
        val intent = Intent(app, BetaService::class.java)
        try {
            ContextCompat.startForegroundService(app, intent)
            serviceBound = app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                activationLog = "Service bind failed: ${e.message}"
            )
        }
    }

    override fun onCleared() {
        if (serviceBound) {
            try {
                getApplication<Application>().unbindService(connection)
            } catch (_: Exception) {
            }
            serviceBound = false
        }
        super.onCleared()
    }

    private fun checkModes() {
        viewModelScope.launch {
            val modes = activator.checkAllModes()
            val available = mutableListOf<ActivationMode>()
            if (modes[ActivationMode.WIRELESS_DEBUG] == true) available.add(ActivationMode.WIRELESS_DEBUG)
            if (modes[ActivationMode.ADB_USB] == true) available.add(ActivationMode.ADB_USB)
            if (modes[ActivationMode.TCP_MODE] == true) available.add(ActivationMode.TCP_MODE)
            if (modes[ActivationMode.ROOT_SU] == true) available.add(ActivationMode.ROOT_SU)
            if (modes[ActivationMode.SHIZUKU] == true) available.add(ActivationMode.SHIZUKU)
            if (available.isEmpty()) available.addAll(ActivationMode.entries)
            _uiState.value = _uiState.value.copy(availableModes = available)
        }
    }

    private fun checkService() {
        viewModelScope.launch {
            try {
                val service = betaService
                val running = service?.isRunning ?: client.isServiceRunning()
                _uiState.value = _uiState.value.copy(isServiceRunning = running)
                if (running) {
                    val plugins = service?.listPlugins()?.toList() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        pluginCount = plugins.size,
                        activeCount = plugins.size,
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun scanModules() {
        viewModelScope.launch {
            val modules = pluginManager.scanPlugins()
            _uiState.value = _uiState.value.copy(
                modules = modules,
                pluginCount = modules.size,
                activeCount = modules.count { it.isEnabled }
            )
        }
    }

    fun autoActivate() {
        viewModelScope.launch {
            val mode = activator.autoDetectMode()
            val modeName = mode.name.lowercase().replace('_', ' ')
            _uiState.value = _uiState.value.copy(
                isActivating = true,
                activationLog = "Auto-detected: $modeName..."
            )
            val result = activator.activate(mode)
            _uiState.value = _uiState.value.copy(
                isActivating = false,
                isServiceRunning = result.success,
                activeMode = if (result.success) mode.name else "N/A",
                activationLog = if (result.success) "✓ ${result.message}" else "✗ ${result.message}"
            )
            if (result.success) refreshStatus()
            scanModules()
        }
    }

    fun activate(mode: ActivationMode) {
        viewModelScope.launch {
            val modeName = mode.name.lowercase().replace('_', ' ')
            _uiState.value = _uiState.value.copy(
                isActivating = true,
                activationLog = "Activating via $modeName..."
            )
            val result = activator.activate(mode)
            _uiState.value = _uiState.value.copy(
                isActivating = false,
                isServiceRunning = result.success,
                activeMode = if (result.success) mode.name else "N/A",
                activationLog = if (result.success) "✓ ${result.message}" else "✗ ${result.message}"
            )
            if (result.success) refreshStatus()
            scanModules()
        }
    }

    fun refreshStatus() {
        if (!serviceBound) bindService()
        checkService()
        scanModules()
    }

    fun cleanAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true, cleanResult = "")
            val cleaned = pluginManager.cleanAllMarked()
            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                cleanResult = "Cleaned $cleaned module(s)"
            )
            scanModules()
        }
    }

    fun toggleGameBoost() {
        viewModelScope.launch {
            val current = _uiState.value.isGameBoosted
            _uiState.value = _uiState.value.copy(isBoosting = true)
            if (!current) {
                val r1 = Shell.executeWithElevation("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$cpu 2>/dev/null; done")
                val r2 = Shell.executeWithElevation("echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null")
                val r3 = Shell.executeWithElevation("echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null")
                val r4 = Shell.executeWithElevation("echo 1 > /proc/sys/vm/compact_memory 2>/dev/null")
                val allFailed = listOf(r1, r2, r3, r4).all { it is Shell.Result.Error }
                if (allFailed) {
                    _uiState.value = _uiState.value.copy(
                        isBoosting = false,
                        activationLog = "Game Boost requires root or Shizuku permission"
                    )
                    return@launch
                }
            } else {
                Shell.executeWithElevation("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$cpu 2>/dev/null; done")
                Shell.executeWithElevation("echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null")
            }
            prefs.setGameBoostActive(!current)
            _uiState.value = _uiState.value.copy(isGameBoosted = !current, isBoosting = false)
        }
    }

    fun toggleModule(id: String) {
        viewModelScope.launch {
            val plugin = _uiState.value.modules.find { it.id == id } ?: return@launch
            try {
                if (plugin.isEnabled) pluginManager.disable(id)
                else pluginManager.enable(id)
            } catch (_: Exception) {}
            scanModules()
        }
    }

    fun removeModule(id: String) {
        viewModelScope.launch {
            try {
                pluginManager.markForRemoval(id)
            } catch (_: Exception) {}
            scanModules()
        }
    }
}
