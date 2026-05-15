package beta.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import beta.manager.plugin.PluginInfo
import beta.manager.plugin.PluginSource
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
    var showDetail by remember { mutableStateOf(false) }

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
        elevation = CardDefaults.cardElevation(defaultElevation = if (plugin.isEnabled) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showDetail = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        imageVector = Icons.Filled.Extension,
                        contentDescription = null,
                        tint = if (plugin.isEnabled) Color.White else TextTertiary,
                        modifier = Modifier.size(22.dp)
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
                    )
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

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plugin.hasAction) {
                    FilledTonalButton(
                        onClick = onRunAction,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = NeonPurple.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Action", color = NeonPurple, fontSize = 12.sp)
                    }
                }
                if (plugin.hasWebUI && onOpenWebUI != null) {
                    FilledTonalButton(
                        onClick = onOpenWebUI,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = NeonCyan.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WebUI", color = NeonCyan, fontSize = 12.sp)
                    }
                }
                FilledTonalButton(
                    onClick = { showDetail = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = NeonCyan.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                    Spacer(Modifier.width(6.dp))
                    Text("Info", color = NeonCyan, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { showRemoveDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remove", fontSize = 12.sp)
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
                            Icons.Filled.Info, contentDescription = null,
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
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(28.dp)) },
            title = { Text("Remove Module", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Remove \"${plugin.name}\"? It will be deleted on next reboot.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { onRemove(); showRemoveDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f), contentColor = NeonRed),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDialog = false }, shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            },
            containerColor = DarkSurface, titleContentColor = TextPrimary, textContentColor = TextSecondary,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            icon = { Icon(Icons.Filled.Extension, contentDescription = null, tint = sourceColor, modifier = Modifier.size(32.dp)) },
            title = { Text(plugin.name, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow("ID", plugin.id)
                    DetailRow("Version", "${plugin.version} (code ${plugin.versionCode})")
                    DetailRow("Author", plugin.author.ifEmpty { "Unknown" })
                    DetailRow("Source", sourceLabel)
                    DetailRow("Status", if (plugin.isEnabled) "Enabled" else "Disabled")
                    DetailRow("Action Script", if (plugin.hasAction) "Yes" else "No")
                    DetailRow("WebUI", if (plugin.hasWebUI) "Available" else "N/A")
                    if (plugin.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Description", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Text(plugin.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetail = false },
                    colors = ButtonDefaults.buttonColors(containerColor = sourceColor, contentColor = DarkBackground),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("OK") }
            },
            containerColor = DarkSurface, titleContentColor = TextPrimary, textContentColor = TextSecondary,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}
