package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BusinessCard
import com.example.ui.theme.CardGradients
import kotlin.math.sin

import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
fun Card3DView(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    enableDragTilt: Boolean = false,
    showFlipButton: Boolean = true,
    initialFlipped: Boolean = false,
    externalTiltX: Float = 0f,
    externalTiltY: Float = 0f,
    onCardClick: (() -> Unit)? = null,
    onFavoriteToggle: (() -> Unit)? = null
) {
    var isFlipped by remember(initialFlipped) { mutableStateOf(initialFlipped) }
    var dragTiltX by remember { mutableFloatStateOf(0f) }
    var dragTiltY by remember { mutableFloatStateOf(0f) }

    // Face culling: when flip exceeds 90 degrees, show Back face
    val isBackSideFacing = initialFlipped || (isFlipped && !initialFlipped) // simplified for logic below

    // Physical, responsive flip animation
    val targetFlip = if (isFlipped) 180f else 0f
    val animatedFlipY by animateFloatAsState(
        targetValue = targetFlip,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "flipY"
    )

    // Dynamic Orientation Detection from Card Metadata and Image Bounds
    val context = androidx.compose.ui.platform.LocalContext.current
    val isVerticalDetected = remember(card.isVertical, card.frontImageUri, card.backImageUri) {
        if (card.isVertical) {
            true
        } else {
            val frontVertical = com.example.data.storage.CardImageHelper.isImageVertical(context, card.frontImageUri)
            val backVertical = com.example.data.storage.CardImageHelper.isImageVertical(context, card.backImageUri)
            frontVertical ?: backVertical ?: false
        }
    }

    // Subtle physical tilt (Apple Wallet inspired: gentle feedback, not exaggerated)
    val animatedDragX by animateFloatAsState(
        targetValue = dragTiltX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "dragX"
    )
    val animatedDragY by animateFloatAsState(
        targetValue = dragTiltY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "dragY"
    )

    val flipRadians = Math.toRadians(animatedFlipY.toDouble()).toFloat()
    val midFlipLift = sin(flipRadians)
    val dynamicElevation = (8 + (midFlipLift * 10)).dp

    val density = LocalDensity.current.density
    val brush = CardGradients.getBrush(card.cardColor)
    val pastelAccent = CardGradients.getPrimaryColor(card.cardColor)
    val darkAccent = CardGradients.getAccentDarkColor(card.cardColor)

    // Face culling: when flip exceeds 90 degrees, show Back face
    val isBackSideFacingActual = animatedFlipY > 90f

    val totalTiltX = (animatedDragX + externalTiltX).coerceIn(-6f, 6f)
    val totalTiltY = (animatedDragY + externalTiltY).coerceIn(-6f, 6f)

    val cardShape = RoundedCornerShape(22.dp)

    val dynamicAspectRatio = if (isVerticalDetected) 0.63f else 1.586f

    Box(
        modifier = modifier
            .testTag("card_3d_${card.id}")
            .shadow(
                elevation = dynamicElevation,
                shape = cardShape,
                ambientColor = if (isBackSideFacingActual) Color(0xFF0F172A).copy(alpha = 0.3f) else pastelAccent.copy(alpha = 0.35f),
                spotColor = if (isBackSideFacingActual) pastelAccent.copy(alpha = 0.2f) else darkAccent.copy(alpha = 0.25f)
            )
            .graphicsLayer {
                rotationY = animatedFlipY + totalTiltY
                rotationX = -totalTiltX
                cameraDistance = 14f * density
            }
            .then(
                if (isInteractive && enableDragTilt) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragTiltY = (dragTiltY + dragAmount.x * 0.04f).coerceIn(-5f, 5f)
                                dragTiltX = (dragTiltX + dragAmount.y * 0.04f).coerceIn(-5f, 5f)
                            },
                            onDragEnd = {
                                dragTiltX = 0f
                                dragTiltY = 0f
                            },
                            onDragCancel = {
                                dragTiltX = 0f
                                dragTiltY = 0f
                            }
                        )
                    }
                } else Modifier
            )
            .clip(cardShape)
            .background(if (isBackSideFacingActual) CardGradients.VelvetObsidianBrush else brush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isBackSideFacingActual) {
                        listOf(pastelAccent.copy(alpha = 0.6f), Color(0xFF334155))
                    } else {
                        listOf(Color.White.copy(alpha = 0.8f), pastelAccent.copy(alpha = 0.4f))
                    }
                ),
                shape = cardShape
            )
            .then(if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier)
            .aspectRatio(dynamicAspectRatio)
    ) {
        if (!isBackSideFacingActual) {
            // Check if card has a scanned physical front image
            if (card.frontImageUri.isNotBlank()) {
                ScannedCardFrontLook(
                    card = card,
                    showFlipButton = showFlipButton,
                    onFlip = { isFlipped = !isFlipped }
                )
            } else {
                CleanCardFrontContent(
                    card = card,
                    pastelAccent = pastelAccent,
                    showFlipButton = showFlipButton,
                    onFlip = { isFlipped = !isFlipped }
                )
            }
        } else {
            // Back face (counter-mirrored)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                if (card.backImageUri.isNotBlank()) {
                    ScannedCardBackLook(
                        card = card,
                        showFlipButton = showFlipButton,
                        onFlip = { isFlipped = !isFlipped }
                    )
                } else {
                    CleanCardBackContent(
                        card = card,
                        pastelAccent = pastelAccent,
                        showFlipButton = showFlipButton,
                        onFlip = { isFlipped = !isFlipped }
                    )
                }
            }
        }
    }
}

/**
 * Scanned Image Look: Front face using the real captured physical card photo
 */
@Composable
private fun ScannedCardFrontLook(
    card: BusinessCard,
    showFlipButton: Boolean,
    onFlip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = card.frontImageUri,
            contentDescription = "Scanned card front",
            contentScale = if (card.scaleMode == "fit") ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient top scrim for control clarity
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // Top control bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = card.category.uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                if (card.isVirtual) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF6366F1).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "VIRTUAL",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                if (showFlipButton) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onFlip() }
                            .testTag("btn_flip_front_${card.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Flip",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

    }
}

/**
 * Scanned Image Look: Back face using the real captured physical card photo
 */
@Composable
private fun ScannedCardBackLook(
    card: BusinessCard,
    showFlipButton: Boolean,
    onFlip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = card.backImageUri,
            contentDescription = "Scanned card back",
            contentScale = if (card.scaleMode == "fit") ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top Flip Button overlay
        if (showFlipButton) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clickable { onFlip() }
                    .testTag("btn_flip_back_${card.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Flip to front",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean Front View with proper text wrapping and Material icons
 */
@Composable
private fun CleanCardFrontContent(
    card: BusinessCard,
    pastelAccent: Color,
    showFlipButton: Boolean,
    onFlip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetallicEmvChip()
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = pastelAccent.copy(alpha = 0.9f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, pastelAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = card.category.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (card.isVirtual) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF6366F1).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "VIRTUAL",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }



                if (showFlipButton) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onFlip() }
                            .testTag("btn_flip_front_${card.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Flip",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Center Identity: Clean layout with thumbnail logo preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name.ifBlank { "Unnamed Contact" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (card.title.isNotBlank()) {
                    Text(
                        text = card.title,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (card.company.isNotBlank()) {
                    Text(
                        text = card.company,
                        color = pastelAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (card.thumbnailUrl.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(42.dp)
                ) {
                    AsyncImage(
                        model = card.thumbnailUrl,
                        contentDescription = "Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(4.dp).fillMaxSize()
                    )
                }
            }
        }

        // Bottom Strip: Direct Communications
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (card.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = pastelAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = card.phone,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (card.email.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = pastelAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = card.email,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Executive Back View
 */
@Composable
private fun CleanCardBackContent(
    card: BusinessCard,
    pastelAccent: Color,
    showFlipButton: Boolean,
    onFlip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = pastelAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VERIFIED CARD",
                    color = pastelAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            if (showFlipButton) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onFlip() }
                        .testTag("btn_flip_back_${card.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip back",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Details with clean wrapping
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (card.website.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = pastelAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.website,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (card.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.address,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (card.notes.isNotBlank()) {
                Text(
                    text = card.notes,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Footer: Tags & QR Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                card.tags.take(3).forEach { tag ->
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            color = pastelAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Metallic Dual-Tone EMV Chip
 */
@Composable
fun MetallicEmvChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(width = 30.dp, height = 22.dp)
            .shadow(2.dp, RoundedCornerShape(4.dp)),
        color = Color(0xFFE2C46F),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFB5933C))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFF8C6D1F))
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .background(Color(0xFF8C6D1F))
            )
        }
    }
}
