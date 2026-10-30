package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import android.content.ClipboardManager
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.BusinessCard
import com.example.data.ocr.CardOcrHelper
import com.example.ui.components.Card3DView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelSand
import com.example.ui.viewmodel.WalletViewModel
import java.util.concurrent.Executors

@Composable
fun ScanCardScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var cardMediumMode by remember { mutableIntStateOf(0) } // 0 = Physical, 1 = Virtual / QR
    var virtualUrlInput by remember { mutableStateOf("https://") }
    var virtualScanMethod by remember { mutableIntStateOf(0) } // 0 = Live Camera QR Scanner, 1 = Web URL / vCard Text, 2 = Gallery Photo QR
    var activeSideTab by remember { mutableIntStateOf(0) } // 0 = Front, 1 = Back
    var inputMode by remember { mutableIntStateOf(0) } // 0 = Camera, 1 = Gallery
    var isVerticalFacing by remember { mutableStateOf(false) } // Horizontal (16:9) vs Vertical (9:16)
    var isPreviewFit by remember { mutableStateOf(false) }

    val frontBitmap by viewModel.scanFrontImage.collectAsState()
    val backBitmap by viewModel.scanBackImage.collectAsState()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()
    val ocrEngineStatus by viewModel.ocrEngineStatus.collectAsState()
    val draftCard by viewModel.draftCard.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val duplicateDetection by viewModel.duplicateDetection.collectAsState()

    // Camera permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission needed for scanning", Toast.LENGTH_SHORT).show()
            inputMode = 1 // Fallback to gallery
            virtualScanMethod = 1
        }
    }

    // Photo picker fallback & QR barcode scanner
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val rawBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (rawBitmap != null) {
                    val properlyOrientedBitmap = CardOcrHelper.rotateBitmapIfNeeded(context, uri, rawBitmap)
                    if (cardMediumMode == 1) {
                        // Scan for QR code / Barcode first
                        val inputImage = InputImage.fromBitmap(properlyOrientedBitmap, 0)
                        val scanner = BarcodeScanning.getClient()
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val qr = barcodes.firstOrNull()?.rawValue
                                if (!qr.isNullOrBlank()) {
                                    if (qr.contains("BEGIN:VCARD", ignoreCase = true)) {
                                        viewModel.parseAndApplyVCard(qr) { c ->
                                            Toast.makeText(context, "Parsed vCard: ${c.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        virtualUrlInput = qr
                                        viewModel.createVirtualCardFromUrl(qr) { c ->
                                            Toast.makeText(context, "Loaded web card for ${c.company}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "No QR Code detected in image", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "QR Scan failed", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        if (properlyOrientedBitmap.height > properlyOrientedBitmap.width) {
                            isVerticalFacing = true
                        }
                        viewModel.processScanBitmap(properlyOrientedBitmap, isBack = activeSideTab == 1)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(draftCard?.isVertical) {
        draftCard?.let {
            if (it.isVertical && !isVerticalFacing) {
                isVerticalFacing = true
            }
        }
    }

    val isEffectiveVertical = isVerticalFacing || (draftCard?.isVertical == true)

    val currentCard = (draftCard ?: BusinessCard(
        userId = user?.uid ?: "",
        name = "",
        company = "",
        title = "",
        phone = "",
        email = "",
        category = "Technology",
        isVertical = isEffectiveVertical
    )).copy(isVertical = isEffectiveVertical)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Scan Header
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
                    .testTag("btn_back_scan")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Scan Card",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { viewModel.clearScanner() },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_reset_scan")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Top Physical vs Virtual / QR Card Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (cardMediumMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (cardMediumMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable { cardMediumMode = 0 }
                    .testTag("mode_physical_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = if (cardMediumMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Physical Card (OCR)",
                        color = if (cardMediumMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (cardMediumMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (cardMediumMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable {
                        cardMediumMode = 1
                        if (draftCard == null) {
                            viewModel.updateDraftCard(currentCard.copy(isVirtual = true))
                        }
                    }
                    .testTag("mode_virtual_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (cardMediumMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Virtual / QR Code",
                        color = if (cardMediumMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (cardMediumMode == 0) {
            // Card Side Selector (Front / Back) for Physical Card
            TabRow(
                selectedTabIndex = activeSideTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeSideTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = activeSideTab == 0,
                    onClick = { activeSideTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Front Side",
                                fontWeight = if (activeSideTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                            if (frontBitmap != null || currentCard.frontImageUri.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
                Tab(
                    selected = activeSideTab == 1,
                    onClick = { activeSideTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Back Side",
                                fontWeight = if (activeSideTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                            if (backBitmap != null || currentCard.backImageUri.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Format Orientation Toggle (Horizontal 16:9 vs Vertical 9:16)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isVerticalFacing) "Format: Vertical Card (9:16)" else "Format: Horizontal Card (16:9)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable {
                        isVerticalFacing = !isVerticalFacing
                        val updated = (draftCard ?: currentCard).copy(
                            isVertical = isVerticalFacing,
                            scaleMode = if (isPreviewFit) "fit" else "crop"
                        )
                        viewModel.updateDraftCard(updated)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Rotate Format",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isVerticalFacing) "Switch 16:9" else "Switch 9:16",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (cardMediumMode == 1) {
                // VIRTUAL CARD & QR CODE IMPORTER CONTROLS
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIRTUAL CARD / QR CODE IMPORTER",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Method Sub-Tabs: 0 = Live Camera QR Scanner, 1 = Web URL / vCard, 2 = Photo Gallery QR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Live Camera QR", "Web URL / vCard", "Photo Gallery").forEachIndexed { idx, label ->
                                val isSel = virtualScanMethod == idx
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clickable {
                                            if (idx == 0 && !hasCameraPermission) {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            } else if (idx == 2) {
                                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                            virtualScanMethod = idx
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (virtualScanMethod == 0) {
                            // LIVE CAMERA QR SCANNER VIEW
                            if (hasCameraPermission) {
                                QrCameraViewfinder(
                                    onQrDetected = { qrText ->
                                        if (qrText.contains("BEGIN:VCARD", ignoreCase = true)) {
                                            viewModel.parseAndApplyVCard(qrText) { c ->
                                                Toast.makeText(context, "Parsed vCard: ${c.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            virtualUrlInput = qrText
                                            viewModel.createVirtualCardFromUrl(qrText) { c ->
                                                Toast.makeText(context, "Loaded web card for ${c.company}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            } else {
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Grant Camera Permission for QR Scanning", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Web Link or vCard Input Field
                            OutlinedTextField(
                                value = virtualUrlInput,
                                onValueChange = { virtualUrlInput = it },
                                placeholder = { Text("https://stripe.com or vCard text...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                            if (!clip.isNullOrBlank()) {
                                                virtualUrlInput = clip
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = scanOutlinedColors()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (virtualUrlInput.contains("BEGIN:VCARD", ignoreCase = true)) {
                                            viewModel.parseAndApplyVCard(virtualUrlInput) { c ->
                                                Toast.makeText(context, "Parsed vCard: ${c.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            viewModel.createVirtualCardFromUrl(virtualUrlInput) { c ->
                                                Toast.makeText(context, "Loaded web card for ${c.company}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isOcrProcessing && virtualUrlInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.3f).height(44.dp)
                                ) {
                                    if (isOcrProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Fetch Site Info", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!hasCameraPermission) {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        } else {
                                            virtualScanMethod = 0
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Scan QR", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // PHYSICAL CARD CAMERA / PHOTO CONTROLS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanModeChip(
                        label = "Live Camera",
                        icon = Icons.Default.CameraAlt,
                        isSelected = inputMode == 0,
                        onClick = {
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                inputMode = 0
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ScanModeChip(
                        label = "Photo Picker",
                        icon = Icons.Default.PhotoLibrary,
                        isSelected = inputMode == 1,
                        onClick = {
                            inputMode = 1
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                val currentBitmap = if (activeSideTab == 0) frontBitmap else backBitmap

                if (inputMode == 0) {
                    if (hasCameraPermission) {
                        CameraViewfinder(
                            isBackSide = activeSideTab == 1,
                            isVertical = isVerticalFacing,
                            onImageCaptured = { bitmap ->
                                if (bitmap.height > bitmap.width) {
                                    isVerticalFacing = true
                                }
                                viewModel.processScanBitmap(bitmap, isBack = activeSideTab == 1)
                            }
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.586f),
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = PastelMint,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Camera access required",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelMint, contentColor = Color(0xFF022C22)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Grant Camera Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (currentBitmap != null) {
                    // Static Captured Preview with Edit / Rotate / Scale Controls
                    var previewScale by remember { mutableStateOf(1f) }
                    var previewOffsetX by remember { mutableStateOf(0f) }
                    var previewOffsetY by remember { mutableStateOf(0f) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (isVerticalFacing) 0.63f else 1.586f),
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = currentBitmap.asImageBitmap(),
                                    contentDescription = "Scanned bitmap",
                                    contentScale = if (isPreviewFit) androidx.compose.ui.layout.ContentScale.Fit else androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = previewScale
                                            scaleY = previewScale
                                            translationX = previewOffsetX
                                            translationY = previewOffsetY
                                        }
                                        .clip(RoundedCornerShape(18.dp))
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PastelMint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (activeSideTab == 0) "Front Scanned" else "Back Scanned",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Alignment & Crop Edit Toolbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val matrix = Matrix().apply { postRotate(90f) }
                                    val rotated = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
                                    viewModel.processScanBitmap(rotated, isBack = activeSideTab == 1)
                                },
                                modifier = Modifier.background(DarkSurfaceVariant, CircleShape).size(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            
                            androidx.compose.material3.Slider(
                                value = previewScale,
                                onValueChange = { previewScale = it },
                                valueRange = 0.5f..2.5f,
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = PastelMint,
                                    activeTrackColor = PastelMint
                                )
                            )
                            
                            Text("Zoom", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            OutlinedButton(
                                onClick = { 
                                    isPreviewFit = !isPreviewFit 
                                    val updated = (draftCard ?: currentCard).copy(
                                        scaleMode = if (isPreviewFit) "fit" else "crop"
                                    )
                                    viewModel.updateDraftCard(updated)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(if (isPreviewFit) "Fit" else "Crop", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Align", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            androidx.compose.material3.Slider(
                                value = previewOffsetX,
                                onValueChange = { previewOffsetX = it },
                                valueRange = -200f..200f,
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.Slider(
                                value = previewOffsetY,
                                onValueChange = { previewOffsetY = it },
                                valueRange = -200f..200f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (isOcrProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = PastelMint,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = ocrEngineStatus.ifBlank { "Extracting text with OCR..." },
                        color = PastelMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (ocrEngineStatus != "Ready") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PastelLavender,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ocrEngineStatus,
                            color = PastelLavender,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Real-Time 3D Card Preview
            Text(
                text = "CARD PREVIEW",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Card3DView(
                card = currentCard,
                isInteractive = true,
                showFlipButton = true,
                initialFlipped = activeSideTab == 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (currentCard.isVertical) 0.63f else 1.586f)
            )

            // Extracted Fields (Clean, no bloat)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "EXTRACTED DETAILS",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Multiple Persons Switcher Chip Row
                    if (currentCard.secondaryName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Contact:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PastelSand.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PastelSand),
                                modifier = Modifier.clickable {
                                    // Already primary
                                }
                            ) {
                                Text(
                                    text = currentCard.name.ifBlank { "Person 1" },
                                    color = PastelSand,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.clickable {
                                    // Swap to make secondary the primary name
                                    val tempName = currentCard.name
                                    val tempPhone = currentCard.phone
                                    viewModel.updateDraftCard(
                                        currentCard.copy(
                                            name = currentCard.secondaryName,
                                            phone = if (currentCard.secondaryPhone.isNotBlank()) currentCard.secondaryPhone else currentCard.phone,
                                            secondaryName = tempName,
                                            secondaryPhone = tempPhone
                                        )
                                    )
                                    Toast.makeText(context, "Switched to ${currentCard.secondaryName}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = currentCard.secondaryName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Category Selector
                    Text(
                        text = "Category",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf("Misc", "Technology", "Finance", "Design", "Healthcare", "Legal", "Other")
                        items(categories) { cat ->
                            val isSel = currentCard.category.equals(cat, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else DarkBorder),
                                modifier = Modifier.clickable {
                                    viewModel.updateDraftCard(currentCard.copy(category = cat))
                                }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = currentCard.name,
                        onValueChange = { viewModel.updateDraftCard(currentCard.copy(name = it)) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = scanOutlinedColors()
                    )

                    OutlinedTextField(
                        value = currentCard.title,
                        onValueChange = { viewModel.updateDraftCard(currentCard.copy(title = it)) },
                        label = { Text("Title / Role") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = scanOutlinedColors()
                    )

                    OutlinedTextField(
                        value = currentCard.company,
                        onValueChange = { viewModel.updateDraftCard(currentCard.copy(company = it)) },
                        label = { Text("Company") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = scanOutlinedColors()
                    )

                    // Primary Mobile Phone
                    OutlinedTextField(
                        value = currentCard.phone,
                        onValueChange = { viewModel.updateDraftCard(currentCard.copy(phone = it)) },
                        label = { Text("Mobile Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = scanOutlinedColors()
                    )

                    // Secondary Mobile Phone (Allotted slot if detected or alternate number)
                    if (currentCard.secondaryPhone.isNotBlank()) {
                        OutlinedTextField(
                            value = currentCard.secondaryPhone,
                            onValueChange = { viewModel.updateDraftCard(currentCard.copy(secondaryPhone = it)) },
                            label = { Text("Secondary Mobile") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = scanOutlinedColors()
                        )
                    }

                    // Telephone / Landline Row (Distinct slot if detected)
                    if (currentCard.telephone.isNotBlank()) {
                        OutlinedTextField(
                            value = currentCard.telephone,
                            onValueChange = { viewModel.updateDraftCard(currentCard.copy(telephone = it)) },
                            label = { Text("Telephone (Landline)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = scanOutlinedColors()
                        )
                    }

                    OutlinedTextField(
                        value = currentCard.email,
                        onValueChange = { viewModel.updateDraftCard(currentCard.copy(email = it)) },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = scanOutlinedColors()
                    )

                    OutlinedTextField(
                        value = currentCard.website,
                        onValueChange = { viewModel.updateDraftCard(currentCard.copy(website = it)) },
                        label = { Text("Website") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = scanOutlinedColors()
                    )

                    // Address with dedicated Google Maps button on its side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentCard.address,
                            onValueChange = { viewModel.updateDraftCard(currentCard.copy(address = it)) },
                            label = { Text("Address") },
                            modifier = Modifier.weight(1f),
                            colors = scanOutlinedColors()
                        )
                        if (currentCard.address.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable {
                                        com.example.data.contacts.GoogleContactsHelper.openAddressInMaps(context, currentCard.address)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Open in Google Maps",
                                        tint = PastelSand,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = currentCard.notes,
                        onValueChange = { viewModel.updateDraftCard(currentCard.copy(notes = it)) },
                        label = { Text("Notes / Offerings") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = scanOutlinedColors()
                    )
                }
            }

            // Save Card Action
            // Save Card Action
            Button(
                onClick = {
                    val finalName = currentCard.name.ifBlank { "Card #${System.currentTimeMillis() % 1000}" }
                    val isVirt = cardMediumMode == 1 || currentCard.isVirtual
                    val finalTags = currentCard.tags
                    val finalIsVertical = if (frontBitmap != null) {
                        frontBitmap!!.height > frontBitmap!!.width
                    } else {
                        isVerticalFacing || currentCard.isVertical
                    }
                    val finalCard = currentCard.copy(
                        name = finalName,
                        isVirtual = isVirt,
                        isVertical = finalIsVertical,
                        scaleMode = if (isPreviewFit) "fit" else "crop",
                        tags = finalTags,
                        websiteUrl = if (isVirt && currentCard.websiteUrl.isBlank()) virtualUrlInput else currentCard.websiteUrl
                    )
                    viewModel.saveDraftCard(finalCard) {
                        Toast.makeText(context, "Card saved to wallet", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_scanned_card"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PastelMint,
                    contentColor = Color(0xFF022C22)
                )
            ) {
                Text(
                    text = "Save to Wallet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Duplicate Card Alert Dialog
        if (duplicateDetection != null) {
            val dup = duplicateDetection!!
            AlertDialog(
                onDismissRequest = { viewModel.dismissDuplicatePrompt() },
                title = {
                    Text("Duplicate Card Detected", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Text(
                        "A contact with ${dup.matchReason} is already in your wallet (${dup.existingCard.name} at ${dup.existingCard.company}).\n\nWould you like to update the existing contact with new details or save as a duplicate?",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateExistingWithDraft(dup.existingCard, dup.incomingCard) {
                                Toast.makeText(context, "Updated ${dup.existingCard.name} with new details", Toast.LENGTH_SHORT).show()
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelMint, contentColor = Color(0xFF022C22))
                    ) {
                        Text("Update Existing", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.dismissDuplicatePrompt() }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                        TextButton(
                            onClick = {
                                viewModel.saveDraftCard(dup.incomingCard, force = true) {
                                    Toast.makeText(context, "Saved to wallet", Toast.LENGTH_SHORT).show()
                                    onSaved()
                                }
                            }
                        ) {
                            Text("Keep Both", color = PastelLavender, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

/**
 * CameraX Live Viewfinder with Shutter Trigger
 */
@Composable
private fun CameraViewfinder(
    isBackSide: Boolean,
    isVertical: Boolean = false,
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var zoomRatio by remember { mutableStateOf(1.0f) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (isVertical) 0.63f else 1.586f),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            // Camera bind fallback
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder Guide Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )

            // Zoom Ratio Toggle Chips (1x / 1.5x / 2x / 3x)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x", 3.0f to "3x").forEach { (ratio, label) ->
                    val isSel = zoomRatio == ratio
                    Surface(
                        shape = CircleShape,
                        color = if (isSel) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable {
                                zoomRatio = ratio
                                cameraControl?.setZoomRatio(ratio)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Shutter Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .size(54.dp)
                    .clickable(enabled = !isCapturing) {
                        val capture = imageCapture ?: return@clickable
                        isCapturing = true
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                    val bitmap = imageProxy.toBitmap()
                                    imageProxy.close()

                                    val rotatedBitmap = if (rotation != 0) {
                                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                    } else {
                                        bitmap
                                    }

                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                        onImageCaptured(rotatedBitmap)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                        Toast.makeText(context, "Capture error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    .testTag("btn_camera_shutter")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Capture",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Top side label
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 10.dp, start = 10.dp)
            ) {
                Text(
                    text = if (isBackSide) "Scan Back" else "Scan Front",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ScanModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun scanOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurfaceVariant,
    focusedBorderColor = PastelMint,
    unfocusedBorderColor = DarkBorder,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

@Composable
private fun QrCameraViewfinder(
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDetected by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val scanner = BarcodeScanning.getClient()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isDetected) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrBlank() && !isDetected) {
                                                isDetected = true
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    onQrDetected(rawValue)
                                                }
                                                break
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("QrScanner", "Failed to bind camera", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .border(1.5.dp, PastelLavender.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Point camera at vCard / Website QR",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
