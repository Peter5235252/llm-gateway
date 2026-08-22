package com.example.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.util.SoundSynth
import com.example.ui.components.MarkdownText

// Dynamic local composition for complete visual theme state
data class GatewayColors(
    val isDarkMode: Boolean,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val frostedGlass: Color,
    val frostedBorder: Color,
    val userBubble: Color,
    val userBubbleText: Color,
    val otherBubble: Color,
    val otherBubbleText: Color,
    val systemAmber: Color,
    val accent: Color = Color(0xFF29B6F6)
)

val LocalGatewayColors = staticCompositionLocalOf<GatewayColors> {
    error("No GatewayColors provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayScreen() {
    val viewModel: GatewayViewModel = viewModel()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isBrowsingWeb by viewModel.isBrowsingWeb.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedAttachment by viewModel.selectedAttachment.collectAsState()
    val pendingIntentUri by viewModel.pendingIntentUri.collectAsState()
    val geminiKeyEntered by viewModel.geminiKey.collectAsState()
    val anthropicKey by viewModel.anthropicKey.collectAsState()
    val openAiKey by viewModel.openAiKey.collectAsState()
    val customKey by viewModel.customKey.collectAsState()
    val forceGrounding by viewModel.forceGroundingNext.collectAsState()
    val context = LocalContext.current
    var showProWarningDialog by remember { mutableStateOf(false) }
    var pendingModelSwitch by remember { mutableStateOf<LlmModel?>(null) }

    val colorScheme = remember(isDarkMode) {
        if (isDarkMode) {
            darkColorScheme(
                primary = Color.White,
                background = Color.Black,
                surface = Color.Black,
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color.Black,
                background = Color(0xFFF7F8FA),
                surface = Color.White,
                onPrimary = Color.White,
                onBackground = Color(0xFF1C1D1F),
                onSurface = Color(0xFF1C1D1F)
            )
        }
    }

    val gatewayColors = remember(isDarkMode) {
        GatewayColors(
            isDarkMode = isDarkMode,
            background = if (isDarkMode) Color(0xFF000000) else Color(0xFFF7F8FA),
            onBackground = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF1C1D1F),
            surface = if (isDarkMode) Color(0xFF000000) else Color(0xFFFFFFFF),
            onSurface = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF1C1D1F),
            frostedGlass = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0E000000),
            frostedBorder = if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000),
            userBubble = if (isDarkMode) Color(0xFF0B5D3E) else Color(0xFFE2F9EC),
            userBubbleText = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF0B3A25),
            otherBubble = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFFFFFFF),
            otherBubbleText = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF1C1D1F),
            systemAmber = Color(0xFFE4A115)
        )
    }

    LaunchedEffect(pendingIntentUri) {
        pendingIntentUri?.let { uriStr ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.clearPendingIntent()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalGatewayColors provides gatewayColors) {
            val colors = LocalGatewayColors.current
            
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = colors.surface,
                        modifier = Modifier.border(1.dp, colors.frostedBorder)
                    ) {
                        SettingsDrawerContent(viewModel)
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        var modelDropdownExpanded by remember { mutableStateOf(false) }
                        var anthropicSubMenuExpanded by remember { mutableStateOf(false) }
                        var openAiSubMenuExpanded by remember { mutableStateOf(false) }
                        var geminiSubMenuExpanded by remember { mutableStateOf(false) }

                        val modelLabel = selectedModel.displayName

                        val handleModelSelect: (LlmModel) -> Unit = { targetModel ->
                            modelDropdownExpanded = false
                            if (targetModel != selectedModel && messages.isNotEmpty()) {
                                pendingModelSwitch = targetModel
                            } else {
                                if (targetModel == LlmModel.GEMINI_PRO) {
                                    showProWarningDialog = true
                                }
                                viewModel.selectModel(targetModel)
                                if (targetModel == LlmModel.CUSTOM_MODEL && customKey.isEmpty()) {
                                    scope.launch { drawerState.open() }
                                }
                            }
                        }

                        LaunchedEffect(modelDropdownExpanded) {
                            if (modelDropdownExpanded) {
                                anthropicSubMenuExpanded = selectedModel.provider == LlmProvider.ANTHROPIC
                                openAiSubMenuExpanded = selectedModel.provider == LlmProvider.OPENAI
                                geminiSubMenuExpanded = selectedModel.provider == LlmProvider.GEMINI_FLASH || selectedModel.provider == LlmProvider.GEMINI_PRO
                            }
                        }

                        TopAppBar(
                            title = {
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                SoundSynth.playTap()
                                                modelDropdownExpanded = !modelDropdownExpanded
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = modelLabel,
                                                    color = colors.onBackground,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (modelDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Select Model",
                                                    tint = colors.onBackground.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            
                                            if (selectedModel.provider == LlmProvider.GEMINI_FLASH || selectedModel.provider == LlmProvider.GEMINI_PRO) {
                                                val usingFallback = geminiKeyEntered.isEmpty()
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(
                                                                if (usingFallback) colors.systemAmber else Color.Green,
                                                                CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (usingFallback) "Baseline Active" else "Personal Key Active",
                                                        color = colors.onBackground.copy(alpha = 0.5f),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            } else {
                                                val keyEntered = when (selectedModel.provider) {
                                                    LlmProvider.ANTHROPIC -> anthropicKey.isNotEmpty()
                                                    LlmProvider.OPENAI -> openAiKey.isNotEmpty()
                                                    LlmProvider.CUSTOM -> customKey.isNotEmpty()
                                                    else -> false
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(
                                                                if (keyEntered) Color.Green else Color.Red,
                                                                CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (keyEntered) "Key Active" else "No Key Configured",
                                                        color = colors.onBackground.copy(alpha = 0.5f),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = modelDropdownExpanded,
                                        onDismissRequest = { modelDropdownExpanded = false },
                                        modifier = Modifier
                                            .width(260.dp)
                                            .background(colors.surface)
                                            .border(1.dp, colors.frostedBorder, RoundedCornerShape(12.dp))
                                    ) {
                                        // Anthropic Section
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Anthropic", color = colors.onBackground, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Paid Key",
                                                                color = colors.systemAmber,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .background(colors.systemAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = "Requires paid API key",
                                                            color = colors.onBackground.copy(alpha = 0.4f),
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (anthropicSubMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = colors.onBackground.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                SoundSynth.playTap()
                                                anthropicSubMenuExpanded = !anthropicSubMenuExpanded
                                            }
                                        )

                                        if (anthropicSubMenuExpanded) {
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Claude Sonnet 5", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.CLAUDE_SONNET_5)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Claude Fable 5", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.CLAUDE_FABLE_5)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Claude Opus 5", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.CLAUDE_OPUS_5)
                                                }
                                            )
                                        }

                                        HorizontalDivider(color = colors.frostedBorder, modifier = Modifier.padding(vertical = 4.dp))

                                        // OpenAI Section
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("OpenAI", color = colors.onBackground, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Paid Key",
                                                                color = colors.systemAmber,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .background(colors.systemAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = "Requires paid API key",
                                                            color = colors.onBackground.copy(alpha = 0.4f),
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (openAiSubMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = colors.onBackground.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                SoundSynth.playTap()
                                                openAiSubMenuExpanded = !openAiSubMenuExpanded
                                            }
                                        )

                                        if (openAiSubMenuExpanded) {
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  GPT-5.6 Luna", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.GPT_5_6_LUNA)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  GPT-5.6 Terra", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.GPT_5_6_TERRA)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  GPT-5.6 Sol", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.GPT_5_6_SOL)
                                                }
                                            )
                                        }

                                        HorizontalDivider(color = colors.frostedBorder, modifier = Modifier.padding(vertical = 4.dp))

                                        // Gemini Section
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Gemini", color = colors.onBackground, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Free / Baseline",
                                                                color = Color.Green,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .background(Color.Green.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = "Key-free fallback built-in",
                                                            color = colors.onBackground.copy(alpha = 0.4f),
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (geminiSubMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = colors.onBackground.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                SoundSynth.playTap()
                                                geminiSubMenuExpanded = !geminiSubMenuExpanded
                                            }
                                        )

                                        if (geminiSubMenuExpanded) {
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Gemini 3.7 Flash", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.GEMINI_FLASH)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Gemini 3.1 Pro", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.GEMINI_PRO)
                                                }
                                            )
                                        }

                                        HorizontalDivider(color = colors.frostedBorder, modifier = Modifier.padding(vertical = 4.dp))

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Custom", color = colors.onBackground, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text("OpenAI-compatible endpoints", color = colors.onBackground.copy(alpha = 0.4f), fontSize = 9.sp)
                                                }
                                            },
                                            onClick = {
                                                SoundSynth.playTap()
                                                handleModelSelect(LlmModel.CUSTOM_MODEL)
                                            }
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    SoundSynth.playTap()
                                    scope.launch { drawerState.open() } 
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Settings", tint = colors.onBackground)
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    SoundSynth.playTap()
                                    viewModel.toggleDarkMode()
                                }) {
                                    Text(
                                        text = if (isDarkMode) "☀️" else "🌙",
                                        fontSize = 20.sp
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colors.background,
                                titleContentColor = colors.onBackground,
                                navigationIconContentColor = colors.onBackground
                            )
                        )
                    },
                    containerColor = colors.background
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                        if (showProWarningDialog) {
                            AlertDialog(
                                onDismissRequest = { 
                                    showProWarningDialog = false 
                                },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Warning",
                                            tint = colors.systemAmber,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Gemini 3.1 Pro Notice",
                                            color = colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = "Gemini 3.7 Flash is recommended:",
                                            color = colors.onSurface.copy(alpha = 0.95f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Text(
                                            text = "• Performance: Gemini 3.7 Flash outperforms Gemini 3.1 Pro in most metrics (including coding benchmarks, SWE benchmarks, and agentic workflows).\n" +
                                                    "• Speed & Latency: Gemini 3.7 Flash is significantly faster with lower response latency (generating up to ~381 tokens/sec).\n" +
                                                    "• Cost & Efficiency: Gemini 3.7 Flash is substantially cheaper per token and provides higher token throughput quotas.",
                                            color = colors.onSurface.copy(alpha = 0.85f),
                                            fontSize = 13.sp,
                                            lineHeight = 18.5.sp
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            SoundSynth.playTap()
                                            viewModel.selectModel(LlmModel.GEMINI_FLASH)
                                            showProWarningDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                             containerColor = colors.userBubble,
                                             contentColor = colors.userBubbleText
                                        )
                                    ) {
                                        Text("Use 3.7 Flash instead (Recommended)", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            SoundSynth.playTap()
                                            showProWarningDialog = false
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = colors.onSurface.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Text("Continue with 3.1 Pro")
                                    }
                                },
                                containerColor = colors.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.border(1.dp, colors.frostedBorder, RoundedCornerShape(16.dp))
                            )
                        }

                        if (pendingModelSwitch != null) {
                            AlertDialog(
                                onDismissRequest = { 
                                    pendingModelSwitch = null 
                                },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Warning",
                                            tint = colors.systemAmber,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Switch Model Mid-Conversation?",
                                            color = colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = "Switching models mid-conversation can introduce intelligence degradation, context inconsistency, and formatting shifts. It is recommended to stick to the current model for the remainder of this session.",
                                            color = colors.onSurface.copy(alpha = 0.85f),
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            SoundSynth.playTap()
                                            pendingModelSwitch = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.userBubble,
                                            contentColor = colors.userBubbleText
                                        )
                                    ) {
                                        Text(
                                            text = "Continue chatting with ${selectedModel.displayName} (Recommended)",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            SoundSynth.playTap()
                                            val target = pendingModelSwitch
                                            pendingModelSwitch = null
                                            if (target != null) {
                                                if (target == LlmModel.GEMINI_PRO) {
                                                    showProWarningDialog = true
                                                }
                                                viewModel.selectModel(target)
                                                if (target == LlmModel.CUSTOM_MODEL && customKey.isEmpty()) {
                                                    scope.launch { drawerState.open() }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = colors.onSurface.copy(alpha = 0.7f)
                                        )
                                    ) {
                                        Text(
                                            text = "Switch anyway",
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                containerColor = colors.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.border(1.dp, colors.frostedBorder, RoundedCornerShape(16.dp))
                            )
                        }

                        LaunchedEffect(error) {
                            if (error != null) {
                                kotlinx.coroutines.delay(6000)
                                viewModel.clearError()
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = error != null,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Surface(
                                color = Color(0x22FF3B30),
                                border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error ?: "",
                                        color = if (isDarkMode) Color(0xFFFF6B6B) else Color(0xFFD32F2F),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = colors.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Main Chat List or Empty State - polished with quick actions
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = colors.onBackground.copy(alpha = 0.12f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Start a conversation",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.onBackground.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Secure local key-guarded pipeline.\nPick a provider above or try a suggestion:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onBackground.copy(alpha = 0.45f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    // Suggestion chips - UX polish
                                    val suggestions = listOf(
                                        "Explain this code" to "Explain this code snippet and suggest improvements",
                                        "Summarize file" to "Use /read <filename> to summarize a file from my working directory",
                                        "Draft email" to "Draft a concise professional email about...",
                                        "Search the web" to "Search the web for the latest news on Gemini 3.7 Flash"
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        suggestions.chunked(2).forEach { row ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                row.forEach { (label, prompt) ->
                                                    Surface(
                                                        shape = RoundedCornerShape(20.dp),
                                                        color = colors.frostedGlass,
                                                        border = BorderStroke(1.dp, colors.frostedBorder),
                                                        modifier = Modifier.clickable {
                                                            SoundSynth.playTap()
                                                            viewModel.sendMessage(prompt)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                            color = colors.onBackground.copy(alpha = 0.85f),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Tip: Attach files with 📎 or use /export to save chat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onBackground.copy(alpha = 0.3f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    val isSpeaking = speakingMessageId == msg.id
                                    MessageBubble(
                                        message = msg,
                                        isSpeaking = isSpeaking,
                                        onSpeak = { viewModel.speakText(msg.id, msg.content) },
                                        onRegenerate = { viewModel.regenerateResponse(msg.id) }
                                    )
                                }
                                if (isLoading) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Surface(
                                                color = if (isBrowsingWeb) Color(0xFF1A3A5F).copy(alpha = if (isDarkMode) 0.6f else 0.15f) else colors.otherBubble,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, if (isBrowsingWeb) Color(0xFF4285F4).copy(alpha = 0.5f) else colors.frostedBorder),
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = if (isBrowsingWeb) Color(0xFF4285F4) else colors.onBackground,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    if (isBrowsingWeb) {
                                                        Icon(
                                                            imageVector = Icons.Default.Language,
                                                            contentDescription = "Browsing",
                                                            tint = Color(0xFF4285F4),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Browsing the web...",
                                                            color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1967D2),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "Generating response...",
                                                            color = colors.onBackground.copy(alpha = 0.7f),
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Rate limits Warning & Attachment Previews Card
                        if (selectedAttachment != null) {
                            Surface(
                                color = Color(0x15FFB300),
                                border = BorderStroke(1.dp, colors.systemAmber.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (selectedAttachment?.mimeType?.startsWith("image/") == true) Icons.Default.AttachFile else Icons.Default.Description,
                                                contentDescription = "Attachment Selected",
                                                tint = colors.systemAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = selectedAttachment?.name ?: "File Selected",
                                                color = colors.onBackground,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 200.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                SoundSynth.playTap()
                                                viewModel.clearAttachment()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear Attachment", tint = colors.onBackground, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠️ Large file & photo attachments consume API rate limits significantly faster.",
                                        color = colors.systemAmber,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        MessageInput(
                            selectedAttachment = selectedAttachment,
                            selectedModel = selectedModel,
                            isForceGrounding = forceGrounding,
                            onToggleForceGrounding = { viewModel.toggleForceGrounding() },
                            onFileSelected = { uri, mimeType, name ->
                                viewModel.processSelectedUri(uri, mimeType, name)
                            },
                            onSend = { viewModel.sendMessage(it) }
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun MessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean = false,
    onSpeak: () -> Unit = {},
    onRegenerate: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val isSystem = message.role == "system"
    val colors = LocalGatewayColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else if (isSystem) Arrangement.Center else Arrangement.Start
    ) {
        if (isSystem) {
            Surface(
                color = colors.frostedGlass.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, colors.frostedBorder.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = colors.onBackground.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        } else {
            // WhatsApp speech-corners
            val bubbleShape = remember(isUser) {
                RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                )
            }

            Column(modifier = Modifier.widthIn(max = 310.dp)) {
                Surface(
                    color = if (isUser) colors.userBubble else colors.otherBubble,
                    contentColor = if (isUser) colors.userBubbleText else colors.otherBubbleText,
                    shape = bubbleShape,
                    border = if (!isUser) BorderStroke(1.dp, colors.frostedBorder) else null,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        message.attachment?.let { attachment ->
                        val isImage = attachment.mimeType.startsWith("image/")
                        if (isImage && attachment.base64Data != null) {
                            val bitmap = remember(attachment.base64Data) {
                                try {
                                    val bytes = android.util.Base64.decode(attachment.base64Data, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            bitmap?.let {
                                androidx.compose.foundation.Image(
                                    bitmap = it,
                                    contentDescription = "Message Image Attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp)
                                        .padding(bottom = 6.dp)
                                        .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, colors.frostedBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            SoundSynth.playTap()
                                        }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Document Attachment", tint = if (isUser) colors.userBubbleText else colors.otherBubbleText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = attachment.name,
                                        color = if (isUser) colors.userBubbleText else colors.otherBubbleText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = attachment.mimeType,
                                        color = (if (isUser) colors.userBubbleText else colors.otherBubbleText).copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    if (message.content.isNotEmpty()) {
                        MarkdownText(
                            markdown = message.content,
                            color = if (isUser) colors.userBubbleText else colors.otherBubbleText,
                            isUser = isUser
                        )
                    }

                    if (message.groundingSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = colors.frostedBorder.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Sources",
                                tint = if (colors.isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1967D2),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sources & Search Results",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1967D2)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val uriHandler = LocalUriHandler.current
                            message.groundingSources.take(5).forEach { source ->
                                Surface(
                                    color = if (colors.isDarkMode) Color(0x1FFFFFFF) else Color(0x0A000000),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, colors.frostedBorder.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            try {
                                                uriHandler.openUri(source.url)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔗 " + source.title,
                                            fontSize = 11.sp,
                                            color = if (colors.isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1967D2),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.frostedGlass.copy(alpha = 0.05f),
                            border = BorderStroke(0.5.dp, colors.frostedBorder.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onSpeak() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop Speaking" else "Read Aloud",
                                    tint = colors.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeaking) "Stop" else "Read Aloud",
                                    fontSize = 11.sp,
                                    color = colors.onBackground.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.frostedGlass.copy(alpha = 0.05f),
                            border = BorderStroke(0.5.dp, colors.frostedBorder.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onRegenerate() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = colors.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Regenerate",
                                    fontSize = 11.sp,
                                    color = colors.onBackground.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    selectedAttachment: Attachment?,
    selectedModel: LlmModel,
    isForceGrounding: Boolean,
    onToggleForceGrounding: () -> Unit,
    onFileSelected: (Uri, String?, String) -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var showPlusMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = LocalGatewayColors.current
    val isGemini = selectedModel.provider == LlmProvider.GEMINI_FLASH || selectedModel.provider == LlmProvider.GEMINI_PRO

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                text += if (text.isEmpty()) matches[0] else " " + matches[0]
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            var name = "attachment"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
            onFileSelected(uri, mimeType, name)
        }
    }

    val canSend = text.isNotBlank() || selectedAttachment != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(colors.frostedGlass, RoundedCornerShape(24.dp))
            .border(1.dp, colors.frostedBorder, RoundedCornerShape(24.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // + button far left - overflow for tools to reduce bloat
        Box {
            IconButton(onClick = {
                SoundSynth.playTap()
                showPlusMenu = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "More tools", tint = colors.onBackground.copy(alpha = 0.85f))
            }
            DropdownMenu(
                expanded = showPlusMenu,
                onDismissRequest = { showPlusMenu = false },
                modifier = Modifier.background(colors.surface).border(1.dp, colors.frostedBorder, RoundedCornerShape(12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Attach file / image", color = colors.onBackground, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        showPlusMenu = false
                        SoundSynth.playTap()
                        filePickerLauncher.launch("*/*")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Voice input", color = colors.onBackground, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        showPlusMenu = false
                        SoundSynth.playTap()
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            speechLauncher.launch(intent)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                )
                HorizontalDivider(color = colors.frostedBorder, thickness = 0.5.dp)
                DropdownMenuItem(
                    text = { Column { Text("/read <file>", color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("Read from working dir", color = colors.onBackground.copy(alpha = 0.5f), fontSize = 10.sp) } },
                    onClick = {
                        showPlusMenu = false
                        text = "/read "
                    }
                )
                DropdownMenuItem(
                    text = { Column { Text("/export <file>", color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("Save chat to file", color = colors.onBackground.copy(alpha = 0.5f), fontSize = 10.sp) } },
                    onClick = {
                        showPlusMenu = false
                        text = "/export "
                    }
                )
            }
        }

        // Globe icon - force web grounding for next Gemini response (both 3.1 Pro & 3.7 Flash)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isForceGrounding) Color(0xFF4285F4).copy(alpha = 0.15f) else Color.Transparent)
                .border(1.dp, if (isForceGrounding) Color(0xFF4285F4).copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(16.dp))
        ) {
            IconButton(onClick = {
                SoundSynth.playTap()
                if (!isGemini) {
                    android.widget.Toast.makeText(context, "Web grounding only for Gemini 3.7 Flash / 3.1 Pro", android.widget.Toast.LENGTH_SHORT).show()
                }
                onToggleForceGrounding()
            }) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = if (isForceGrounding) "Grounding forced for next response" else "Force web search (Gemini)",
                    tint = if (isForceGrounding) Color(0xFF4285F4) else if (isGemini) colors.onBackground.copy(alpha = 0.85f) else colors.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                cursorColor = colors.onBackground
            ),
            placeholder = { Text(if (isForceGrounding && isGemini) "Message • 🌐 grounding next" else "Message • /read & /export", color = colors.onBackground.copy(alpha = 0.4f), fontSize = 13.sp) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                if (canSend) {
                    SoundSynth.playTap()
                    onSend(text)
                    text = ""
                }
            }),
            maxLines = 5
        )

        IconButton(
            onClick = {
                if (!canSend) return@IconButton
                SoundSynth.playTap()
                onSend(text)
                text = ""
            },
            enabled = true
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "Send",
                tint = if (canSend) colors.onBackground else colors.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawerContent(viewModel: GatewayViewModel) {
    val context = LocalContext.current
    val colors = LocalGatewayColors.current
    val anthropicKey by viewModel.anthropicKey.collectAsState()
    val openAiKey by viewModel.openAiKey.collectAsState()
    val geminiKey by viewModel.geminiKey.collectAsState()
    val customKey by viewModel.customKey.collectAsState()
    val customBaseUrl by viewModel.customBaseUrl.collectAsState()
    val customModelId by viewModel.customModelId.collectAsState()
    val workingDirUri by viewModel.workingDirUri.collectAsState()

    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.updateWorkingDirUri(uri.toString())
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
        Text("Gateway Settings", style = MaterialTheme.typography.titleLarge, color = colors.onBackground, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        var showPasswords by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = showPasswords, 
                onCheckedChange = { 
                    SoundSynth.playTap()
                    showPasswords = it 
                }, 
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.onBackground,
                    checkmarkColor = colors.background
                )
            )
            Text("Show API Keys", color = colors.onBackground)
        }

        val visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()

        OutlinedTextField(
            value = anthropicKey,
            onValueChange = { viewModel.updateAnthropicKey(it) },
            label = { Text("Anthropic API Key", color = colors.onBackground.copy(alpha = 0.7f)) },
            placeholder = { Text("Or set via AI Studio Secrets panel", color = colors.onBackground.copy(alpha = 0.4f), maxLines = 1, fontSize = 12.sp) },
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.onBackground,
                unfocusedBorderColor = colors.frostedBorder,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedLabelColor = colors.onBackground.copy(alpha = 0.8f),
                unfocusedLabelColor = colors.onBackground.copy(alpha = 0.6f)
            )
        )
        OutlinedTextField(
            value = openAiKey,
            onValueChange = { viewModel.updateOpenAiKey(it) },
            label = { Text("OpenAI API Key", color = colors.onBackground.copy(alpha = 0.7f)) },
            placeholder = { Text("Or set via AI Studio Secrets panel", color = colors.onBackground.copy(alpha = 0.4f), maxLines = 1, fontSize = 12.sp) },
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.onBackground,
                unfocusedBorderColor = colors.frostedBorder,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedLabelColor = colors.onBackground.copy(alpha = 0.8f),
                unfocusedLabelColor = colors.onBackground.copy(alpha = 0.6f)
            )
        )
        OutlinedTextField(
            value = geminiKey,
            onValueChange = { viewModel.updateGeminiKey(it) },
            label = { Text("Gemini API Key (Optional)", color = colors.onBackground.copy(alpha = 0.7f)) },
            placeholder = { Text("Using baseline Google AI Studio key", color = colors.onBackground.copy(alpha = 0.4f), maxLines = 1, fontSize = 12.sp) },
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.onBackground,
                unfocusedBorderColor = colors.frostedBorder,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedLabelColor = colors.onBackground.copy(alpha = 0.8f),
                unfocusedLabelColor = colors.onBackground.copy(alpha = 0.6f)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Custom Endpoint Settings", style = MaterialTheme.typography.titleMedium, color = colors.onBackground, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customKey,
            onValueChange = { viewModel.updateCustomKey(it) },
            label = { Text("Custom API Key", color = colors.onBackground.copy(alpha = 0.7f)) },
            placeholder = { Text("Or set via AI Studio Secrets panel", color = colors.onBackground.copy(alpha = 0.4f), maxLines = 1, fontSize = 12.sp) },
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.onBackground,
                unfocusedBorderColor = colors.frostedBorder,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedLabelColor = colors.onBackground.copy(alpha = 0.8f),
                unfocusedLabelColor = colors.onBackground.copy(alpha = 0.6f)
            )
        )
        OutlinedTextField(
            value = customBaseUrl,
            onValueChange = { viewModel.updateCustomBaseUrl(it) },
            label = { Text("Custom Base URL", color = colors.onBackground.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.onBackground,
                unfocusedBorderColor = colors.frostedBorder,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedLabelColor = colors.onBackground.copy(alpha = 0.8f),
                unfocusedLabelColor = colors.onBackground.copy(alpha = 0.6f)
            )
        )
        OutlinedTextField(
            value = customModelId,
            onValueChange = { viewModel.updateCustomModelId(it) },
            label = { Text("Custom Model ID", color = colors.onBackground.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.onBackground,
                unfocusedBorderColor = colors.frostedBorder,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedLabelColor = colors.onBackground.copy(alpha = 0.8f),
                unfocusedLabelColor = colors.onBackground.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Filesystem Access", style = MaterialTheme.typography.titleMedium, color = colors.onBackground, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { 
                SoundSynth.playTap()
                dirPicker.launch(null) 
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.frostedGlass, contentColor = colors.onBackground),
            border = BorderStroke(1.dp, colors.frostedBorder)
        ) {
            Text(if (workingDirUri.isEmpty()) "Select Working Directory" else "✓ Directory Selected")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Uses Storage Access Framework - no broad file permission needed. Re-select after reboot if needed.",
            color = colors.onBackground.copy(alpha = 0.4f),
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    }
}
