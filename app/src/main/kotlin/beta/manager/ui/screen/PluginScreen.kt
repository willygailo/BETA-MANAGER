package beta.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import beta.manager.ui.component.PluginCard
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.FlashMode
import beta.manager.ui.viewmodel.PluginViewModel
import beta.manager.utils.RootType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginScreen(
    viewModel: PluginViewModel,
    onNavigateBack: () -> Unit,
    onInstallPlugin: () -> Unit,
    onOpenWebUI: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.installResult) {
        uiState.installResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInstallResult()
        }
    }

    LaunchedEffect(uiState.fixResult) {
        uiState.fixResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFixResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Plugins", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fixUpdateAll() },
                        enabled = !uiState.isFixing
                    ) {
                        Icon(
                            imageVector = if (uiState.isFixing) Icons.Filled.Sync else Icons.Filled.Build,
                            contentDescription = "Fix/Update All",
                            tint = if (uiState.isFixing) NeonGreen else NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!uiState.isInstalling && !uiState.isFixing) onInstallPlugin()
                },
                containerColor = if (uiState.isInstalling) DarkSurfaceVariant else NeonCyan,
                contentColor = if (uiState.isInstalling) TextTertiary else DarkBackground,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = if (uiState.isInstalling) Icons.Filled.Sync else Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(if (uiState.isInstalling) "Installing..." else "Install Plugin")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Root type indicator + mismatch banner
            val detectedType = uiState.detectedRootType
            if (detectedType != RootType.NONE) {
                val isMismatch = when (detectedType) {
                    RootType.MAGISK -> uiState.flashMode != FlashMode.MAGISK
                    RootType.KERNELSU -> uiState.flashMode != FlashMode.KSU
                    RootType.APATCH -> uiState.flashMode != FlashMode.APATCH
                    RootType.AXERON -> uiState.flashMode != FlashMode.AXERON
                    else -> false
                }
                if (isMismatch) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonYellow.copy(alpha = 0.1f))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = NeonYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Detected ${detectedType.name.lowercase()}, but using ${uiState.flashMode.name} mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonYellow
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Detected: ${detectedType.name.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            // Installation progress
            if (uiState.isInstalling) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.installationProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonCyan,
                        trackColor = DarkSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            uiState.installationStep.takeIf { it.isNotBlank() } ?: "Installing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                }
            }

            if (uiState.isFixing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.1f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Fixing/updating all plugins...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Flash Mode selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlashMode.entries.forEach { mode ->
                    val isSelected = uiState.flashMode == mode
                    val chipColor = when (mode) {
                        FlashMode.BETA -> NeonCyan
                        FlashMode.MAGISK -> NeonGreen
                        FlashMode.KSU -> NeonPurple
                        FlashMode.APATCH -> NeonYellow
                        FlashMode.AXERON -> NeonPink
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFlashMode(mode) },
                        label = { Text(mode.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.2f),
                            selectedLabelColor = chipColor,
                            containerColor = DarkCard,
                            labelColor = TextTertiary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = chipColor,
                            borderColor = DarkSurfaceVariant
                        )
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading plugins...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            uiState.error!!,
                            color = NeonRed,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (uiState.plugins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.2f)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Extension,
                                contentDescription = null,
                                tint = NeonCyan.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No plugins installed",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap the button below to install a performance plugin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(uiState.plugins) { plugin ->
                        PluginCard(
                            plugin = plugin,
                            onToggle = { viewModel.togglePlugin(plugin.id) },
                            onRemove = { viewModel.removePlugin(plugin.id) },
                            onRunAction = { viewModel.runAction(plugin.id) },
                            onOpenWebUI = if (plugin.hasWebUI && onOpenWebUI != null) {
                                { onOpenWebUI(plugin.id) }
                            } else null,
                            showActionOutput = uiState.actionOutput
                        )
                    }
                }
            }
        }
    }
}
