package beta.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import beta.manager.plugin.PluginInfo
import beta.manager.ui.theme.*

@Composable
fun PluginCard(
    plugin: PluginInfo,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onRunAction: () -> Unit,
    onOpenWebUI: (() -> Unit)? = null,
    showActionOutput: String? = null
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plugin.isEnabled) DarkCard else DarkSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (plugin.isEnabled) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (plugin.isEnabled)
                                Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                            else
                                Brush.linearGradient(listOf(DarkSurfaceVariant, DarkSurfaceVariant))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Extension,
                        contentDescription = null,
                        tint = if (plugin.isEnabled) Color.White else TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (plugin.isEnabled) TextPrimary else TextTertiary
                    )
                    Text(
                        "${plugin.version} by ${plugin.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }

                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            if (plugin.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    plugin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plugin.hasAction) {
                    FilledTonalButton(
                        onClick = onRunAction,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonPurple.copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Run", color = NeonPurple)
                    }
                }
                if (plugin.hasWebUI && onOpenWebUI != null) {
                    FilledTonalButton(
                        onClick = onOpenWebUI,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonCyan.copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("WebUI", color = NeonCyan)
                    }
                }
                OutlinedButton(
                    onClick = { showRemoveDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Remove")
                }
            }

            if (showActionOutput != null) {
                Spacer(Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (showActionOutput.startsWith("Error") || showActionOutput.startsWith("Failed")) NeonRed else NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            showActionOutput,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (showActionOutput.startsWith("Error") || showActionOutput.startsWith("Failed")) NeonRed else NeonGreen
                        )
                    }
                }
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = NeonRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Remove Plugin",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "Remove \"${plugin.name}\"? It will be deleted on next reboot.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonRed.copy(alpha = 0.2f),
                        contentColor = NeonRed
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRemoveDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
