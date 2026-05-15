package beta.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import beta.manager.utils.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class GameProfile(
    val id: String,
    val name: String,
    val packageName: String,
    val isActive: Boolean = false,
    val isInstalled: Boolean = true
)

class GameProfilesViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val profilesFile = File(context.filesDir, "game_profiles.json")

    private val _profiles = MutableStateFlow<List<GameProfile>>(emptyList())
    val profiles: StateFlow<List<GameProfile>> = _profiles.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val saved = loadFromFile()
            val detected = detectInstalledGames()
            val merged = mergeProfiles(saved, detected)
            _profiles.value = merged
            _isLoading.value = false
        }
    }

    private fun detectInstalledGames(): List<GameProfile> {
        val knownGames = listOf(
            GameProfile("mlbb", "Mobile Legends", "com.mobile.legends"),
            GameProfile("codm", "COD Mobile", "com.activision.callofduty.shooter"),
            GameProfile("pubg", "PUBG Mobile", "com.tencent.ig"),
            GameProfile("genshin", "Genshin Impact", "com.miHoYo.GenshinImpact"),
            GameProfile("wildrift", "Wild Rift", "com.riotgames.league.wildrift"),
            GameProfile("farlight", "Farlight 84", "com.farlightgames.ig"),
            GameProfile("mlbbcn", "Mobile Legends CN", "com.mobilelegends.cn"),
            GameProfile("codmcn", "COD Mobile CN", "com.tencent.game"),
            GameProfile("pubgcn", "PUBG Mobile CN", "com.tencent.tmgp.pubgmhd"),
            GameProfile("freefire", "Free Fire", "com.dts.freefireth"),
            GameProfile("freefiremax", "Free Fire Max", "com.dts.freefiremax"),
            GameProfile("arena", "Arena of Valor", "com.tencent.tmgp.sgame"),
            GameProfile("valorant", "Valorant Mobile", "com.riotgames.valorant"),
            GameProfile("honkai", "Honkai: Star Rail", "com.miHoYo.hkrpg"),
            GameProfile("zzz", "Zenless Zone Zero", "com.miHoYo.zzz"),
            GameProfile("wuthering", "Wuthering Waves", "com.kuro.wutheringwaves"),
        )
        val pm = context.packageManager
        return knownGames.map { game ->
            val installed = try {
                pm.getPackageInfo(game.packageName, PackageManager.PackageInfoFlags.of(0))
                true
            } catch (_: Exception) { false }
            game.copy(isInstalled = installed)
        }
    }

    private fun loadFromFile(): List<GameProfile> {
        if (!profilesFile.exists()) return emptyList()
        return try {
            val json = JSONArray(profilesFile.readText())
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                GameProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    packageName = obj.getString("packageName"),
                    isActive = obj.optBoolean("isActive", false),
                    isInstalled = true
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun mergeProfiles(saved: List<GameProfile>, detected: List<GameProfile>): List<GameProfile> {
        val savedMap = saved.associateBy { it.id }
        return detected.map { detected ->
            val savedProfile = savedMap[detected.id]
            if (savedProfile != null) detected.copy(isActive = savedProfile.isActive)
            else detected
        }
    }

    private fun saveToFile() {
        try {
            val json = JSONArray(_profiles.value.map { p ->
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("packageName", p.packageName)
                    put("isActive", p.isActive)
                }
            })
            profilesFile.parentFile?.mkdirs()
            profilesFile.writeText(json.toString(2))
        } catch (_: Exception) {}
    }

    fun toggleProfile(id: String) {
        val updated = _profiles.value.map { p ->
            if (p.id == id) p.copy(isActive = !p.isActive) else p
        }
        _profiles.value = updated
        saveToFile()
    }

    suspend fun applyProfile(id: String) {
        val profile = _profiles.value.find { it.id == id } ?: return
        if (!profile.isActive) return
        val cmds = listOf(
            "for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$cpu 2>/dev/null; done",
            "echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null",
            "echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null"
        )
        withContext(Dispatchers.IO) {
            cmds.forEach { Shell.execute(it) }
        }
    }
}
