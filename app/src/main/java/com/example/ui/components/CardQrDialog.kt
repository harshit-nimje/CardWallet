package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.export.VCardExporter
import com.example.data.model.BusinessCard
import com.example.data.qr.CardQrGenerator
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelSand
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CardQrDialog(
    card: BusinessCard,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var qrMode by remember { mutableIntStateOf(0) } // 0 = vCard Contact, 1 = Web URL
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(card, qrMode) {
        isGenerating = true
        withContext(Dispatchers.Default) {
            val bmp = if (qrMode == 0 || card.website.isBlank()) {
                CardQrGenerator.generateVCardQrBitmap(card, sizePx = 640)
            } else {
                CardQrGenerator.generateWebsiteQrBitmap(card, sizePx = 640)
            }
            withContext(Dispatchers.Main) {
                qrBitmap = bmp
                isGenerating = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with title and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelSand.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = PastelSand,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Contact QR Code",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Anyone can scan to save contact directly",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // If website available, allow toggle between vCard and Web URL
                if (card.website.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("vCard Contact", "Website URL").forEachIndexed { index, label ->
                            val isSel = qrMode == index
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) PastelSand.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) PastelSand else DarkBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable { qrMode = index }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (isSel) PastelSand else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // QR Code Display Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .padding(vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, PastelSand.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier.padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating || qrBitmap == null) {
                            CircularProgressIndicator(
                                color = Color(0xFF1E1A16),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "vCard QR Code",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Contact Identification Summary
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = card.name.ifBlank { card.company },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (card.title.isNotBlank() || card.company.isNotBlank()) {
                        Text(
                            text = listOfNotNull(card.title.takeIf { it.isNotBlank() }, card.company.takeIf { it.isNotBlank() }).joinToString(" • "),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (card.phone.isNotBlank()) {
                        Text(
                            text = card.phone,
                            color = PastelSand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            VCardExporter.shareCardsAsVcf(context, listOf(card), "${card.name.ifBlank { "contact" }}.vcf")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelSand,
                            contentColor = Color(0xFF1E1A16)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share .vcf", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val info = buildString {
                                appendLine(card.name)
                                if (card.company.isNotBlank()) appendLine(card.company)
                                if (card.title.isNotBlank()) appendLine(card.title)
                                if (card.phone.isNotBlank()) appendLine("Phone: ${card.phone}")
                                if (card.email.isNotBlank()) appendLine("Email: ${card.email}")
                                if (card.website.isNotBlank()) appendLine("Website: ${card.website}")
                                if (card.address.isNotBlank()) appendLine("Address: ${card.address}")
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Contact Info", info))
                            Toast.makeText(context, "Contact copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Info", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
