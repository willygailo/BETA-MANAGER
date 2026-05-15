package beta.manager.ui.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import beta.manager.IBetaService
import beta.manager.ui.theme.*

data class CommandEntry(val command: String, val output: String, val isError: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellExecutorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf<CommandEntry>()) }
    var history by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var service by remember { mutableStateOf<IBetaService?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val intent = Intent(context, Class.forName("beta.manager.service.BetaService"))
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IBetaService.Stub.asInterface(binder)
            }
            override fun onServiceDisconnected(name: ComponentName?) { service = null }
        }
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    fun execute(cmd: String) {
        if (cmd.isBlank()) return
        val s = service
        entries = entries + CommandEntry("$ $cmd", "")
        history = listOf(cmd) + history.take(99)
        historyIndex = -1
        input = ""

        entries = if (s != null) {
            val result = s.executeCommand(cmd)
            entries + CommandEntry("", result, result.startsWith("ERROR:"))
        } else {
            entries + CommandEntry("", "ERROR: Service not connected", true)
        }
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(entries) { entry ->
                        if (entry.command.isNotBlank()) {
                            Text(
                                entry.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonGreen
                            )
                        }
                        if (entry.output.isNotBlank()) {
                            Text(
                                entry.output,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (entry.isError) NeonRed else TextPrimary,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Type command...",
                            color = TextTertiary,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CardBorder,
                        cursorColor = NeonGreen,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { execute(input.trim()) }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = { execute(input.trim()) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonGreen.copy(alpha = 0.2f), NeonGreen.copy(alpha = 0.05f)))
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute",
                        tint = NeonGreen
                    )
                }
            }
        }
    }
}
