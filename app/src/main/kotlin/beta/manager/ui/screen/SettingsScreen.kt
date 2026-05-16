package beta.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                SettingsHeader("General")
            }
            item { SettingsToggleCard("Gaming Mode", "Auto-apply performance plugins when games launch", settings.gamingMode, Icons.Outlined.SportsEsports, NeonCyan) { viewModel.setGamingMode(it) } }
            item { SettingsToggleCard("Auto Start", "Start service on device boot", settings.autoStart, Icons.Outlined.PowerSettingsNew, NeonGreen) { viewModel.setAutoStart(it) } }
            item { SettingsToggleCard("Notifications", "Show service notifications", settings.notifications, Icons.Outlined.Notifications, NeonPurple) { viewModel.setNotifications(it) } }

            item {
                Spacer(Modifier.height(8.dp))
                SettingsHeader("Root & Privilege")
            }
            item { SettingsToggleCard("Shizuku Mode", "Use Shizuku for elevated shell (non-root)", settings.shizukuMode, Icons.Outlined.Security, NeonCyan) { viewModel.setShizukuMode(it) } }
            item { SettingsToggleCard("Flash as Magisk Module", "Install plugins as Magisk modules", settings.flashModule, Icons.Outlined.Extension, NeonOrange) { viewModel.setFlashModule(it) } }
            item { SettingsToggleCard("Magisk Module Mode", "Enable Magisk module management", settings.magiskModuleMode, Icons.Outlined.Extension, NeonGreen) { viewModel.setMagiskModuleMode(it) } }
            item { SettingsToggleCard("KernelSU Module Mode", "Enable KernelSU module management", settings.ksuModuleMode, Icons.Outlined.Extension, NeonPurple) { viewModel.setKsuModuleMode(it) } }
            item { SettingsToggleCard("Axeron Module Mode", "Enable Axeron Manager plugin support", settings.axeronModuleMode, Icons.Outlined.Extension, NeonPink) { viewModel.setAxeronModuleMode(it) } }

            item {
                Spacer(Modifier.height(8.dp))
                SettingsHeader("Performance")
            }
            item { SettingsToggleCard("Thermal Control", "Manage thermal throttling", settings.thermalControl, Icons.Outlined.Thermostat, NeonOrange) { viewModel.setThermalControl(it) } }
            item { SettingsToggleCard("CPU Governor", "Force performance governor", settings.cpuGovernor, Icons.Outlined.Memory, NeonCyan) { viewModel.setCpuGovernor(it) } }
            item { SettingsToggleCard("GPU Boost", "Apply GPU optimization tweaks", settings.gpuBoost, Icons.Outlined.Speed, NeonPink) { viewModel.setGpuBoost(it) } }

            item {
                Spacer(Modifier.height(8.dp))
                SettingsHeader("Debug & Advanced")
            }
            item { SettingsToggleCard("Debug Mode", "Show debug logs and shell access", settings.debugMode, Icons.Outlined.BugReport, NeonYellow) { viewModel.setDebugMode(it) } }
            item { SettingsToggleCard("Advanced Debug (SU)", "Verbose root shell logging and debug props", settings.advDebug, Icons.Outlined.Adb, NeonRed) { viewModel.setAdvDebug(it) } }
            item { SettingsToggleCard("BusyBox Mode", "Use BusyBox standalone mode", settings.busyboxMode, Icons.Outlined.Terminal, NeonGreen) { viewModel.setBusyboxMode(it) } }

            item { Spacer(Modifier.height(16.dp)) }
            item { AboutCard() }
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
private fun SettingsToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = iconColor,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "β",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Beta Manager",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "v1.3.0",
                style = MaterialTheme.typography.bodyMedium,
                color = NeonCyan
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Game Performance Optimizer",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Built with Kotlin + Jetpack Compose",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = TextTertiary
            )
        }
    }
}
