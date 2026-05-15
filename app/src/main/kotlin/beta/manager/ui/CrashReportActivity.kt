package beta.manager.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import beta.manager.ui.theme.*

class CrashReportActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CRASH_INFO = "crash_info"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashInfo = intent?.getStringExtra(EXTRA_CRASH_INFO) ?: "Unknown error"
        setContent {
            BetaManagerTheme {
                CrashScreen(
                    crashInfo = crashInfo,
                    onRestart = { restartApp() }
                )
            }
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
        Runtime.getRuntime().exit(0)
    }
}

@Composable
private fun CrashScreen(crashInfo: String, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚡", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Beta Manager Crashed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NeonRed
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Sorry for the interruption",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Crash Details",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(8.dp))
                val scrollState = rememberScrollState()
                Text(
                    crashInfo,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(scrollState)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text("Restart App", fontWeight = FontWeight.Bold, color = DarkBackground)
        }
    }
}
