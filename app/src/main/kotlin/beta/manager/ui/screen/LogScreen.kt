package beta.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import beta.manager.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onNavigateBack: () -> Unit
) {
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val lines = withContext(Dispatchers.IO) {
            val logDir = File("/data/user_de/0/com.android.shell/beta/logs/")
            if (logDir.exists()) {
                logDir.listFiles()
                    ?.filter { it.name.endsWith(".log") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.flatMap { file ->
                        listOf("=== ${file.name} ===") + (file.readLines().ifEmpty { listOf("(empty)") })
                    } ?: listOf("No log files found")
            } else {
                listOf("Log directory not found.\nService may not be running yet.")
            }
        }
        logs = lines
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Logs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeonCyan)
            } else if (logs.isEmpty()) {
                Text("No logs available", color = TextTertiary, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(logs) { line ->
                        Text(
                            line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (line.startsWith("===")) NeonCyan else if (line.startsWith("ERROR") || line.startsWith("FAIL")) NeonRed else TextSecondary,
                            fontWeight = if (line.startsWith("===")) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
