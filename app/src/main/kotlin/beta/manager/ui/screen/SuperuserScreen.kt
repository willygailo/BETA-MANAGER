package beta.manager.ui.screen

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import beta.manager.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SuAppInfo(
    val packageName: String,
    val appName: String,
    val hasRootAccess: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun SuperuserScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(listOf<SuAppInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRooted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val (rooted, appList) = withContext(Dispatchers.IO) {
            val suApps = mutableListOf<SuAppInfo>()
            val hasSu = try {
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
                proc.outputStream.close()
                val line = proc.inputStream.bufferedReader().readLine()
                proc.waitFor()
                line == "root"
            } catch (_: Exception) { false }

            if (hasSu) {
                val pm = context.packageManager
                try {
                    val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls /data/system/appops/*.xml 2>/dev/null || ls /data/system/packages.list 2>/dev/null"))
                    val output = proc.inputStream.bufferedReader().readText()
                    proc.waitFor()

                    val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getInstalledApplications(0)
                    }
                    appList@ for (app in installedPackages) {
                        val name = pm.getApplicationLabel(app).toString()
                        suApps.add(SuAppInfo(app.packageName, name))
                        if (suApps.size >= 50) break
                    }
                } catch (_: Exception) {
                    suApps.add(SuAppInfo("system", "Root Shell", true))
                }
            }
            Pair(hasSu, suApps)
        }
        isRooted = rooted
        apps = appList
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Superuser", fontWeight = FontWeight.Bold)
                        if (isRooted) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ROOTED", fontSize = 10.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeonCyan)
            } else if (!isRooted) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Device is not rooted", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("Superuser management requires root access", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text(
                            "Installed Apps (${apps.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextTertiary
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(apps) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Android, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = TextTertiary, fontSize = 11.sp)
                                }
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = NeonGreen.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
