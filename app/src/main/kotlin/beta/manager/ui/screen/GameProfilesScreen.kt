package beta.manager.ui.screen

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Apply performance profiles for your games",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Scanning games...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }
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
                item { ProfileSummaryCard(installedCount, activeCount) }
            }
        }
    }
}

@Composable
private fun GameProfileCard(
    name: String,
    packageName: String,
    isActive: Boolean,
    isInstalled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) DarkCard else DarkSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 2.dp else 0.dp)
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isActive)
                            Brush.linearGradient(listOf(NeonGreen, NeonGreen.copy(alpha = 0.4f)))
                        else
                            Brush.linearGradient(listOf(DarkSurface, DarkSurface))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.PlayCircle else Icons.Filled.SportsEsports,
                    contentDescription = null,
                    tint = if (isActive) Color.White else TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) TextPrimary else TextSecondary
                    )
                    if (!isInstalled) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Not installed",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonRed.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInstalled) TextTertiary else NeonRed.copy(alpha = 0.5f)
                )
            }

            Switch(
                checked = isActive,
                onCheckedChange = { if (isInstalled) onToggle() },
                enabled = isInstalled,
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

@Composable
private fun ProfileSummaryCard(installed: Int, active: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$installed",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    "Installed",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$active",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Text(
                    "Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
            }
        }
    }
}
