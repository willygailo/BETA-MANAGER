package beta.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import beta.manager.adb.ActivationMode
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToPlugins: () -> Unit,
    onNavigateToGameProfiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToShell: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("BETA MANAGER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonCyan)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { StatusCard(uiState) }

            item {
                Text("Activation", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            item {
                ActivationSelector(
                    modes = uiState.availableModes,
                    isActivating = uiState.isActivating,
                    onActivate = { mode -> viewModel.activate(mode) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameBoostCard(
                        isActive = uiState.isGameBoosted,
                        isBoosting = uiState.isBoosting,
                        onToggle = { viewModel.toggleGameBoost() },
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Shell",
                        subtitle = "Command line",
                        color = NeonGreen,
                        onClick = onNavigateToShell,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            item {
                QuickActionsGrid(
                    pluginCount = uiState.pluginCount,
                    profileCount = uiState.profileCount,
                    onPlugins = onNavigateToPlugins,
                    onProfiles = onNavigateToGameProfiles,
                    onSettings = onNavigateToSettings
                )
            }

            if (uiState.activationLog.isNotBlank()) {
                item { ActivationLog(uiState.activationLog) }
            }
        }
    }
}

@Composable
private fun GameBoostCard(isActive: Boolean, isBoosting: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    Card(
        onClick = { if (!isBoosting) onToggle() },
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) NeonGreen.copy(alpha = 0.15f) else DarkCard
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isBoosting) {
                CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("⚡", fontSize = 24.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (isActive) "BOOSTED" else "GAME BOOST",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isActive) NeonGreen else TextPrimary
            )
        }
    }
}

@Composable
private fun StatusCard(state: beta.manager.ui.viewmodel.HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (state.isServiceRunning) NeonGreen else NeonRed))
                Text(
                    if (state.isServiceRunning) "Service Running" else "Service Stopped",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isServiceRunning) NeonGreen else NeonRed
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Plugins", "${state.pluginCount}")
                StatItem("Active", "${state.activeCount}")
                StatItem("Mode", state.activeMode)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun ActivationSelector(modes: List<ActivationMode>, isActivating: Boolean, onActivate: (ActivationMode) -> Unit) {
    val modeInfo = mapOf(
        ActivationMode.WIRELESS_DEBUG to "Wireless Debug",
        ActivationMode.ADB_USB to "ADB (USB)",
        ActivationMode.TCP_MODE to "TCP Mode",
        ActivationMode.ROOT_SU to "Root / SU"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { mode ->
            Button(
                onClick = { onActivate(mode) },
                enabled = !isActivating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
            ) {
                if (isActivating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeonCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(modeInfo[mode] ?: mode.name, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(pluginCount: Int, profileCount: Int, onPlugins: () -> Unit, onProfiles: () -> Unit, onSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard("Plugins", "$pluginCount installed", NeonPurple, onPlugins, Modifier.weight(1f))
        ActionCard("Profiles", "$profileCount game profiles", NeonPink, onProfiles, Modifier.weight(1f))
        ActionCard("Settings", "Configure", NeonCyan, onSettings, Modifier.weight(1f))
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun ActivationLog(log: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Log", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Spacer(Modifier.height(8.dp))
            Text(log, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
