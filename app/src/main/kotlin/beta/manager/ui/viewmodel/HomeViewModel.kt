package beta.manager.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import beta.manager.IBetaService
import beta.manager.adb.ActivationMode
import beta.manager.adb.AdbActivator
import beta.manager.adb.AdbClient
import beta.manager.utils.PreferencesManager
import beta.manager.utils.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val pluginCount: Int = 0,
    val profileCount: Int = 0,
    val activeCount: Int = 0,
    val activeMode: String = "N/A",
    val availableModes: List<ActivationMode> = emptyList(),
    val isActivating: Boolean = false,
    val activationLog: String = "",
    val isGameBoosted: Boolean = false,
    val isBoosting: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val activator = AdbActivator(application)
    private val client = AdbClient(application)
    private val prefs = PreferencesManager(application)
    private var betaService: IBetaService? = null

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
        checkModes()
        checkService()
        viewModelScope.launch {
            prefs.settingsFlow.collect { s ->
                _uiState.value = _uiState.value.copy(isGameBoosted = s.gameBoostActive)
            }
        }
    }

    private fun checkModes() {
        viewModelScope.launch {
            val modes = activator.checkAllModes()
            val available = mutableListOf<ActivationMode>()
            if (modes[ActivationMode.WIRELESS_DEBUG] == true) available.add(ActivationMode.WIRELESS_DEBUG)
            if (modes[ActivationMode.ADB_USB] == true) available.add(ActivationMode.ADB_USB)
            if (modes[ActivationMode.TCP_MODE] == true) available.add(ActivationMode.TCP_MODE)
            if (modes[ActivationMode.ROOT_SU] == true) available.add(ActivationMode.ROOT_SU)
            if (available.isEmpty()) available.addAll(ActivationMode.entries)
            _uiState.value = _uiState.value.copy(availableModes = available)
        }
    }

    private fun checkService() {
        viewModelScope.launch {
            try {
                val running = client.isServiceRunning()
                _uiState.value = _uiState.value.copy(isServiceRunning = running)
                if (running) {
                    val service = betaService
                    val plugins = service?.listPlugins()?.toList() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        pluginCount = plugins.size,
                        activeCount = plugins.size
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun activate(mode: ActivationMode) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActivating = true,
                activationLog = "Activating via ${mode.name.lowercase().replace('_', ' ')}..."
            )
            val result = activator.activate(mode)
            _uiState.value = _uiState.value.copy(
                isActivating = false,
                isServiceRunning = result.success,
                activeMode = if (result.success) mode.name else "N/A",
                activationLog = if (result.success) "✓ ${result.message}" else "✗ ${result.message}"
            )
            if (result.success) refreshStatus()
        }
    }

    fun refreshStatus() { checkService() }

    fun toggleGameBoost() {
        viewModelScope.launch {
            val current = _uiState.value.isGameBoosted
            _uiState.value = _uiState.value.copy(isBoosting = true)
            if (!current) {
                Shell.execute("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$cpu 2>/dev/null; done")
                Shell.execute("echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null")
                Shell.execute("echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null")
                Shell.execute("echo 1 > /proc/sys/vm/compact_memory 2>/dev/null")
            } else {
                Shell.execute("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$cpu 2>/dev/null; done")
                Shell.execute("echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null")
            }
            prefs.setGameBoostActive(!current)
            _uiState.value = _uiState.value.copy(isGameBoosted = !current, isBoosting = false)
        }
    }
}
