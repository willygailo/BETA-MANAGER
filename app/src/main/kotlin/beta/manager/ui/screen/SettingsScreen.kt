package beta.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.SettingsViewModel
import beta.manager.utils.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionHeader("General") }
            item { SettingsToggle("Enable Gaming Mode", "Auto-apply performance plugins when games launch", settings.gamingMode) { viewModel.setGamingMode(it) } }
            item { SettingsToggle("Auto Start Service", "Start Beta service on device boot", settings.autoStart) { viewModel.setAutoStart(it) } }
            item { SettingsToggle("Notifications", "Show service notifications", settings.notifications) { viewModel.setNotifications(it) } }

            item { SectionHeader("Performance") }
            item { SettingsToggle("Thermal Throttle Control", "Manage thermal throttling", settings.thermalControl) { viewModel.setThermalControl(it) } }
            item { SettingsToggle("CPU Governor Override", "Force performance governor", settings.cpuGovernor) { viewModel.setCpuGovernor(it) } }
            item { SettingsToggle("GPU Boost", "Apply GPU optimization tweaks", settings.gpuBoost) { viewModel.setGpuBoost(it) } }

            item { SectionHeader("Debug") }
            item { SettingsToggle("Debug Mode", "Show debug logs and shell access", settings.debugMode) { viewModel.setDebugMode(it) } }
            item { SettingsToggle("BusyBox Mode", "Use BusyBox standalone mode", settings.busyboxMode) { viewModel.setBusyboxMode(it) } }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Beta Manager v1.0.0", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Game Performance Optimizer", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        Spacer(Modifier.height(8.dp))
                        Text("Built with Kotlin + Jetpack Compose", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                )
            )
        }
    }
}
