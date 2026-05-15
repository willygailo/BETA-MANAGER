package beta.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import beta.manager.plugin.PluginInfo
import beta.manager.plugin.PluginSource
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToShell: () -> Unit,
    onInstallPlugin: (() -> Unit)? = null,
    onNavigateToLogs: (() -> Unit)? = null,
    onNavigateToSuperuser: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("β", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Beta Manager", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item { ServiceStatusHeader(uiState) }

            if (!uiState.isServiceRunning && !uiState.isActivating) {
                item { AutoActivateCard(uiState, viewModel::autoActivate) }
            }

            item { QuickActionsRow(uiState, viewModel, onNavigateToShell, onInstallPlugin, onNavigateToLogs, onNavigateToSuperuser) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Extension,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Modules",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        "${uiState.modules.size} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary
                    )
                }
            }

            if (uiState.modules.isEmpty()) {
                item { EmptyModulesCard() }
            } else {
                items(uiState.modules) { module ->
                    ModuleCard(
                        plugin = module,
                        onToggle = {
                            viewModel.activate(ActivationMode.ROOT_SU)
                            viewModel.refreshStatus()
                        },
                        onRemove = { viewModel.cleanAll() }
                    )
                }
            }

            if (uiState.activationLog.isNotBlank()) {
                item { ActivationLogCard(uiState.activationLog) }
            }
        }
    }
}

@Composable
private fun ServiceStatusHeader(state: beta.manager.ui.viewmodel.HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.isServiceRunning)
                            Brush.linearGradient(listOf(NeonGreen, NeonGreen.copy(alpha = 0.3f)))
                        else
                            Brush.linearGradient(listOf(NeonRed, NeonRed.copy(alpha = 0.3f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.isActivating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (state.isServiceRunning) Icons.Filled.CheckCircle else Icons.Filled.StopCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (state.isServiceRunning) "Service Running" else "Service Stopped",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isServiceRunning) NeonGreen else NeonRed
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.activeMode != "N/A") {
                        LabelBadge(state.activeMode.lowercase().replace('_', ' '), NeonCyan)
                    }
                    LabelBadge(state.rootType.lowercase(), NeonPurple)
                    if (state.isGameBoosted) {
                        LabelBadge("boosted", NeonGreen)
                    }
                }
            }
            if (state.isServiceRunning) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
            }
        }
    }
}

@Composable
private fun LabelBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun AutoActivateCard(
    state: beta.manager.ui.viewmodel.HomeUiState,
    onAutoActivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Tap to activate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Detected: ${state.rootType} · ${state.autoDetectedMode.lowercase().replace('_', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            Button(
                onClick = onAutoActivate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Activate", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    state: beta.manager.ui.viewmodel.HomeUiState,
    viewModel: HomeViewModel,
    onNavigateToShell: () -> Unit,
    onInstallPlugin: (() -> Unit)?,
    onNavigateToLogs: (() -> Unit)?,
    onNavigateToSuperuser: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniChip(
            icon = Icons.Filled.Add,
            label = "Install",
            color = NeonCyan,
            onClick = { onInstallPlugin?.invoke() },
            modifier = Modifier.weight(1f)
        )
        MiniChip(
            icon = Icons.Filled.CleaningServices,
            label = if (state.isCleaning) "..." else "Clean",
            color = NeonOrange,
            onClick = { viewModel.cleanAll() },
            modifier = Modifier.weight(1f)
        )
        MiniChip(
            icon = Icons.Filled.Terminal,
            label = "Shell",
            color = NeonGreen,
            onClick = onNavigateToShell,
            modifier = Modifier.weight(1f)
        )
        MiniChip(
            icon = if (state.isGameBoosted) Icons.Filled.Bolt else Icons.Filled.Speed,
            label = if (state.isGameBoosted) "Boost" else "Boost",
            color = if (state.isGameBoosted) NeonGreen else NeonPink,
            onClick = { viewModel.toggleGameBoost() },
            modifier = Modifier.weight(1f)
        )
        MiniChip(
            icon = Icons.Filled.Description,
            label = "Logs",
            color = NeonPurple,
            onClick = { onNavigateToLogs?.invoke() },
            modifier = Modifier.weight(1f)
        )
        MiniChip(
            icon = Icons.Filled.Shield,
            label = "SU",
            color = NeonYellow,
            onClick = { onNavigateToSuperuser?.invoke() },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MiniChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}



@Composable
private fun ModuleCard(
    plugin: PluginInfo,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val sourceColor = when (plugin.source) {
        PluginSource.BETA -> NeonCyan
        PluginSource.AXMANAGER -> NeonOrange
        PluginSource.MAGISK -> NeonGreen
        PluginSource.KSU -> NeonPurple
    }
    val sourceLabel = when (plugin.source) {
        PluginSource.BETA -> "BETA"
        PluginSource.AXMANAGER -> "AXRON"
        PluginSource.MAGISK -> "MAGISK"
        PluginSource.KSU -> "KSU"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plugin.isEnabled) DarkCard else DarkSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (plugin.isEnabled) 1.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (plugin.isEnabled)
                                Brush.linearGradient(listOf(sourceColor, sourceColor.copy(alpha = 0.4f)))
                            else
                                Brush.linearGradient(listOf(DarkSurfaceVariant, DarkSurfaceVariant))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Extension,
                        contentDescription = null,
                        tint = if (plugin.isEnabled) Color.White else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            plugin.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (plugin.isEnabled) TextPrimary else TextTertiary,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(sourceColor.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                sourceLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = sourceColor
                            )
                        }
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        "${plugin.version} · ${plugin.author.ifEmpty { "Unknown" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = sourceColor,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
            if (plugin.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyModulesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.15f), NeonPurple.copy(alpha = 0.15f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Extension,
                    contentDescription = null,
                    tint = NeonCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("No modules found", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Install a plugin ZIP or module to get started",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ActivationLogCard(log: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Activity Log", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            }
            Spacer(Modifier.height(6.dp))
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
