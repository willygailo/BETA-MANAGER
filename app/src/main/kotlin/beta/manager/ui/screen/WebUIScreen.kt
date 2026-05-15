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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(pluginId) {
        val intent = Intent(context, Class.forName("beta.manager.service.BetaService"))
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IBetaService.Stub.asInterface(binder)
                status = "Connected"
            }
            override fun onServiceDisconnected(name: ComponentName?) { service = null }
        }
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebUI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            val webRoot = "/data/user_de/0/com.android.shell/beta/plugins/$pluginId/webroot/index.html"

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        addJavascriptInterface(KsuBridge(service, ctx), "ksu")
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
                CircularProgressIndicator(color = NeonCyan)
            }
        }
    }
}

private class KsuBridge(
    private val service: IBetaService?,
    private val context: Context
) {
    @JavascriptInterface
    fun exec(command: String): String {
        return try {
            service?.executeCommand(command) ?: "ERROR: Service not connected"
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
