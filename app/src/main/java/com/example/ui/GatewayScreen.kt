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
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Keyboard
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
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
    val mistralKey by viewModel.mistralKey.collectAsState()
    val customKey by viewModel.customKey.collectAsState()
    val forceGrounding by viewModel.forceGroundingNext.collectAsState()
    val isVoiceMode by viewModel.isVoiceMode.collectAsState()
    val voiceModeStatus by viewModel.voiceModeStatus.collectAsState()
    val voiceTranscript by viewModel.voiceTranscript.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val context = LocalContext.current
    var showProWarningDialog by remember { mutableStateOf(false) }
    var pendingModelSwitch by remember { mutableStateOf<LlmModel?>(null) }
    var highlightedUserMessageId by remember { mutableStateOf<String?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editDraft by remember { mutableStateOf("") }

    val colorScheme = remember(isDarkMode) {
        if (isDarkMode) {
            darkColorScheme(
                primary = Color.White,
                background = Color.Black,
                surface = Color(0xFF121214),
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color.Black,
                background = Color.White,
                surface = Color.White,
                onPrimary = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black
            )
        }
    }

    val gatewayColors = remember(isDarkMode) {
        GatewayColors(
            isDarkMode = isDarkMode,
            background = if (isDarkMode) Color(0xFF000000) else Color(0xFFFFFFFF),
            onBackground = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF000000),
            surface = if (isDarkMode) Color(0xFF121214) else Color(0xFFFFFFFF),
            onSurface = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF000000),
            frostedGlass = if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000),
            frostedBorder = if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000),
            userBubble = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF000000),
            userBubbleText = if (isDarkMode) Color(0xFF000000) else Color(0xFFFFFFFF),
            otherBubble = if (isDarkMode) Color(0xFF18181A) else Color(0xFFF2F2F5),
            otherBubbleText = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF000000),
            systemAmber = Color(0xFFE4A115),
            accent = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF000000)
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
                        var mistralSubMenuExpanded by remember { mutableStateOf(false) }

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
                                mistralSubMenuExpanded = selectedModel.provider == LlmProvider.MISTRAL
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
                                                    LlmProvider.MISTRAL -> mistralKey.isNotEmpty()
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

                                        // Mistral AI Section
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Mistral AI", color = colors.onBackground, fontWeight = FontWeight.Bold)
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
                                                            text = "Large 3 / Med 3.5 / Small 4 / Code",
                                                            color = colors.onBackground.copy(alpha = 0.4f),
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (mistralSubMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = colors.onBackground.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                SoundSynth.playTap()
                                                mistralSubMenuExpanded = !mistralSubMenuExpanded
                                            }
                                        )

                                        if (mistralSubMenuExpanded) {
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Mistral Large 3", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.MISTRAL_LARGE_3)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Mistral Medium 3.5", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.MISTRAL_MEDIUM_3_5)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Mistral Small 4", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.MISTRAL_SMALL_4)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Codestral", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.CODESTRAL)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Ministral 8B", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.MINISTRAL_8B)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                                                        Text("•  Ministral 3B", color = colors.onBackground)
                                                    }
                                                },
                                                onClick = { 
                                                    SoundSynth.playTap()
                                                    handleModelSelect(LlmModel.MINISTRAL_3B)
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

                        if (editingMessage != null) {
                            AlertDialog(
                                onDismissRequest = { editingMessage = null },
                                title = { Text("Edit message", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                text = {
                                    OutlinedTextField(
                                        value = editDraft,
                                        onValueChange = { editDraft = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.onBackground,
                                            unfocusedBorderColor = colors.frostedBorder,
                                            focusedTextColor = colors.onBackground,
                                            unfocusedTextColor = colors.onBackground
                                        )
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            SoundSynth.playTap()
                                            editingMessage?.let { viewModel.editUserMessage(it.id, editDraft) }
                                            editingMessage = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.userBubble, contentColor = colors.userBubbleText)
                                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { editingMessage = null }) { Text("Cancel", color = colors.onSurface.copy(alpha = 0.6f)) }
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
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        suggestions.chunked(2).forEach { row ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                row.forEach { (label, prompt) ->
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = colors.frostedGlass,
                                                        border = BorderStroke(1.dp, colors.frostedBorder),
                                                        modifier = Modifier.clickable {
                                                            SoundSynth.playTap()
                                                            viewModel.sendMessage(prompt)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                            color = colors.onBackground.copy(alpha = 0.85f),
                                                            fontSize = 11.sp,
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
                                    val isHighlighted = highlightedUserMessageId == msg.id
                                    MessageBubble(
                                        message = msg,
                                        isSpeaking = isSpeaking,
                                        isHighlighted = isHighlighted,
                                        onSpeak = { viewModel.speakText(msg.id, msg.content) },
                                        onRegenerate = { viewModel.regenerateResponse(msg.id) },
                                        onUserMessageClick = {
                                            highlightedUserMessageId = if (isHighlighted) null else msg.id
                                        },
                                        onEditUserMessage = {
                                            editDraft = msg.content
                                            editingMessage = msg
                                            highlightedUserMessageId = null
                                        },
                                        onRevertUserMessage = {
                                            viewModel.revertUserMessage(msg.id)
                                            highlightedUserMessageId = null
                                        }
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
                            isVoiceMode = isVoiceMode,
                            voiceModeStatus = voiceModeStatus,
                            voiceTranscript = voiceTranscript,
                            isMicMuted = isMicMuted,
                            isLoading = isLoading,
                            onToggleVoiceMode = { viewModel.setVoiceMode(!isVoiceMode) },
                            onUpdateVoiceStatus = { viewModel.updateVoiceStatus(it) },
                            onUpdateVoiceTranscript = { viewModel.updateVoiceTranscript(it) },
                            onToggleMicMute = { viewModel.toggleMicMute() },
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
    isHighlighted: Boolean = false,
    onSpeak: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onUserMessageClick: () -> Unit = {},
    onEditUserMessage: () -> Unit = {},
    onRevertUserMessage: () -> Unit = {}
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
            // Distinct squircle shape for message bubble
            val bubbleShape = remember { RoundedCornerShape(12.dp) }

            Column(modifier = Modifier.widthIn(max = 310.dp)) {
                Surface(
                    color = if (isUser) colors.userBubble else colors.otherBubble,
                    contentColor = if (isUser) colors.userBubbleText else colors.otherBubbleText,
                    shape = bubbleShape,
                    border = if (isHighlighted && isUser) {
                        BorderStroke(1.5.dp, colors.onBackground)
                    } else if (!isUser) {
                        BorderStroke(1.dp, if (colors.isDarkMode) Color(0xFF2C2C30) else Color(0xFFE2E2E6))
                    } else {
                        BorderStroke(1.dp, if (colors.isDarkMode) Color(0xFFFFFFFF) else Color(0xFF000000))
                    },
                    modifier = Modifier.padding(vertical = 2.dp).then(if (isUser) Modifier.clickable { onUserMessageClick() } else Modifier)
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
                                tint = colors.onBackground,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sources & Search Results",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val uriHandler = LocalUriHandler.current
                            message.groundingSources.take(5).forEach { source ->
                                Surface(
                                    color = colors.frostedGlass,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, colors.frostedBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
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
                                            color = colors.onBackground.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
                if (isUser) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isHighlighted,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp, end = 2.dp).align(Alignment.End)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.frostedGlass,
                                border = BorderStroke(1.dp, colors.frostedBorder),
                                modifier = Modifier.clickable { 
                                    SoundSynth.playTap()
                                    onEditUserMessage() 
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = colors.onBackground.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit",
                                        fontSize = 11.sp,
                                        color = colors.onBackground.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.frostedGlass,
                                border = BorderStroke(1.dp, colors.frostedBorder),
                                modifier = Modifier.clickable { 
                                    SoundSynth.playTap()
                                    onRevertUserMessage() 
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Revert",
                                        tint = colors.onBackground.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Revert",
                                        fontSize = 11.sp,
                                        color = colors.onBackground.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isUser) {
                    val context = LocalContext.current
                    var copied by remember { mutableStateOf(false) }

                    LaunchedEffect(copied) {
                        if (copied) {
                            kotlinx.coroutines.delay(2000)
                            copied = false
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 4.dp)
                    ) {
                        // Copy button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.frostedGlass,
                            border = BorderStroke(1.dp, colors.frostedBorder),
                            modifier = Modifier.clickable {
                                SoundSynth.playTap()
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("AI Response", message.content)
                                clipboard.setPrimaryClip(clip)
                                copied = true
                                android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy message",
                                    tint = colors.onBackground.copy(alpha = 0.75f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (copied) "Copied" else "Copy",
                                    fontSize = 11.sp,
                                    color = colors.onBackground.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Read Aloud / Stop button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSpeaking) colors.onBackground else colors.frostedGlass,
                            border = BorderStroke(1.dp, colors.frostedBorder),
                            modifier = Modifier.clickable { 
                                SoundSynth.playTap()
                                onSpeak() 
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop Speaking" else "Read Aloud",
                                    tint = if (isSpeaking) colors.background else colors.onBackground.copy(alpha = 0.75f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeaking) "Stop" else "Read",
                                    fontSize = 11.sp,
                                    color = if (isSpeaking) colors.background else colors.onBackground.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Regenerate button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.frostedGlass,
                            border = BorderStroke(1.dp, colors.frostedBorder),
                            modifier = Modifier.clickable { 
                                SoundSynth.playTap()
                                onRegenerate() 
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = colors.onBackground.copy(alpha = 0.75f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Regenerate",
                                    fontSize = 11.sp,
                                    color = colors.onBackground.copy(alpha = 0.75f),
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

@Composable
fun WaveformIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Black,
    isPulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_bars")
    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )
    val anim4 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(470, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w4"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val baseHeights = listOf(7.dp, 15.dp, 21.dp, 13.dp, 8.dp)
        val animFactors = listOf(anim1, anim2, anim3, anim4, anim1)

        for (i in 0 until 5) {
            val barHeight = if (isPulsing) baseHeights[i] * (0.35f + 0.65f * animFactors[i]) else baseHeights[i]
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .background(tint, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    selectedAttachment: Attachment?,
    selectedModel: LlmModel,
    isForceGrounding: Boolean,
    isVoiceMode: Boolean,
    voiceModeStatus: VoiceModeStatus,
    voiceTranscript: String,
    isMicMuted: Boolean,
    isLoading: Boolean,
    onToggleVoiceMode: () -> Unit,
    onUpdateVoiceStatus: (VoiceModeStatus) -> Unit,
    onUpdateVoiceTranscript: (String) -> Unit,
    onToggleMicMute: () -> Unit,
    onToggleForceGrounding: () -> Unit,
    onFileSelected: (Uri, String?, String) -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var showPlusMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = LocalGatewayColors.current
    val isGemini = selectedModel.provider == LlmProvider.GEMINI_FLASH || selectedModel.provider == LlmProvider.GEMINI_PRO

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required for Voice Mode", Toast.LENGTH_SHORT).show()
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val speechFallbackLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spoken = matches[0]
                if (isVoiceMode) {
                    onUpdateVoiceTranscript(spoken)
                    onSend(spoken)
                } else {
                    text += if (text.isEmpty()) spoken else " " + spoken
                }
            }
        }
        if (isVoiceMode && !isLoading) {
            onUpdateVoiceStatus(VoiceModeStatus.IDLE)
        }
    }

    val startVoiceRecognition: () -> Unit = {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            if (speechRecognizer != null) {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    onUpdateVoiceTranscript("")
                    onUpdateVoiceStatus(VoiceModeStatus.LISTENING)
                    speechRecognizer.startListening(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    }
                    speechFallbackLauncher.launch(intent)
                }
            } else {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                speechFallbackLauncher.launch(intent)
            }
        }
    }

    val stopVoiceRecognition: () -> Unit = {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onUpdateVoiceStatus(VoiceModeStatus.IDLE)
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onUpdateVoiceStatus(VoiceModeStatus.LISTENING)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                onUpdateVoiceStatus(VoiceModeStatus.PROCESSING)
            }
            override fun onError(error: Int) {
                // Return to idle unless already processing/speaking
                if (voiceModeStatus == VoiceModeStatus.LISTENING) {
                    onUpdateVoiceStatus(VoiceModeStatus.IDLE)
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                    val spoken = matches[0]
                    onUpdateVoiceTranscript(spoken)
                    onUpdateVoiceStatus(VoiceModeStatus.PROCESSING)
                    onSend(spoken)
                } else {
                    onUpdateVoiceStatus(VoiceModeStatus.IDLE)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onUpdateVoiceTranscript(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // When entering voice mode, automatically trigger microphone listening
    LaunchedEffect(isVoiceMode) {
        if (isVoiceMode) {
            if (!isMicMuted) startVoiceRecognition()
        } else {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Continuous listening loop: while in Voice Mode and mic not muted, auto-restart when idle
    LaunchedEffect(isVoiceMode, voiceModeStatus, isLoading, isMicMuted) {
        if (isVoiceMode && !isMicMuted && !isLoading && voiceModeStatus == VoiceModeStatus.IDLE) {
            kotlinx.coroutines.delay(700)
            if (isVoiceMode && !isMicMuted && !isLoading && voiceModeStatus == VoiceModeStatus.IDLE) {
                startVoiceRecognition()
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

    if (isVoiceMode) {
        // VOICE MODE ACTIVE - compact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(colors.frostedGlass, RoundedCornerShape(10.dp))
                .border(1.dp, colors.frostedBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left button: Exit voice mode - compact
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.frostedGlass,
                border = BorderStroke(1.dp, colors.frostedBorder),
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        SoundSynth.playTap()
                        onToggleVoiceMode()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Switch to texting",
                        tint = colors.onBackground.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Center Voice Status & Live transcript
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (voiceModeStatus) {
                        VoiceModeStatus.LISTENING -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Listening... Speak now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                        }
                        VoiceModeStatus.PROCESSING -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(colors.systemAmber, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thinking...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                        }
                        VoiceModeStatus.SPEAKING -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4285F4), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔊 Speaking response...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                        }
                        VoiceModeStatus.MUTED -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFF3B30), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Microphone muted • Tap mic to unmute",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3B30)
                            )
                        }
                        VoiceModeStatus.IDLE -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(colors.onBackground.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice Mode • Tap mic to talk",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (voiceTranscript.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"$voiceTranscript\"",
                        fontSize = 11.5.sp,
                        color = colors.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Mic toggle - compact
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isMicMuted) Color(0x22FF3B30) else if (voiceModeStatus == VoiceModeStatus.LISTENING) colors.onBackground else colors.frostedGlass,
                border = BorderStroke(1.dp, if (isMicMuted) Color(0xFFFF3B30).copy(alpha = 0.5f) else colors.frostedBorder),
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        SoundSynth.playTap()
                        if (isMicMuted) {
                            onToggleMicMute()
                            onUpdateVoiceStatus(VoiceModeStatus.LISTENING)
                            startVoiceRecognition()
                        } else {
                            onToggleMicMute()
                            stopVoiceRecognition()
                            onUpdateVoiceStatus(VoiceModeStatus.MUTED)
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else if (voiceModeStatus == VoiceModeStatus.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isMicMuted) "Unmute microphone" else if (voiceModeStatus == VoiceModeStatus.LISTENING) "Mute microphone" else "Start speaking",
                        tint = if (isMicMuted) Color(0xFFFF3B30) else if (!isMicMuted && voiceModeStatus == VoiceModeStatus.LISTENING) colors.background else colors.onBackground.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Extreme far right: waveform - compact
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isMicMuted) colors.frostedGlass else Color.White,
                border = BorderStroke(1.dp, if (isMicMuted) colors.frostedBorder else Color(0xFFE0E0E0)),
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        SoundSynth.playTap()
                        if (isMicMuted) {
                            onToggleMicMute()
                            onUpdateVoiceStatus(VoiceModeStatus.LISTENING)
                            startVoiceRecognition()
                        } else {
                            onToggleMicMute()
                            stopVoiceRecognition()
                            onUpdateVoiceStatus(VoiceModeStatus.MUTED)
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    WaveformIcon(
                        tint = if (isMicMuted) colors.onBackground.copy(alpha = 0.3f) else Color.Black,
                        isPulsing = !isMicMuted && (voiceModeStatus == VoiceModeStatus.LISTENING || voiceModeStatus == VoiceModeStatus.SPEAKING),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        // STANDARD TEXTING MODE - compact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(colors.frostedGlass, RoundedCornerShape(10.dp))
                .border(1.dp, colors.frostedBorder, RoundedCornerShape(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // + button far left - all-in-one tooltip for tools (now includes globe)
            Box {
            IconButton(
                onClick = {
                    SoundSynth.playTap()
                    showPlusMenu = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box {
                    Icon(Icons.Default.Add, contentDescription = "More tools", tint = if (isForceGrounding && isGemini) Color(0xFF4285F4) else colors.onBackground.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    if (isForceGrounding && isGemini) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .align(Alignment.TopEnd)
                                .background(Color(0xFF4285F4), androidx.compose.foundation.shape.CircleShape)
                                .border(1.dp, colors.surface, androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
            }
                DropdownMenu(
                    expanded = showPlusMenu,
                    onDismissRequest = { showPlusMenu = false },
                    modifier = Modifier.background(colors.surface).border(1.dp, colors.frostedBorder, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Column { Text("Attach file / image", color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("Image, doc, text", color = colors.onBackground.copy(alpha = 0.5f), fontSize = 10.sp) } },
                        leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = colors.onBackground.copy(alpha = 0.85f), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showPlusMenu = false
                            SoundSynth.playTap()
                            filePickerLauncher.launch("*/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Column { Text(if (isForceGrounding) "✓ Web grounding (next)" else "Force web grounding", color = if (!isGemini) colors.onBackground.copy(alpha = 0.35f) else if (isForceGrounding) Color(0xFF4285F4) else colors.onBackground, fontSize = 13.sp, fontWeight = if (isForceGrounding) FontWeight.Bold else FontWeight.Normal); Text(if (isGemini) "Gemini 3.7 Flash / 3.1 Pro" else "Only for Gemini — select Gemini model", color = colors.onBackground.copy(alpha = if (isGemini) 0.5f else 0.35f), fontSize = 10.sp) } },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = if (!isGemini) colors.onBackground.copy(alpha = 0.3f) else if (isForceGrounding) Color(0xFF4285F4) else colors.onBackground.copy(alpha = 0.85f), modifier = Modifier.size(18.dp)) },
                        enabled = isGemini,
                        onClick = {
                            showPlusMenu = false
                            SoundSynth.playTap()
                            onToggleForceGrounding()
                        }
                    )
                    HorizontalDivider(color = colors.frostedBorder, thickness = 0.5.dp)
                    DropdownMenuItem(
                        text = { Column { Text("/read <file>", color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("Read from working dir", color = colors.onBackground.copy(alpha = 0.5f), fontSize = 10.sp) } },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = colors.onBackground.copy(alpha = 0.85f), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showPlusMenu = false
                            text = "/read "
                        }
                    )
                    DropdownMenuItem(
                        text = { Column { Text("/export <file>", color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("Save chat to file", color = colors.onBackground.copy(alpha = 0.5f), fontSize = 10.sp) } },
                        leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null, tint = colors.onBackground.copy(alpha = 0.85f), modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showPlusMenu = false
                            text = "/export "
                        }
                    )
                    DropdownMenuItem(
                        text = { Column { Text("Clear highlights", color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("Deselect user messages", color = colors.onBackground.copy(alpha = 0.5f), fontSize = 10.sp) } },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = colors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) },
                        onClick = { showPlusMenu = false }
                    )
                }
            }

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 16.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.onBackground,
                    unfocusedTextColor = colors.onBackground,
                    cursorColor = colors.onBackground
                ),
                placeholder = { Text(if (isForceGrounding && isGemini) "Message • 🌐 grounding next" else "Message", color = colors.onBackground.copy(alpha = 0.4f), fontSize = 12.sp) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                    if (canSend) {
                        SoundSynth.playTap()
                        onSend(text)
                        text = ""
                    }
                }),
                maxLines = 3,
                minLines = 1
            )

            IconButton(
                onClick = {
                    SoundSynth.playTap()
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        speechFallbackLauncher.launch(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Dictation", tint = colors.onBackground.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = {
                    if (!canSend) return@IconButton
                    SoundSynth.playTap()
                    onSend(text)
                    text = ""
                },
                enabled = true,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) colors.onBackground else colors.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Extreme far right: Pure white squircle with black waveform icon for Voice Mode
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(36.dp)
                    .clickable {
                        SoundSynth.playTap()
                        onToggleVoiceMode()
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    WaveformIcon(tint = Color.Black)
                }
            }
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
    val mistralKey by viewModel.mistralKey.collectAsState()
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

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
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
            value = mistralKey,
            onValueChange = { viewModel.updateMistralKey(it) },
            label = { Text("Mistral API Key", color = colors.onBackground.copy(alpha = 0.7f)) },
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Quickly configure any OpenAI-compatible provider:",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBackground.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Quick presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    SoundSynth.playTap()
                    viewModel.updateCustomBaseUrl("https://api.mistral.ai/v1/chat/completions")
                    viewModel.updateCustomModelId("mistral-large-latest")
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.frostedGlass, contentColor = colors.onBackground),
                border = BorderStroke(1.dp, colors.frostedBorder)
            ) {
                Text("Mistral", fontSize = 10.sp)
            }
            Button(
                onClick = {
                    SoundSynth.playTap()
                    viewModel.updateCustomBaseUrl("https://api.groq.com/openai/v1/chat/completions")
                    viewModel.updateCustomModelId("llama-3.3-70b-versatile")
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.frostedGlass, contentColor = colors.onBackground),
                border = BorderStroke(1.dp, colors.frostedBorder)
            ) {
                Text("Groq", fontSize = 10.sp)
            }
            Button(
                onClick = {
                    SoundSynth.playTap()
                    viewModel.updateCustomBaseUrl("https://openrouter.ai/api/v1/chat/completions")
                    viewModel.updateCustomModelId("anthropic/claude-3.5-sonnet")
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.frostedGlass, contentColor = colors.onBackground),
                border = BorderStroke(1.dp, colors.frostedBorder)
            ) {
                Text("OpenRouter", fontSize = 10.sp)
            }
            Button(
                onClick = {
                    SoundSynth.playTap()
                    viewModel.updateCustomBaseUrl("https://api.deepseek.com/v1/chat/completions")
                    viewModel.updateCustomModelId("deepseek-chat")
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.frostedGlass, contentColor = colors.onBackground),
                border = BorderStroke(1.dp, colors.frostedBorder)
            ) {
                Text("DeepSeek", fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customKey,
            onValueChange = { viewModel.updateCustomKey(it) },
            shape = RoundedCornerShape(8.dp),
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
            shape = RoundedCornerShape(8.dp),
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
            shape = RoundedCornerShape(8.dp),
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
            shape = RoundedCornerShape(8.dp),
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
