package com.example.imagepicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.permissions.PermissionState

@Composable
fun GalleryImagePickerScreen(
    viewModel: GalleryPermissionViewModel
) {
    val uiState = viewModel.uiState

    val galleryManager = rememberGalleryManager { sharedImage ->
        viewModel.onImageSelected(sharedImage)
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A0A0F),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F0F23)
                    ),
                    radius = 1200f
                )
            )
    ) {
        // Animated background
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Rotating shapes
            rotate(rotationAngle * 0.1f, pivot = Offset(canvasWidth * 0.2f, canvasHeight * 0.3f)) {
                drawRoundRect(
                    color = Color(0xFF6366F1).copy(alpha = 0.05f),
                    size = Size(300f, 300f),
                    topLeft = Offset(canvasWidth * 0.1f, canvasHeight * 0.2f),
                    cornerRadius = CornerRadius(50f, 50f)
                )
            }

            rotate(-rotationAngle * 0.15f, pivot = Offset(canvasWidth * 0.8f, canvasHeight * 0.7f)) {
                drawRoundRect(
                    color = Color(0xFF8B5CF6).copy(alpha = 0.04f),
                    size = Size(250f, 250f),
                    topLeft = Offset(canvasWidth * 0.7f, canvasHeight * 0.6f),
                    cornerRadius = CornerRadius(40f, 40f)
                )
            }

            // Pulsing circles
            drawCircle(
                color = Color(0xFF06B6D4).copy(alpha = pulseAlpha * 0.1f),
                radius = 150f + (pulseAlpha * 50f),
                center = Offset(canvasWidth * 0.9f, canvasHeight * 0.1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Header(pulseAlpha)

            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    ErrorCard(
                        error = error,
                        onDismiss = viewModel::clearError
                    )
                }
            }

            when (uiState.permissionState) {
                PermissionState.Granted -> {
                    if (uiState.selectedImageBitmap != null) {
                        ImageDisplay(
                            imageBitmap = uiState.selectedImageBitmap,
                            imageBytes = uiState.selectedImageBytes,
                            isLoading = uiState.isLoading,
                            onClearImage = viewModel::clearSelectedImage,
                            onSelectNewImage = { galleryManager.launch() }
                        )
                    } else {
                        ImagePickerCard(
                            isLoading = uiState.isLoading,
                            onPickImage = { galleryManager.launch() },
                            pulseAlpha = pulseAlpha
                        )
                    }
                }

                PermissionState.DeniedAlways -> {
                    PermissionDenied(
                        onOpenSettings = viewModel::openAppSettings
                    )
                }

                else -> {
                    PermissionRequest(
                        isLoading = uiState.isLoading,
                        onRequestPermission = viewModel::requestGalleryPermission
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(pulseAlpha: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = pulseAlpha),
                            Color(0xFF8B5CF6).copy(alpha = pulseAlpha),
                            Color(0xFF06B6D4).copy(alpha = pulseAlpha),
                            Color(0xFF6366F1).copy(alpha = pulseAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(2.dp)
                .background(
                    Color(0xFF0F0F23),
                    RoundedCornerShape(26.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GP",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF8B5CF6)
                                )
                            ),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "GALLERY",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFE2E8F0),
                        Color(0xFFCBD5E1)
                    )
                )
            )
        )

        Text(
            text = "PRO",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            ),
            color = Color(0xFF6366F1),
            modifier = Modifier.offset(y = (-2).dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Professional Image Management Suite",
            style = MaterialTheme.typography.bodyLarge.copy(
                letterSpacing = 1.sp
            ),
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color(0xFFEF4444).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onDismiss() }
                    .background(
                        Color(0xFF334155).copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun ImageDisplay(
    imageBitmap: ImageBitmap,
    imageBytes: ByteArray?,
    isLoading: Boolean,
    onClearImage: () -> Unit,
    onSelectNewImage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF6366F1)
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "SELECTED IMAGE",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            Color(0xFF6366F1),
                            RoundedCornerShape(
                                topStart = 20.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .align(Alignment.TopStart)
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            Color(0xFF8B5CF6),
                            RoundedCornerShape(
                                topEnd = 20.dp,
                                bottomStart = 8.dp
                            )
                        )
                        .align(Alignment.TopEnd)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            imageBytes?.let { bytes ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FILE SIZE",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${(bytes.size / 1024.0)} KB",
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "FORMAT",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "BITMAP",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF6366F1)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    Color(0xFF10B981),
                                    CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrimaryButton(
                    text = "CLEAR",
                    onClick = onClearImage,
                    enabled = !isLoading,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "NEW IMAGE",
                    onClick = onSelectNewImage,
                    enabled = !isLoading,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ImagePickerCard(
    isLoading: Boolean,
    onPickImage: () -> Unit,
    pulseAlpha: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulsing ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            Color(0xFF6366F1).copy(alpha = pulseAlpha * 0.1f),
                            CircleShape
                        )
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Color(0xFF8B5CF6).copy(alpha = pulseAlpha * 0.2f),
                            CircleShape
                        )
                )

                // border effect
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color(0xFF1E293B),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF6366F1).copy(alpha = pulseAlpha),
                                    Color.Transparent,
                                    Color(0xFF8B5CF6).copy(alpha = pulseAlpha),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "SELECT YOUR IMAGE",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Choose a stunning image from your gallery\nto showcase with our premium viewer",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(36.dp))

            PrimaryButton(
                text = if (isLoading) "LOADING..." else "PICK FROM GALLERY",
                onClick = onPickImage,
                enabled = !isLoading,
                isPrimary = true,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionDenied(
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Color(0xFFEF4444).copy(alpha = 0.1f),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        Color(0xFFEF4444).copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    style = TextStyle(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "PERMISSION REQUIRED",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gallery access was denied. Please enable it in settings to unlock the full experience.",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(36.dp))

            PrimaryButton(
                text = "OPEN SETTINGS",
                onClick = onOpenSettings,
                enabled = true,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionRequest(
    isLoading: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(80.dp)
                ) {
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, 0f)
                        lineTo(size.width, size.height * 0.3f)
                        lineTo(size.width, size.height * 0.7f)
                        lineTo(size.width * 0.5f, size.height)
                        lineTo(0f, size.height * 0.7f)
                        lineTo(0f, size.height * 0.3f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF06B6D4).copy(alpha = 0.2f)
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFF06B6D4),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Text(
                    text = "🛡",
                    style = TextStyle(fontSize = 32.sp),
                    color = Color(0xFF06B6D4)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "GALLERY ACCESS",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We need access to your gallery to provide you with the best image selection experience.",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(36.dp))

            PrimaryButton(
                text = if (isLoading) "REQUESTING..." else "GRANT PERMISSION",
                onClick = onRequestPermission,
                enabled = !isLoading,
                isPrimary = true,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = buttonColors,
        modifier = modifier
            .height(64.dp)
            .then(
                if (isPrimary) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = if (enabled) {
                                listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF8B5CF6),
                                    Color(0xFF06B6D4)
                                )
                            } else {
                                listOf(
                                    Color(0xFF374151),
                                    Color(0xFF4B5563)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier.background(
                        Color(0xFF0F172A).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    )
                        .border(
                            1.dp,
                            Color(0xFF334155).copy(alpha = 0.6f),
                            RoundedCornerShape(20.dp)
                        )
                }
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isPrimary) 12.dp else 4.dp,
            pressedElevation = if (isPrimary) 16.dp else 6.dp
        ),
        contentPadding = PaddingValues(horizontal = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White, CircleShape)
                            .align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = text,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White
            )
        }
    }
}