package beta.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import beta.manager.utils.PreferencesManager
import beta.manager.utils.Shell
import beta.manager.utils.ShizukuShell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val hasElevatedPrivileges: Boolean
        get() = Shell.isRootAvailableSync() || ShizukuShell.hasPermissionSync()

    private val _settings = MutableStateFlow(PreferencesManager.Settings())
    val settings: StateFlow<PreferencesManager.Settings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.settingsFlow.collect { s ->
                _settings.value = s
                Shell.setDebugMode(s.advDebug)
            }
        }
    }

    fun setGamingMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setGamingMode(enabled)
            applyPerformanceTweaks()
        }
    }

    fun setAutoStart(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoStart(enabled)
            val bootPrefs = getApplication<Application>()
                .getSharedPreferences("beta_boot_prefs", android.content.Context.MODE_PRIVATE)
            bootPrefs.edit().putBoolean("auto_start", enabled).apply()
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotifications(enabled) }
    }

    fun setThermalControl(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setThermalControl(enabled)
            if (!hasElevatedPrivileges) {
                android.util.Log.w("SettingsViewModel", "Thermal Control requires root or Shizuku")
                return@launch
            }
            if (enabled) {
                Shell.executeWithElevation("sh -c 'echo performance > /sys/class/thermal/thermal_message/sconfig 2>/dev/null || echo 0 > /sys/class/thermal/thermal_zone0/mode 2>/dev/null'")
            } else {
                Shell.executeWithElevation("sh -c 'echo disabled > /sys/class/thermal/thermal_message/sconfig 2>/dev/null || echo 1 > /sys/class/thermal/thermal_zone0/mode 2>/dev/null'")
            }
        }
    }

    fun setCpuGovernor(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setCpuGovernor(enabled)
            if (!hasElevatedPrivileges) {
                android.util.Log.w("SettingsViewModel", "CPU Governor requires root or Shizuku")
                return@launch
            }
            if (enabled) {
                Shell.executeWithElevation("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$cpu 2>/dev/null; done")
            } else {
                Shell.executeWithElevation("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$cpu 2>/dev/null; done")
            }
        }
    }

    fun setGpuBoost(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setGpuBoost(enabled)
            if (!hasElevatedPrivileges) {
                android.util.Log.w("SettingsViewModel", "GPU Boost requires root or Shizuku")
                return@launch
            }
            if (enabled) {
                Shell.executeWithElevation("sh -c 'echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null; echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null'")
            } else {
                Shell.executeWithElevation("sh -c 'echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null; echo 0 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null'")
            }
        }
    }

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDebugMode(enabled) }
    }

    fun setBusyboxMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBusyboxMode(enabled)
            if (enabled) {
                // Try to find and activate BusyBox in standalone mode
                Shell.execute("ASH_STANDALONE=1 busybox sh -c 'echo BusyBox activated' 2>/dev/null || true")
            }
        }
    }

    fun setShizukuMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setShizukuMode(enabled) }
    }

    fun setMagiskModuleMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setMagiskModuleMode(enabled) }
    }

    fun setKsuModuleMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setKsuModuleMode(enabled) }
    }

    fun setAxeronModuleMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setAxeronModuleMode(enabled) }
    }

    fun setFlashModule(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlashModule(enabled) }
    }

    fun setAdvDebug(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAdvDebug(enabled)
            Shell.setDebugMode(enabled)
            if (enabled) {
                Shell.executeWithElevation("setprop debug.beta.manager 1 2>/dev/null")
            } else {
                Shell.executeWithElevation("setprop debug.beta.manager 0 2>/dev/null")
            }
        }
    }

    private suspend fun applyPerformanceTweaks() {
        val s = _settings.value
        if (s.gamingMode) {
            if (s.cpuGovernor) Shell.executeWithElevation("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$cpu 2>/dev/null; done")
            if (s.gpuBoost) Shell.executeWithElevation("echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null")
        }
    }
}
