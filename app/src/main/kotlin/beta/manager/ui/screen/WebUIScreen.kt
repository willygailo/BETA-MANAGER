package beta.manager.ui.screen

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import beta.manager.IBetaService
import beta.manager.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebUIScreen(
    pluginId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var service by remember { mutableStateOf<IBetaService?>(null) }
    var status by remember { mutableStateOf("Loading...") }

    DisposableEffect(pluginId) {
        val intent = Intent(context, Class.forName("beta.manager.service.BetaService"))
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IBetaService.Stub.asInterface(binder)
                status = "Connected"
            }
            override fun onServiceDisconnected(name: ComponentName?) { service = null }
        }
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        onDispose {
            try {
                context.unbindService(conn)
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("WebUI: $pluginId", fontWeight = FontWeight.Bold)
                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val webRoot = "/data/user_de/0/com.android.shell/beta/plugins/$pluginId/webroot/index.html"

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        addJavascriptInterface(KsuBridge({ service }, ctx), "ksu")
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                status = "Ready"
                            }
                        }
                        loadUrl("file://$webRoot")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (status == "Loading...") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = NeonCyan
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading WebUI...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (status == "Connected") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Connected to service",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen
                    )
                }
            }
        }
    }
}

private class KsuBridge(
    private val serviceProvider: () -> IBetaService?,
    private val context: Context
) {
    @JavascriptInterface
    fun exec(command: String): String {
        return try {
            serviceProvider()?.executeCommand(command) ?: "ERROR: Service not connected"
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    @JavascriptInterface
    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun fullScreen(enabled: Boolean) {
    }
}
