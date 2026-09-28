package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InspectionSample
import com.example.model.InspectionSamples
import com.example.model.MagnifierMode
import com.example.model.SampleSection
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.HighContrastYellow
import com.example.ui.theme.LensCyan
import com.example.ui.theme.LensCyanBright
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.HapticFeedbackHelper
import kotlin.math.roundToInt

@Composable
fun InspectionSheetView(
    activeMode: MagnifierMode,
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    maxZoom: Float,
    lensShape: LensShape,
    onLensShapeChange: (LensShape) -> Unit,
    lensPositionOffset: Offset,
    onLensPositionChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticHelper = remember { HapticFeedbackHelper(context) }

    var selectedSample by remember { mutableStateOf(InspectionSamples.samples.first()) }
    var customTextSample by remember { mutableStateOf<InspectionSample?>(null) }
    var showCustomTextDialog by remember { mutableStateOf(false) }
    var inputCustomText by remember { mutableStateOf("") }
    var inputCustomTitle by remember { mutableStateOf("Meu Documento / Contrato") }

    // Full screen pan offset when in FULL_SCREEN mode
    var fullScreenPanOffset by remember { mutableStateOf(Offset.Zero) }

    // Dynamic Pull-to-Resize lens dimensions
    var customLensWidth by remember { mutableFloatStateOf(240f) }
    var customLensHeight by remember { mutableFloatStateOf(200f) }

    // Dynamic color styling based on active custom view mode
    val (bgColor, textColor, headerColor, accentBorder) = when (activeMode) {
        MagnifierMode.NORMAL -> Quad(Color(0xFF0F172A), TextPrimaryLight, LensCyanBright, Color(0xFF334155))
        MagnifierMode.HIGH_CONTRAST -> Quad(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF38BDF8), Color(0xFFFFFFFF))
        MagnifierMode.INVERTED -> Quad(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFFDE68A), Color(0xFF64748B))
        MagnifierMode.YELLOW_ON_BLACK -> Quad(Color(0xFF050505), HighContrastYellow, HighContrastYellow, HighContrastYellow)
        MagnifierMode.MONOCHROME -> Quad(Color(0xFF111111), Color(0xFFEEEEEE), Color(0xFFCCCCCC), Color(0xFF444444))
        MagnifierMode.WARM_SEPIA -> Quad(Color(0xFF241D17), Color(0xFFFEEBC8), Color(0xFFF6AD55), Color(0xFF7B341E))
    }

    val activeSample = customTextSample?.takeIf { selectedSample.id == "custom" } ?: selectedSample

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBackground)
    ) {
        // Document Category Switcher (Pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InspectionSamples.samples.forEach { sample ->
                val isSelected = sample.id == selectedSample.id
                AssistChip(
                    onClick = {
                        selectedSample = sample
                        fullScreenPanOffset = Offset.Zero
                        hapticHelper.performStepClick(hapticFeedback)
                    },
                    label = {
                        Text(
                            text = sample.category,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = sample.icon,
                            contentDescription = sample.title,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) LensCyanBright else TextSecondaryLight
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) DarkNavySurface else DeepNavyBackground,
                        labelColor = if (isSelected) LensCyanBright else TextSecondaryLight
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = if (isSelected) LensCyanBright else Color(0xFF334155)
                    )
                )
            }

            // Custom user text button
            AssistChip(
                onClick = {
                    showCustomTextDialog = true
                    hapticHelper.performStepClick(hapticFeedback)
                },
                label = {
                    Text(
                        text = if (customTextSample != null) "Texto Próprio" else "+ Inserir Texto",
                        fontSize = 11.sp,
                        fontWeight = if (selectedSample.id == "custom") FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar texto",
                        modifier = Modifier.size(16.dp),
                        tint = if (selectedSample.id == "custom") LensCyanBright else TextSecondaryLight
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedSample.id == "custom") DarkNavySurface else DeepNavyBackground,
                    labelColor = if (selectedSample.id == "custom") LensCyanBright else TextSecondaryLight
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = if (selectedSample.id == "custom") LensCyanBright else Color(0xFF334155)
                )
            )
        }

        // Main Document Viewport with Internal Optical Loupe
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, accentBorder, RoundedCornerShape(16.dp))
                .background(bgColor)
                .pointerInput(lensShape) {
                    if (lensShape == LensShape.FULL_SCREEN) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomLevel * zoom).coerceIn(1.0f, maxZoom)
                            if (newZoom != zoomLevel) {
                                onZoomChange(newZoom)
                                hapticHelper.onZoomChanged(newZoom, maxZoom, hapticFeedback)
                            }
                            fullScreenPanOffset += pan
                        }
                    }
                }
        ) {
            val viewportWidthPx = constraints.maxWidth.toFloat()
            val viewportHeightPx = constraints.maxHeight.toFloat()

            // 1. Underlying document canvas
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (lensShape == LensShape.FULL_SCREEN) {
                            Modifier.graphicsLayer {
                                scaleX = zoomLevel
                                scaleY = zoomLevel
                                translationX = fullScreenPanOffset.x
                                translationY = fullScreenPanOffset.y
                            }
                        } else {
                            Modifier.verticalScroll(scrollState)
                        }
                    )
                    .padding(18.dp)
            ) {
                // Header of document
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = activeSample.icon,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeSample.title.uppercase(),
                        color = headerColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "• ${activeSample.hint}",
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Sections with realistic micro text
                activeSample.sections.forEach { sec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = bgColor.copy(alpha = 0.75f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentBorder.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = sec.header,
                                color = headerColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (sec.isMicroText) 9.sp else 11.sp,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sec.text,
                                color = textColor,
                                fontSize = if (sec.isMicroText) 8.sp else 10.sp,
                                lineHeight = if (sec.isMicroText) 11.sp else 14.sp,
                                fontFamily = if (sec.isMicroText) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }

            // 2. INTERNAL OPTICAL LOUPE (Lente de Aumento Óptica Móvel sobre a tela)
            if (lensShape != LensShape.FULL_SCREEN) {
                val lensWidthDp = customLensWidth.dp
                val lensHeightDp = customLensHeight.dp
                val lensClipShape = RoundedCornerShape(16.dp)

                val lensWidthPx = with(density) { lensWidthDp.toPx() }
                val lensHeightPx = with(density) { lensHeightDp.toPx() }

                val currentLensX = lensPositionOffset.x.coerceIn(0f, (viewportWidthPx - lensWidthPx).coerceAtLeast(0f))
                val currentLensY = lensPositionOffset.y.coerceIn(0f, (viewportHeightPx - lensHeightPx).coerceAtLeast(0f))

                Box(
                    modifier = Modifier
                        .offset { IntOffset(currentLensX.roundToInt(), currentLensY.roundToInt()) }
                        .size(width = lensWidthDp, height = lensHeightDp)
                        .clip(lensClipShape)
                        .background(Color(0xEE0A0F1D))
                        .border(
                            width = 3.dp,
                            color = if (zoomLevel >= maxZoom - 0.05f) AmberAccent else LensCyanBright,
                            shape = lensClipShape
                        )
                        .shadow(16.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onLensPositionChange(lensPositionOffset + dragAmount)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val newZoom = (zoomLevel * zoom).coerceIn(1.0f, maxZoom)
                                if (newZoom != zoomLevel) {
                                    onZoomChange(newZoom)
                                    hapticHelper.onZoomChanged(newZoom, maxZoom, hapticFeedback)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // Quick toggle between zoom levels
                                    val nextZoom = when {
                                        zoomLevel < 2.5f -> 2.5f
                                        zoomLevel < 5.0f -> 5.0f
                                        zoomLevel < 10.0f -> 10.0f
                                        else -> 1.0f
                                    }
                                    onZoomChange(nextZoom)
                                    hapticHelper.onZoomChanged(nextZoom, maxZoom, hapticFeedback)
                                }
                            )
                        }
                ) {
                    // Magnified Document Rendering inside the lens
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomLevel
                                scaleY = zoomLevel
                                // Focal center transformation relative to current lens position
                                translationX = -(currentLensX * (zoomLevel - 1f) * 0.45f)
                                translationY = -(currentLensY * (zoomLevel - 1f) * 0.45f)
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = activeSample.title.uppercase(),
                                color = headerColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            activeSample.sections.forEach { sec ->
                                Text(
                                    text = sec.header,
                                    color = headerColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = sec.text,
                                    color = textColor,
                                    fontSize = 8.5.sp,
                                    lineHeight = 12.sp,
                                    fontFamily = if (sec.isMicroText) FontFamily.Monospace else FontFamily.Default,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                    }

                    // Optical Reticle & Precision Crosshair
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        if (lensShape == LensShape.CIRCULAR) {
                            // Circular reticle ring
                            drawCircle(
                                color = LensCyanBright.copy(alpha = 0.25f),
                                radius = 24.dp.toPx()
                            )
                        } else {
                            // Square corner targeting brackets for square lens
                            val boxHalf = 28.dp.toPx()
                            val cornerLen = 8.dp.toPx()
                            val stroke = 1.5.dp.toPx()
                            val bracketColor = LensCyanBright.copy(alpha = 0.5f)

                            // Top-left corner
                            drawLine(bracketColor, Offset(cx - boxHalf, cy - boxHalf), Offset(cx - boxHalf + cornerLen, cy - boxHalf), stroke)
                            drawLine(bracketColor, Offset(cx - boxHalf, cy - boxHalf), Offset(cx - boxHalf, cy - boxHalf + cornerLen), stroke)

                            // Top-right corner
                            drawLine(bracketColor, Offset(cx + boxHalf, cy - boxHalf), Offset(cx + boxHalf - cornerLen, cy - boxHalf), stroke)
                            drawLine(bracketColor, Offset(cx + boxHalf, cy - boxHalf), Offset(cx + boxHalf, cy - boxHalf + cornerLen), stroke)

                            // Bottom-left corner
                            drawLine(bracketColor, Offset(cx - boxHalf, cy + boxHalf), Offset(cx - boxHalf + cornerLen, cy + boxHalf), stroke)
                            drawLine(bracketColor, Offset(cx - boxHalf, cy + boxHalf), Offset(cx - boxHalf, cy + boxHalf - cornerLen), stroke)

                            // Bottom-right corner
                            drawLine(bracketColor, Offset(cx + boxHalf, cy + boxHalf), Offset(cx + boxHalf - cornerLen, cy + boxHalf), stroke)
                            drawLine(bracketColor, Offset(cx + boxHalf, cy + boxHalf), Offset(cx + boxHalf, cy + boxHalf + cornerLen), stroke)
                        }

                        // Precision Crosshair ticks in center
                        drawLine(
                            color = LensCyanBright.copy(alpha = 0.6f),
                            start = Offset(cx - 10.dp.toPx(), cy),
                            end = Offset(cx + 10.dp.toPx(), cy),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = LensCyanBright.copy(alpha = 0.6f),
                            start = Offset(cx, cy - 10.dp.toPx()),
                            end = Offset(cx, cy + 10.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Clean Header at Top of Lens
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(Color(0xCC0F172A))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = LensCyanBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lente • ${customLensWidth.roundToInt()}×${customLensHeight.roundToInt()}",
                                color = LensCyanBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Puxe o canto ⤡",
                            color = HighContrastYellow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Pull-to-Resize Corner Handle (Bottom-Right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(42.dp)
                            .clip(RoundedCornerShape(topStart = 14.dp, bottomEnd = 16.dp))
                            .background(Color(0xEE0F172A))
                            .border(1.5.dp, HighContrastYellow, RoundedCornerShape(topStart = 14.dp, bottomEnd = 16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    customLensWidth = (customLensWidth + dragAmount.x).coerceIn(160f, 380f)
                                    customLensHeight = (customLensHeight + dragAmount.y).coerceIn(130f, 500f)
                                    hapticHelper.performStepClick(hapticFeedback)
                                }
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Puxar para redimensionar a lente",
                            tint = HighContrastYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Floating Zoom Badge on Lens Bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xEE000000))
                            .border(1.dp, LensCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${String.format("%.1f", zoomLevel)}x",
                                color = if (zoomLevel >= maxZoom - 0.05f) HighContrastYellow else LensCyanBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (zoomLevel >= maxZoom - 0.05f) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Real-time Gestures Hint Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LensCyanBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lensShape != LensShape.FULL_SCREEN)
                            "Arraste a lente sobre o texto • Pinça para zoom"
                        else
                            "Faça pinça ou deslize a tela p/ navegar",
                        color = TextPrimaryLight,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    // Dialog to enter/paste custom text
    if (showCustomTextDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTextDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputCustomText.isNotBlank()) {
                            customTextSample = InspectionSample(
                                id = "custom",
                                title = inputCustomTitle.ifBlank { "Texto Personalizado" },
                                category = "Texto Próprio",
                                icon = Icons.Default.Edit,
                                hint = "Inspeção de texto inserido pelo usuário",
                                sections = listOf(
                                    SampleSection(
                                        header = "CONTEÚDO INSERIDO",
                                        text = inputCustomText,
                                        isMicroText = true
                                    )
                                )
                            )
                            selectedSample = customTextSample!!
                        }
                        showCustomTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LensCyan)
                ) {
                    Text("Inspecionar na Lupa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTextDialog = false }) {
                    Text("Cancelar", color = TextSecondaryLight)
                }
            },
            containerColor = DarkNavySurface,
            title = {
                Text("Inserir Texto ou Letras Miúdas", color = TextPrimaryLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Cole aqui termos de serviço, contratos ou bulas de medicamentos para inspecionar com a lente de aumento interna.",
                        color = TextSecondaryLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = inputCustomTitle,
                        onValueChange = { inputCustomTitle = it },
                        label = { Text("Título do Documento") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LensCyanBright,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimaryLight,
                            unfocusedTextColor = TextPrimaryLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputCustomText,
                        onValueChange = { inputCustomText = it },
                        label = { Text("Cole o texto aqui") },
                        placeholder = { Text("Ex: Cláusula 5.2 - A garantia legal de 90 dias...") },
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LensCyanBright,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimaryLight,
                            unfocusedTextColor = TextPrimaryLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
