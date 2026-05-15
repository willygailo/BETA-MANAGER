package beta.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import beta.manager.adb.ActivationMode
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToShell: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Beta Manager",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { WelcomeHeader() }
            item { ServiceStatusCard(uiState.isServiceRunning, uiState.activeMode) }
            item { StatsRow(uiState) }
            item { ActivationSection(uiState, viewModel::activate) }
            item { GameBoostCard(uiState, viewModel::toggleGameBoost) }
            item {
                Text(
                    "Quick Tools",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            item { QuickToolsRow(onNavigateToShell) }
            if (uiState.activationLog.isNotBlank()) {
                item { ActivationLogCard(uiState.activationLog) }
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "Welcome!",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Optimize your gaming performance",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun ServiceStatusCard(isRunning: Boolean, activeMode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning) Brush.linearGradient(listOf(NeonGreen, NeonGreen.copy(alpha = 0.4f)))
                        else Brush.linearGradient(listOf(NeonRed, NeonRed.copy(alpha = 0.4f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.CheckCircle else Icons.Filled.StopCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isRunning) "Service Running" else "Service Stopped",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) NeonGreen else NeonRed
                )
                Text(
                    "Mode: $activeMode",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
            }
        }
    }
}

@Composable
private fun StatsRow(state: beta.manager.ui.viewmodel.HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Plugins", "${state.pluginCount}", NeonCyan, Modifier.weight(1f))
        StatCard("Active", "${state.activeCount}", NeonGreen, Modifier.weight(1f))
        StatCard("Profiles", "${state.profileCount}", NeonPurple, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun ActivationSection(
    state: beta.manager.ui.viewmodel.HomeUiState,
    onActivate: (ActivationMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Activate Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(12.dp))

            val modeInfo = mapOf(
                ActivationMode.WIRELESS_DEBUG to "Wireless Debug",
                ActivationMode.ADB_USB to "ADB (USB)",
                ActivationMode.TCP_MODE to "TCP Mode",
                ActivationMode.ROOT_SU to "Root / SU"
            )
            val modeIcons = mapOf(
                ActivationMode.WIRELESS_DEBUG to Icons.Outlined.Wifi,
                ActivationMode.ADB_USB to Icons.Outlined.Usb,
                ActivationMode.TCP_MODE to Icons.Outlined.Lan,
                ActivationMode.ROOT_SU to Icons.Outlined.Security
            )
            val modeColors = mapOf(
                ActivationMode.WIRELESS_DEBUG to NeonCyan,
                ActivationMode.ADB_USB to NeonGreen,
                ActivationMode.TCP_MODE to NeonOrange,
                ActivationMode.ROOT_SU to NeonPurple
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableModes.forEach { mode ->
                    val color = modeColors[mode] ?: NeonCyan
                    OutlinedButton(
                        onClick = { onActivate(mode) },
                        enabled = !state.isActivating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = !state.isActivating),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = color
                        )
                    ) {
                        Icon(
                            imageVector = modeIcons[mode] ?: Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            modeInfo[mode] ?: mode.name,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.isActivating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = color,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameBoostCard(
    state: beta.manager.ui.viewmodel.HomeUiState,
    onToggle: () -> Unit
) {
    Card(
        onClick = { if (!state.isBoosting) onToggle() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isGameBoosted) DarkCard else DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (state.isGameBoosted)
                            Brush.linearGradient(listOf(NeonGreen, NeonGreen.copy(alpha = 0.3f)))
                        else
                            Brush.linearGradient(listOf(DarkSurfaceVariant, DarkSurfaceVariant))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = if (state.isGameBoosted) Color.White else TextTertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Game Boost",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isGameBoosted) NeonGreen else TextPrimary
                )
                Text(
                    if (state.isGameBoosted) "Performance mode active" else "Maximize gaming performance",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            if (state.isBoosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonGreen,
                    strokeWidth = 2.dp
                )
            } else {
                Switch(
                    checked = state.isGameBoosted,
                    onCheckedChange = { if (!state.isBoosting) onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickToolsRow(onNavigateToShell: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ToolCard(
            title = "Shell",
            subtitle = "Run commands",
            icon = Icons.Filled.Terminal,
            color = NeonGreen,
            onClick = onNavigateToShell,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun ActivationLogCard(log: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Activity Log",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                log,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
