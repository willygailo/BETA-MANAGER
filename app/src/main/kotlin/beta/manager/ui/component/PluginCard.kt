package beta.manager.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plugin.isEnabled) DarkCard else DarkCard.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (plugin.isEnabled) TextPrimary else TextTertiary
                    )
                    Spacer(Modifier.height(2.dp))
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
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            if (plugin.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(plugin.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plugin.hasAction) {
                    FilledTonalButton(
                        onClick = onRunAction,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = NeonPurple.copy(alpha = 0.2f))
                    ) {
                        Text("Action", color = NeonPurple)
                    }
                }
                if (plugin.hasWebUI && onOpenWebUI != null) {
                    FilledTonalButton(
                        onClick = onOpenWebUI,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = NeonCyan.copy(alpha = 0.2f))
                    ) {
                        Text("WebUI", color = NeonCyan)
                    }
                }
                OutlinedButton(
                    onClick = { showRemoveDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
                ) {
                    Text("Remove")
                }
            }

            if (showActionOutput != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    showActionOutput,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showActionOutput.startsWith("Error") || showActionOutput.startsWith("Failed")) NeonRed else NeonGreen
                )
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Plugin") },
            text = { Text("Remove \"${plugin.name}\"? It will be deleted on next reboot.") },
            confirmButton = {
                TextButton(onClick = { onRemove(); showRemoveDialog = false }) {
                    Text("Remove", color = NeonRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
