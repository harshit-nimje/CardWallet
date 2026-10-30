package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.contacts.GoogleContactsHelper
import com.example.data.model.BusinessCard
import com.example.ui.components.Card3DView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelSand
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiFinderUiState
import com.example.ui.viewmodel.WalletViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiFinderScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onCardClick: (BusinessCard) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.resetAiFinder()
        onBack()
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var queryText by remember { mutableStateOf("") }
    val aiState by viewModel.aiFinderState.collectAsState()
    val scrollState = rememberScrollState()

    // Business networking challenge suggestion chips
    val suggestionChips = listOf(
        "Seed & Series A funding",
        "IP & Patent lawyer",
        "AI / ML Architect",
        "Contract litigation",
        "Payment infrastructure",
        "Spatial UX designer",
        "Biotech advisor",
        "Supply chain lead"
    )

    // Animated atmospheric glow in the lower half
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glowAnimX by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowX"
    )
    val glowAnimY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowY"
    )
    val glowPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Atmospheric Ambient Gradient Aura at the lower half
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.BottomCenter)
                .blur(70.dp)
        ) {
            val cx = size.width * 0.5f + glowAnimX
            val cy = size.height * 0.7f + glowAnimY
            val radius = size.width * 0.65f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE879F9).copy(alpha = glowPulseAlpha * 0.45f), // Soft neon magenta
                        Color(0xFF8B5CF6).copy(alpha = glowPulseAlpha * 0.40f), // Soft violet
                        Color(0xFFF43F5E).copy(alpha = glowPulseAlpha * 0.25f), // Warm rose
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(scrollState)
                .padding(bottom = 90.dp)
        ) {
            // Top minimal brand header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.resetAiFinder()
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "wallet.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PastelSand)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SMART RETRIEVAL",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Central content: Headline, suggestion pills, and match result
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Bold hero headline for network problem solving
                Text(
                    text = "Who in your network\ncan help?",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 40.sp,
                    letterSpacing = (-1.0).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Describe a challenge, role, or venture need to uncover the right contact.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Suggestion chips (FlowRow with rounded pills)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    suggestionChips.forEach { chipText ->
                        Surface(
                            shape = CircleShape,
                            color = Color(0x331E1E26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x448B5CF6)),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .clickable {
                                    queryText = chipText
                                    viewModel.searchWithAi(chipText)
                                    focusManager.clearFocus()
                                }
                        ) {
                            Text(
                                text = chipText,
                                color = Color(0xFFF1F5F9),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Result or Loading State
                when (val state = aiState) {
                    is AiFinderUiState.Idle -> {
                        // Clean, minimal atmospheric space
                    }
                    is AiFinderUiState.Searching -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = PastelLavender,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Finding matching cards across your wallet...",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is AiFinderUiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = Color(0xFFFDA4AF),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is AiFinderUiState.Result -> {
                        val match = state.match
                        if (match.matchedCard != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xEE121218),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PastelLavender.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PastelLavender,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "MATCHED FOR YOUR NEED",
                                                color = PastelLavender,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.resetAiFinder() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Card preview
                                    Card3DView(
                                        card = match.matchedCard,
                                        isInteractive = true,
                                        showFlipButton = true,
                                        onCardClick = { onCardClick(match.matchedCard) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(if (match.matchedCard.isVertical) 0.63f else 1.586f)
                                    )

                                    // Why it matches
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "WHY THIS FITS YOUR VIBE / QUERY",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = match.reasoning,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }

                                    // Quick communication row: Call & WhatsApp
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (match.matchedCard.phone.isNotBlank()) {
                                            Button(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${match.matchedCard.phone}"))
                                                    context.startActivity(intent)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PastelSand,
                                                    contentColor = Color(0xFF1E1A16)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Call", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    GoogleContactsHelper.openWhatsApp(context, match.matchedCard.phone)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Chat,
                                                    contentDescription = "WhatsApp",
                                                    tint = Color(0xFF25D366),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = match.reasoning,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Bottom Search Bar at thumb's reach, floating above keyboard without pushing the page or header/chips
        Surface(
            shape = CircleShape,
            color = Color(0xEE16151D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                .height(58.dp)
        ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sparkle icon (✦)
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PastelLavender,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text input
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (queryText.isEmpty()) {
                            Text(
                                text = "Ask anything (e.g. 'Need an IP lawyer or seed fund')...",
                                color = Color(0xFF64748B),
                                fontSize = 13.5.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(PastelLavender),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (queryText.isNotBlank()) {
                                        viewModel.searchWithAi(queryText)
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_ai_query")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Circular Send / Up-Arrow Button
                    Surface(
                        shape = CircleShape,
                        color = if (queryText.isNotBlank()) Color(0xFF6D28D9) else Color(0x334C1D95),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(enabled = queryText.isNotBlank()) {
                                viewModel.searchWithAi(queryText)
                                focusManager.clearFocus()
                            }
                            .testTag("btn_submit_ai_query")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Search",
                                tint = if (queryText.isNotBlank()) Color.White else Color(0x66FFFFFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
