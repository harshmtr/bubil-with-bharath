package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.domain.models.SamplePresets
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleYellow
import com.example.ui.viewmodel.ScannerViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val stream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            viewModel.processImageUri(uri, bitmap)
            onNavigateToResult()
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf("TEXT") } // "TEXT" or "BARCODE"
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraExecutor: ExecutorService? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("scanner_screen")
    ) {
        // 1. Camera Viewfinder or Fallback
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(
                                    cameraExecutor ?: ContextCompat.getMainExecutor(ctx),
                                    viewModel.textRecognitionHelper.createAnalyzer(
                                        throttleIntervalMs = 400L,
                                        onTextRecognized = { visionText ->
                                            viewModel.onLiveTextRecognized(visionText.text)
                                        }
                                    )
                                )
                            }

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture,
                                imageAnalysis
                            )
                            camera.cameraControl.enableTorch(isFlashOn)
                        } catch (e: Exception) {
                            // Camera bind fallback
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Friendly permission fallback
            CameraPermissionFallback(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onUploadPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // 2. Dynamic Viewfinder Framing Overlay
        ViewfinderFramingOverlay(mode = scanMode)

        // 3. Top Action Bar: Back, Title, Torch, Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("scanner_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Mode Selector Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeButton(
                        title = "OCR Text",
                        icon = Icons.Default.TextFields,
                        isSelected = scanMode == "TEXT",
                        onClick = { scanMode = "TEXT" }
                    )
                    ModeButton(
                        title = "Barcode",
                        icon = Icons.Default.QrCodeScanner,
                        isSelected = scanMode == "BARCODE",
                        onClick = { scanMode = "BARCODE" }
                    )
                }
            }

            IconButton(
                onClick = { isFlashOn = !isFlashOn },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("flash_toggle_button")
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = if (isFlashOn) GoogleYellow else Color.White
                )
            }
        }

        // Live Real-Time OCR Stream Badge
        if (uiState.liveKeywordsSpotted.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GoogleGreen.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp)
                    .testTag("live_ocr_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live Stream OCR: ${uiState.liveKeywordsSpotted.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // 4. Bottom Controls: Presets + Capture Button + Gallery Upload
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Test Presets Carousel (in case camera is pointed at non-label)
            Text(
                text = "Or tap a sample packaging label to test:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SamplePresets.list) { preset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable {
                                viewModel.loadSamplePreset(preset)
                                onNavigateToResult()
                            }
                            .testTag("scanner_preset_${preset.title.take(5)}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (preset.expectedTrustScore >= 80) GoogleGreen else Color(0xFFD93025))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.title.take(20),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Shutter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload Photo
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("gallery_shutter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Upload Photo",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Primary Capture Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(GoogleBlue)
                        .clickable {
                            val capture = imageCapture
                            val executor = cameraExecutor
                            if (capture != null && executor != null) {
                                capture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            ContextCompat.getMainExecutor(context).execute {
                                                viewModel.processCapturedBitmap(bitmap)
                                                onNavigateToResult()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                // Fallback to demo preset if hardware camera capture fails
                                                SamplePresets.list.firstOrNull()?.let {
                                                    viewModel.loadSamplePreset(it)
                                                    onNavigateToResult()
                                                }
                                            }
                                        }
                                    }
                                )
                            } else {
                                // Direct test fallback
                                SamplePresets.list.firstOrNull()?.let {
                                    viewModel.loadSamplePreset(it)
                                    onNavigateToResult()
                                }
                            }
                        }
                        .testTag("capture_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Label",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Demo preset quick button
                IconButton(
                    onClick = {
                        SamplePresets.list.firstOrNull()?.let {
                            viewModel.loadSamplePreset(it)
                            onNavigateToResult()
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("quick_demo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Try Preset Demo",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) GoogleBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ViewfinderFramingOverlay(mode: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxWidth = width * 0.84f
        val boxHeight = if (mode == "BARCODE") height * 0.22f else height * 0.44f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2.3f

        // Draw darkened background around framing rect
        // Framing guidelines for Legal Metrology text (MRP, Expiry, Veg/Non-Veg symbol)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Corner accents
        val cornerLength = 32.dp.toPx()
        val cornerStroke = 5.dp.toPx()
        val accentColor = Color(0xFF1A73E8)

        // Top-left corner
        drawLine(accentColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
        drawLine(accentColor, Offset(left, top), Offset(left, top + cornerLength), cornerStroke)

        // Top-right corner
        drawLine(accentColor, Offset(left + boxWidth, top), Offset(left + boxWidth - cornerLength, top), cornerStroke)
        drawLine(accentColor, Offset(left + boxWidth, top), Offset(left + boxWidth, top + cornerLength), cornerStroke)

        // Bottom-left corner
        drawLine(accentColor, Offset(left, top + boxHeight), Offset(left + cornerLength, top + boxHeight), cornerStroke)
        drawLine(accentColor, Offset(left, top + boxHeight), Offset(left, top + boxHeight - cornerLength), cornerStroke)

        // Bottom-right corner
        drawLine(accentColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - cornerLength, top + boxHeight), cornerStroke)
        drawLine(accentColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + boxHeight - cornerLength), cornerStroke)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 180.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Text(
                text = "Align MRP, Expiry, FSSAI & Label inside frame",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onUploadPhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1C1E))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Camera Access Required",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant camera permission to scan product packages directly, or pick a photo from your gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
            ) {
                Text("Grant Camera Permission")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onUploadPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Text("Pick Package Photo from Gallery", color = Color.White)
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
