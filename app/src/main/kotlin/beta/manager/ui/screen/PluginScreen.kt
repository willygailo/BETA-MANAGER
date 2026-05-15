package beta.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import beta.manager.ui.component.PluginCard
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.PluginViewModel

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Plugins", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!uiState.isInstalling) onInstallPlugin()
                },
                containerColor = NeonCyan,
                contentColor = DarkBackground
            ) {
                if (uiState.isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Installing...")
                } else {
                    Text("Install ZIP")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = NeonRed)
                }
            } else if (uiState.plugins.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No plugins installed", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap Install ZIP to add a performance plugin", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
