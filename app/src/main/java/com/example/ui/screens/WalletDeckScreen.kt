package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.contacts.GoogleContactsHelper
import com.example.data.model.BusinessCard
import com.example.ui.components.Card3DView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PastelClay
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSand
import com.example.ui.theme.PastelSlate
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CardsUiState
import com.example.ui.viewmodel.WalletViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletDeckScreen(
    viewModel: WalletViewModel,
    onCardClick: (BusinessCard) -> Unit,
    onNavigateScan: () -> Unit,
    onNavigateAiFinder: () -> Unit,
    onNavigateContactsImport: () -> Unit,
    onNavigateSettings: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val cardsState by viewModel.cardsUiState.collectAsState()
    val filteredCards by viewModel.filteredCards.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCardIds by viewModel.selectedCardIds.collectAsState()
    val compactStackView by viewModel.compactStackView.collectAsState()
    val cardTilt3dEnabled by viewModel.cardTilt3dEnabled.collectAsState()
    val cardTypeFilter by viewModel.cardTypeFilter.collectAsState()

    var cardToDelete by remember { mutableStateOf<BusinessCard?>(null) }
    var focusedCardId by remember { mutableStateOf<String?>(null) }
    var activeFlipStates by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var batchTagInput by remember { mutableStateOf("") }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var cardForMultiPersonContact by remember { mutableStateOf<BusinessCard?>(null) }

    val allCards = when (val state = cardsState) {
        is CardsUiState.Success -> state.cards
        else -> emptyList()
    }
    val systemTagBlacklist = remember {
        setOf("google contact", "google contacts", "virtual", "technology", "finance", "design", "healthcare", "legal", "misc", "card", "professional")
    }
    val customTags = remember(allCards) {
        allCards.flatMap { it.tags }
            .map { it.lowercase().trim() }
            .distinct()
            .filter { it.isNotBlank() && it !in systemTagBlacklist }
    }
    val industryCategories = remember { listOf("Technology", "Finance", "Design", "Healthcare", "Legal") }
    val nonEmptyIndustryCategories = remember(allCards) {
        industryCategories.filter { cat -> allCards.any { it.category.equals(cat, ignoreCase = true) } }
    }
    val categories = remember(customTags) {
        listOf("All") + customTags.map { "#$it" }
    }
    val allTabIndex = 0
    val categoryPagerState = rememberPagerState(initialPage = 0) { categories.size }

    // Haptic feedback when horizontal category page changes
    LaunchedEffect(categoryPagerState.currentPage) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Always land on "All" category when screen is initialized
    LaunchedEffect(Unit) {
        categoryPagerState.scrollToPage(0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSelectionMode) {
                // Batch Selection Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.clearSelection()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedCardIds.size} Selected",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectAllCards(filteredCards)
                            }
                        ) {
                            Text("Select All", color = PastelSand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { showBatchTagDialog = true },
                            enabled = selectedCardIds.isNotEmpty(),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = "Add Tag",
                                tint = if (selectedCardIds.isNotEmpty()) PastelSand else TextMuted
                            )
                        }

                        IconButton(
                            onClick = { showBatchDeleteDialog = true },
                            enabled = selectedCardIds.isNotEmpty(),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = if (selectedCardIds.isNotEmpty()) PastelRose else TextMuted
                            )
                        }
                    }
                }
            } else {
                // Standard Header Bar: Title + Settings Button (No back button, clean nitty-gritty settings entry)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelSand.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PastelSand.copy(alpha = 0.6f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Wallet",
                                    tint = PastelSand,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Wallet",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Settings icon button for customisation & confirmed sign-out
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateSettings()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Wallet Settings & Customizations",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = {
                    Text("Search cards by name, company, or role...", color = TextMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PastelSand,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = PastelSand,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
                    .testTag("search_cards_input")
            )

            // Physical vs Virtual Card Filter Pill Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("ALL", "All Cards", null),
                    Triple("PHYSICAL", "Physical", Icons.Default.CreditCard),
                    Triple("VIRTUAL", "Virtual", Icons.Default.Language)
                ).forEach { (typeKey, label, icon) ->
                    val isSelected = cardTypeFilter == typeKey
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PastelLavender.copy(alpha = 0.2f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PastelLavender else DarkBorder
                        ),
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setCardTypeFilter(typeKey)
                            }
                            .testTag("filter_type_$typeKey")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) PastelLavender else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = label,
                                color = if (isSelected) PastelLavender else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Category Segment Indicator Tabs (Swiping or clicking shifts category)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(categories) { index, category ->
                    val isSelected = categoryPagerState.currentPage == index
                    val categoryColor = when {
                        category.startsWith("#") -> MaterialTheme.colorScheme.primary
                        category == "Technology" -> PastelSlate
                        category == "Finance" -> PastelClay
                        category == "Design" -> PastelLavender
                        category == "Healthcare" -> PastelSage
                        category == "Legal" -> PastelRose
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) categoryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) categoryColor else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    categoryPagerState.animateScrollToPage(index)
                                }
                            }
                            .testTag("category_pill_$category")
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Main Segment Area: Horizontal Swipe changes category, Scrolling down shows multiple cards in segment
            when (val state = cardsState) {
                is CardsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
                }
                is CardsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
                is CardsUiState.Success -> {
                    HorizontalPager(
                        state = categoryPagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        val currentCategory = categories.getOrElse(page) { "All" }
                        val segmentCards = when {
                            currentCategory.startsWith("#") -> {
                                val tagClean = currentCategory.removePrefix("#")
                                filteredCards.filter { c -> c.tags.any { it.equals(tagClean, ignoreCase = true) } }
                            }
                            currentCategory == "All" -> filteredCards
                            else -> filteredCards.filter { it.category.equals(currentCategory, ignoreCase = true) }
                        }

                        if (segmentCards.isEmpty()) {
                            // Empty Category State
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = DarkSurface,
                                        modifier = Modifier.size(68.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = PastelLavender,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (currentCategory == "All") "Wallet is empty" else "No $currentCategory cards",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Scan cards using camera or tap '+' below to add cards.",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                                    )
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.seedSampleCards()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DarkSurface,
                                            contentColor = PastelSand
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PastelSand.copy(alpha = 0.5f))
                                    ) {
                                        Text("Add Sample Cards", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Tactile Vertical Card Stacking in Current Segment
                            LazyColumn(
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
                                verticalArrangement = Arrangement.spacedBy(if (compactStackView) 8.dp else 14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(segmentCards, key = { it.id }) { card ->
                                    val isFocused = focusedCardId == card.id && !isSelectionMode
                                    val isFlipped = activeFlipStates[card.id] ?: false
                                    val isSelected = selectedCardIds.contains(card.id)

                                    val scale by animateFloatAsState(
                                        targetValue = if (isFocused || isSelected) 1.02f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "card_focus_scale"
                                    )

                                    val borderStrokeColor by animateColorAsState(
                                        targetValue = when {
                                            isSelected -> PastelSand
                                            isFocused -> PastelSand
                                            else -> DarkBorder
                                        },
                                        label = "card_border_color"
                                    )

                                     Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .border(
                                                width = if (isFocused || isSelected) 2.dp else 1.dp,
                                                color = borderStrokeColor,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clip(RoundedCornerShape(18.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.toggleCardSelection(card.id)
                                                    } else {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        if (isFocused) {
                                                            // Second tap opens detailed view!
                                                            onCardClick(card)
                                                        } else {
                                                            // First tap focuses card and reveals Action Dock
                                                            focusedCardId = card.id
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (!isSelectionMode) {
                                                        viewModel.startSelectionMode(card.id)
                                                    } else {
                                                        viewModel.toggleCardSelection(card.id)
                                                    }
                                                }
                                            )
                                    ) {
                                        if (compactStackView) {
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = DarkSurface,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(if (card.isVertical) 0.63f else 1.586f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = DarkSurfaceVariant,
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = card.name.take(1).uppercase().ifBlank { "C" },
                                                                color = PastelSand,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = card.name.ifBlank { card.company },
                                                                color = TextPrimary,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                            if (card.isVirtual) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = PastelLavender.copy(alpha = 0.2f)
                                                                ) {
                                                                    Text(
                                                                        text = "VIRTUAL",
                                                                        color = PastelLavender,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = "${card.company}${if (card.title.isNotBlank()) " • ${card.title}" else ""}",
                                                            color = TextSecondary,
                                                            fontSize = 12.sp,
                                                            maxLines = 1
                                                        )
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = DarkSurfaceVariant,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                                    ) {
                                                        Text(
                                                            text = card.category,
                                                            color = TextMuted,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Card3DView(
                                                card = card,
                                                isInteractive = false,
                                                enableDragTilt = cardTilt3dEnabled,
                                                showFlipButton = false,
                                                initialFlipped = isFlipped,
                                                onCardClick = null,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        // Selection checkbox badge when in batch selection mode
                                        if (isSelectionMode) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) PastelSand else Color.Black.copy(alpha = 0.65f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.5.dp,
                                                    if (isSelected) PastelSand else Color.White.copy(alpha = 0.6f)
                                                ),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(14.dp)
                                                    .size(26.dp)
                                            ) {
                                                if (isSelected) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = Color(0xFF1E1A16),
                                                            modifier = Modifier.size(16.dp)
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
                }
            }
        }

        // Active Card Action Dock (Pushed further up above bottom pill navbar, appears only when a card is focused)
        val activeCard = filteredCards.firstOrNull { it.id == focusedCardId }

        AnimatedVisibility(
            visible = activeCard != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 86.dp)
                .zIndex(10f)
        ) {
            if (activeCard != null) {
                FloatingActionDock(
                    card = activeCard,
                    onDismiss = { focusedCardId = null },
                    onFlip = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val current = activeFlipStates[activeCard.id] ?: false
                        activeFlipStates = activeFlipStates + (activeCard.id to !current)
                    },
                    onSaveToContacts = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (activeCard.secondaryName.isNotBlank()) {
                            cardForMultiPersonContact = activeCard
                        } else {
                            GoogleContactsHelper.saveContact(context, activeCard)
                            Toast.makeText(context, "Opening contacts...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenDetail = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCardClick(activeCard)
                    },
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        cardToDelete = activeCard
                    }
                )
            }
        }

        // Bottom Center Pill-Shaped Navbar with pure icons (no labels anywhere)
        PillBottomNavbar(
            selectedTab = 0, // 0 = Wallet/Cards
            onScanClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNavigateScan()
            },
            onAiSearchClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNavigateAiFinder()
            },
            onContactsClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNavigateContactsImport()
            },
            onSettingsClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNavigateSettings()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp)
                .zIndex(20f)
        )

        // Card Deletion Dialog
        cardToDelete?.let { card ->
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
                title = {
                    Text(text = "Remove Card", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        text = "Remove ${card.name.ifBlank { "this card" }} from your wallet?",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCard(card.id)
                            cardToDelete = null
                            focusedCardId = null
                            Toast.makeText(context, "Card removed", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelRose, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToDelete = null }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Batch Tagging Dialog
        if (showBatchTagDialog) {
            AlertDialog(
                onDismissRequest = {
                    showBatchTagDialog = false
                    batchTagInput = ""
                },
                title = {
                    Text("Tag Selected Cards", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Add a custom tag to ${selectedCardIds.size} cards (e.g. useful, client, lead):",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = batchTagInput,
                            onValueChange = { batchTagInput = it },
                            placeholder = { Text("useful", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelSand,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("useful", "client", "vip", "lead").forEach { quickTag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.clickable { batchTagInput = quickTag }
                                ) {
                                    Text(
                                        text = quickTag,
                                        color = PastelSand,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val tag = batchTagInput.ifBlank { "useful" }.trim()
                            viewModel.addTagToSelectedCards(tag)
                            showBatchTagDialog = false
                            batchTagInput = ""
                            Toast.makeText(context, "Tag #$tag added to selected cards", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSand, contentColor = Color(0xFF1E1A16)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply Tag", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showBatchTagDialog = false
                        batchTagInput = ""
                    }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Batch Delete Confirmation Dialog
        if (showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                title = {
                    Text("Delete ${selectedCardIds.size} Cards", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove ${selectedCardIds.size} cards from your wallet?",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val count = selectedCardIds.size
                            viewModel.deleteSelectedCards()
                            showBatchDeleteDialog = false
                            Toast.makeText(context, "$count cards removed", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelRose, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete All", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Multi-Person Save Contact Dialog
        cardForMultiPersonContact?.let { card ->
            AlertDialog(
                onDismissRequest = { cardForMultiPersonContact = null },
                title = {
                    Text("Save to Contacts", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Multiple people found on this card for ${card.company}. Select who to save:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        // Person 1
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    GoogleContactsHelper.saveContact(context, card, card.name, card.phone)
                                    cardForMultiPersonContact = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PastelSand, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = card.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = card.phone, color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                        // Person 2
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    GoogleContactsHelper.saveContact(context, card, card.secondaryName, card.secondaryPhone.ifBlank { card.phone })
                                    cardForMultiPersonContact = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = card.secondaryName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = card.secondaryPhone.ifBlank { card.phone }, color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            GoogleContactsHelper.saveContact(context, card, card.name, card.phone)
                            cardForMultiPersonContact = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSand, contentColor = Color(0xFF1E1A16)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Primary", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardForMultiPersonContact = null }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * Floating Action Dock that appears pushed comfortably up when a card is tapped and focused.
 */
@Composable
private fun FloatingActionDock(
    card: BusinessCard,
    onDismiss: () -> Unit,
    onFlip: () -> Unit,
    onSaveToContacts: () -> Unit,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkSurface.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Card name, company, and close unfocus button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name.ifBlank { "Card Selected" },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${card.company.ifBlank { "Wallet Card" }} • Tap card again to open",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unfocus",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Quick Communication Actions Row (Call, WhatsApp, Email, Flip, Maps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (card.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${card.phone}"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = PastelSand, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = { GoogleContactsHelper.openWhatsApp(context, card.phone) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = PastelMint, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                if (card.email.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${card.email}"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = PastelLavender, modifier = Modifier.size(14.dp))
                    }
                }

                OutlinedButton(
                    onClick = onFlip,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip", tint = PastelClay, modifier = Modifier.size(14.dp))
                }

                if (card.address.isNotBlank()) {
                    OutlinedButton(
                        onClick = { GoogleContactsHelper.openAddressInMaps(context, card.address) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = "Maps", tint = PastelSlate, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Primary Bottom Actions: Save to Contacts + Open Full Detail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSaveToContacts,
                    colors = ButtonDefaults.buttonColors(containerColor = PastelSand, contentColor = Color(0xFF1E1A16)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Contact", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = onOpenDetail,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Text("Details", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = PastelRose, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

/**
 * Bottom Center Pill-Shaped Navbar with pure icons (no labels anywhere, even on landing).
 * The plus (+) button is centered right in the middle of the pill.
 */
@Composable
private fun PillBottomNavbar(
    selectedTab: Int,
    onScanClick: () -> Unit,
    onAiSearchClick: () -> Unit,
    onContactsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 18.dp,
        modifier = modifier.height(58.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Wallet Tab
            PillNavItem(
                icon = Icons.Default.CreditCard,
                contentDescription = "Cards",
                isSelected = selectedTab == 0,
                onClick = { /* already on cards */ },
                testTag = "nav_tab_cards"
            )

            // 2. AI Assistant Tab
            PillNavItem(
                icon = Icons.Default.AutoAwesome,
                contentDescription = "AI Finder",
                isSelected = selectedTab == 1,
                onClick = onAiSearchClick,
                testTag = "nav_tab_ai"
            )

            // 3. Center Plus (+) Scan Button inside the Pill
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onScanClick() }
                    .testTag("nav_center_plus_btn"),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Scan or Add Business Card",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 4. Contacts Import Tab
            PillNavItem(
                icon = Icons.Default.PersonAdd,
                contentDescription = "Contacts",
                isSelected = selectedTab == 3,
                onClick = onContactsClick,
                testTag = "nav_tab_contacts"
            )

            // 5. Settings Tab
            PillNavItem(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                isSelected = selectedTab == 4,
                onClick = onSettingsClick,
                testTag = "nav_tab_settings"
            )
        }
    }
}

/**
 * Individual item in the bottom pill navbar: pure icon, circular touch target, no label.
 */
@Composable
private fun PillNavItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
