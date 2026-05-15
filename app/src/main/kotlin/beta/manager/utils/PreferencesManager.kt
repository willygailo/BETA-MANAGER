package beta.manager.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beta_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_GAMING_MODE = booleanPreferencesKey("gaming_mode")
        private val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_THERMAL_CONTROL = booleanPreferencesKey("thermal_control")
        private val KEY_CPU_GOVERNOR = booleanPreferencesKey("cpu_governor")
        private val KEY_GPU_BOOST = booleanPreferencesKey("gpu_boost")
        private val KEY_DEBUG_MODE = booleanPreferencesKey("debug_mode")
        private val KEY_BUSYBOX_MODE = booleanPreferencesKey("busybox_mode")
        private val KEY_GAME_BOOST_ACTIVE = booleanPreferencesKey("game_boost_active")
    }

    data class Settings(
        val gamingMode: Boolean = false,
        val autoStart: Boolean = false,
        val notifications: Boolean = true,
        val thermalControl: Boolean = false,
        val cpuGovernor: Boolean = false,
        val gpuBoost: Boolean = false,
        val debugMode: Boolean = false,
        val busyboxMode: Boolean = false,
        val gameBoostActive: Boolean = false
    )

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            gamingMode = prefs[KEY_GAMING_MODE] ?: false,
            autoStart = prefs[KEY_AUTO_START] ?: false,
            notifications = prefs[KEY_NOTIFICATIONS] ?: true,
            thermalControl = prefs[KEY_THERMAL_CONTROL] ?: false,
            cpuGovernor = prefs[KEY_CPU_GOVERNOR] ?: false,
            gpuBoost = prefs[KEY_GPU_BOOST] ?: false,
            debugMode = prefs[KEY_DEBUG_MODE] ?: false,
            busyboxMode = prefs[KEY_BUSYBOX_MODE] ?: false,
            gameBoostActive = prefs[KEY_GAME_BOOST_ACTIVE] ?: false
        )
    }

    suspend fun setGamingMode(enabled: Boolean) { context.dataStore.edit { it[KEY_GAMING_MODE] = enabled } }
    suspend fun setAutoStart(enabled: Boolean) { context.dataStore.edit { it[KEY_AUTO_START] = enabled } }
    suspend fun setNotifications(enabled: Boolean) { context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled } }
    suspend fun setThermalControl(enabled: Boolean) { context.dataStore.edit { it[KEY_THERMAL_CONTROL] = enabled } }
    suspend fun setCpuGovernor(enabled: Boolean) { context.dataStore.edit { it[KEY_CPU_GOVERNOR] = enabled } }
    suspend fun setGpuBoost(enabled: Boolean) { context.dataStore.edit { it[KEY_GPU_BOOST] = enabled } }
    suspend fun setDebugMode(enabled: Boolean) { context.dataStore.edit { it[KEY_DEBUG_MODE] = enabled } }
    suspend fun setBusyboxMode(enabled: Boolean) { context.dataStore.edit { it[KEY_BUSYBOX_MODE] = enabled } }
    suspend fun setGameBoostActive(enabled: Boolean) { context.dataStore.edit { it[KEY_GAME_BOOST_ACTIVE] = enabled } }
}
