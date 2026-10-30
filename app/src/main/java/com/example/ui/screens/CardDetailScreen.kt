package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.CardDistanceEtaView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.contacts.GoogleContactsHelper
import com.example.data.model.BusinessCard
import com.example.ui.components.Card3DView
import com.example.ui.theme.CardGradients
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelRose
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.ui.theme.PastelSand
import com.example.ui.theme.PastelSky
import com.example.ui.viewmodel.WalletViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardDetailScreen(
    card: BusinessCard,
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val allCardsState by viewModel.cardsUiState.collectAsState()
    val liveCard = (allCardsState as? com.example.ui.viewmodel.CardsUiState.Success)?.cards?.find { it.id == card.id } ?: card
    var currentCard by remember(liveCard) { mutableStateOf(liveCard) }
    LaunchedEffect(liveCard) {
        currentCard = liveCard
    }
    var isEditing by remember { mutableStateOf(false) }
    var personToSaveDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val scrollState = rememberScrollState()
    val showDistanceEta by viewModel.showDistanceEta.collectAsState()

    val pastelColorOptions = listOf(
        "cyber_neon" to "Mint Breeze",
        "electric_purple" to "Lavender Dream",
        "obsidian_gold" to "Peach Sunset",
        "deep_stealth" to "Sky Aura",
        "sunset_blaze" to "Rose Blossom",
        "emerald_matrix" to "Lemon Zest"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Detail Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_back_detail")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Card Details",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        viewModel.deleteCard(currentCard.id)
                        Toast.makeText(context, "Card removed", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_delete_card")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete card",
                        tint = PastelRose.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card3DView(
                card = currentCard,
                isInteractive = true,
                enableDragTilt = true,
                showFlipButton = true,
                onCardClick = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            var activePersonName by remember(currentCard) { mutableStateOf(currentCard.name) }
            var activePersonPhone by remember(currentCard) { mutableStateOf(currentCard.phone) }

            // Multi-person chip selector (if multiple individuals detected on card)
            if (currentCard.secondaryName.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PEOPLE ON THIS CARD (TAP TO SWITCH, LONG CLICK TO SAVE)",
                        color = PastelLavender,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Person 1 Chip
                        val isPerson1Selected = activePersonName == currentCard.name
                        Surface(
                            shape = CircleShape,
                            color = if (isPerson1Selected) PastelSand.copy(alpha = 0.2f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPerson1Selected) PastelSand else DarkBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        activePersonName = currentCard.name
                                        activePersonPhone = currentCard.phone
                                    },
                                    onLongClick = {
                                        personToSaveDialog = Pair(currentCard.name, currentCard.phone)
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentCard.name.ifBlank { "Primary" },
                                    color = if (isPerson1Selected) PastelSand else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Person 2 Chip
                        val isPerson2Selected = activePersonName == currentCard.secondaryName
                        Surface(
                            shape = CircleShape,
                            color = if (isPerson2Selected) PastelSand.copy(alpha = 0.2f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPerson2Selected) PastelSand else DarkBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        activePersonName = currentCard.secondaryName
                                        activePersonPhone = if (currentCard.secondaryPhone.isNotBlank()) currentCard.secondaryPhone else currentCard.phone
                                    },
                                    onLongClick = {
                                        val phone = if (currentCard.secondaryPhone.isNotBlank()) currentCard.secondaryPhone else currentCard.phone
                                        personToSaveDialog = Pair(currentCard.secondaryName, phone)
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentCard.secondaryName,
                                    color = if (isPerson2Selected) PastelSand else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Primary Networking Action: Save Contact
            Button(
                onClick = {
                    GoogleContactsHelper.saveContact(
                        context = context,
                        card = currentCard,
                        targetName = activePersonName,
                        targetPhone = activePersonPhone
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_to_contacts"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PastelSand,
                    contentColor = Color(0xFF1E1A16)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentCard.secondaryName.isNotBlank()) "Save $activePersonName to Contacts" else "Save Contact",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Quick Contact Communication Row (Call, WhatsApp, Email, Website, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activePersonPhone.isNotBlank() || currentCard.phone.isNotBlank()) {
                    val phoneToUse = if (activePersonPhone.isNotBlank()) activePersonPhone else currentCard.phone
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneToUse"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = PastelSand,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Call",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Direct WhatsApp Action Button
                    OutlinedButton(
                        onClick = {
                            GoogleContactsHelper.openWhatsApp(context, phoneToUse)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "WhatsApp",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (currentCard.email.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${currentCard.email}"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = PastelLavender,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Email",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (currentCard.website.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val url = if (!currentCard.website.startsWith("http")) "https://${currentCard.website}" else currentCard.website
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Website",
                            tint = PastelSky,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Web",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${currentCard.name} - ${currentCard.title} at ${currentCard.company}\nPhone: ${currentCard.phone}\nEmail: ${currentCard.email}"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Business Card"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Lively Pastel Finish Palette Picker
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PASTEL 3D CARD FINISH",
                        color = PastelLavender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pastelColorOptions) { (key, label) ->
                            val isSelected = currentCard.cardColor == key
                            val brush = CardGradients.getBrush(key)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(brush)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        currentCard = currentCard.copy(cardColor = key)
                                        viewModel.saveDraftCard(currentCard) {}
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (currentCard.isVirtual) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PastelLavender.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PastelLavender.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PastelLavender.copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = PastelLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Virtual Digital Card",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentCard.websiteUrl.ifBlank { "Imported via web presence" },
                                color = PastelLavender,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Contact Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONTACT DETAILS",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isEditing) "Save Changes" else "Edit",
                            color = PastelMint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clickable {
                                if (isEditing) {
                                    viewModel.saveDraftCard(currentCard) {
                                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isEditing = !isEditing
                            }
                        )
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = currentCard.name,
                            onValueChange = { currentCard = currentCard.copy(name = it) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.title,
                            onValueChange = { currentCard = currentCard.copy(title = it) },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.company,
                            onValueChange = { currentCard = currentCard.copy(company = it) },
                            label = { Text("Company") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.phone,
                            onValueChange = { currentCard = currentCard.copy(phone = it) },
                            label = { Text("Primary Mobile") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.secondaryPhone,
                            onValueChange = { currentCard = currentCard.copy(secondaryPhone = it) },
                            label = { Text("Secondary Mobile") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.telephone,
                            onValueChange = { currentCard = currentCard.copy(telephone = it) },
                            label = { Text("Telephone (Landline)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.email,
                            onValueChange = { currentCard = currentCard.copy(email = it) },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.website,
                            onValueChange = { currentCard = currentCard.copy(website = it) },
                            label = { Text("Website") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.address,
                            onValueChange = { currentCard = currentCard.copy(address = it) },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                        OutlinedTextField(
                            value = currentCard.notes,
                            onValueChange = { currentCard = currentCard.copy(notes = it) },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = detailOutlinedColors()
                        )
                    } else {
                        DetailItem(label = "Primary Contact", value = currentCard.name)
                        if (currentCard.secondaryName.isNotBlank()) {
                            DetailItem(label = "Partner / Associate", value = currentCard.secondaryName)
                        }
                        DetailItem(label = "Position / Role", value = currentCard.title)
                        DetailItem(label = "Company / Org", value = currentCard.company)
                        DetailItem(label = "Mobile Phone", value = currentCard.phone)
                        if (currentCard.secondaryPhone.isNotBlank()) {
                            DetailItem(label = "Secondary Mobile", value = currentCard.secondaryPhone)
                        }
                        if (currentCard.telephone.isNotBlank()) {
                            DetailItem(label = "Telephone (Landline)", value = currentCard.telephone)
                        }
                        DetailItem(label = "Email Address", value = currentCard.email)
                        DetailItem(label = "Website", value = currentCard.website)

                        if (currentCard.address.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Office Address",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentCard.address,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable {
                                            GoogleContactsHelper.openAddressInMaps(context, currentCard.address)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = "Open in Maps",
                                            tint = PastelSand,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (currentCard.notes.isNotBlank()) {
                            DetailItem(label = "Meeting & Networking Notes", value = currentCard.notes)
                        }
                    }
                }
            }

            if (showDistanceEta && currentCard.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                CardDistanceEtaView(address = currentCard.address)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (personToSaveDialog != null) {
            val (pName, pPhone) = personToSaveDialog!!
            AlertDialog(
                onDismissRequest = { personToSaveDialog = null },
                title = {
                    Text(
                        text = "Save to Contacts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "Save $pName ($pPhone) at ${currentCard.company} as an individual contact with this business card's photo and company info?",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            GoogleContactsHelper.saveContact(context, currentCard, pName, pPhone)
                            personToSaveDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelSand,
                            contentColor = Color(0xFF1E1A16)
                        )
                    ) {
                        Text("Save Contact", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { personToSaveDialog = null }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            softWrap = true,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun detailOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurfaceVariant,
    focusedBorderColor = PastelMint,
    unfocusedBorderColor = DarkBorder,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
