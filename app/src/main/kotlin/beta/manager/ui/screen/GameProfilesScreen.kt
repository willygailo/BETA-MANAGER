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
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.GameProfilesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameProfilesScreen(
    viewModel: GameProfilesViewModel,
    onNavigateBack: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Profiles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Apply performance profiles per game", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            } else {
                items(profiles) { profile ->
                    GameProfileCard(
                        name = profile.name,
                        packageName = profile.packageName,
                        isActive = profile.isActive,
                        isInstalled = profile.isInstalled,
                        onToggle = { viewModel.toggleProfile(profile.id) }
                    )
                }

                val installedCount = profiles.count { it.isInstalled }
                val activeCount = profiles.count { it.isActive }
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$installedCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Text("Installed", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$activeCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NeonGreen)
                                Text("Active", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameProfileCard(name: String, packageName: String, isActive: Boolean, isInstalled: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) NeonGreen.copy(alpha = 0.08f) else DarkCard)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (!isInstalled) {
                        Spacer(Modifier.width(8.dp))
                        Text("(not installed)", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(packageName, style = MaterialTheme.typography.bodySmall, color = if (isInstalled) TextTertiary else NeonRed.copy(alpha = 0.6f))
            }
            Switch(
                checked = isActive,
                onCheckedChange = { if (isInstalled) onToggle() },
                enabled = isInstalled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                )
            )
        }
    }
}
