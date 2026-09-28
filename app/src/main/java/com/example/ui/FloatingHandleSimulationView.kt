package com.example.ui

import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MagnifierMode
import com.example.service.MagnifierOverlayService
import com.example.service.ScreenMagnifierAccessibilityService
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.HighContrastYellow
import com.example.ui.theme.LensCyan
import com.example.ui.theme.LensCyanBright
import com.example.ui.theme.LensCyanDark
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.HapticFeedbackHelper
import com.example.util.PermissionHelper
import kotlin.math.roundToInt

@Composable
fun FloatingHandleSimulationView(
    activeMode: MagnifierMode,
    onModeChange: (MagnifierMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticHelper = remember { HapticFeedbackHelper(context) }

    var isServiceRunning by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(ScreenMagnifierAccessibilityService.isAccessibilityServiceEnabled(context))
    }

    // Floating UI state
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isZoomActive by remember { mutableStateOf(true) }
    var isFullScreenMode by remember { mutableStateOf(false) }
    var isSquareLens by remember { mutableStateOf(true) }

    var handleOffsetY by remember { mutableFloatStateOf(240f) }
    var windowOffsetX by remember { mutableFloatStateOf(20f) }
    var windowOffsetY by remember { mutableFloatStateOf(170f) }
    var zoomLevel by remember { mutableFloatStateOf(2.5f) }
    val maxZoom = 10.0f
    val minZoom = 1.0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Service Toggle & Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LensCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(LensCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = LensCyanBright,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Controle Flutuante na Tela",
                                    color = TextPrimaryLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isServiceRunning) "Ativo • Aba na borda da tela" else "Toque no botão para ativar a aba",
                                    color = if (isServiceRunning) GreenSuccess else TextSecondaryLight,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { start ->
                                if (start) {
                                    if (PermissionHelper.hasOverlayPermission(context)) {
                                        val intent = Intent(context, MagnifierOverlayService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                        isServiceRunning = true
                                        hapticHelper.performStepClick(hapticFeedback)
                                    } else {
                                        PermissionHelper.openOverlaySettings(context)
                                    }
                                } else {
                                    val intent = Intent(context, MagnifierOverlayService::class.java)
                                    context.stopService(intent)
                                    isServiceRunning = false
                                    hapticHelper.performStepClick(hapticFeedback)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LensCyanBright,
                                checkedTrackColor = LensCyanDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shortcuts for Native Android Accessibility & Display Scale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                PermissionHelper.openAccessibilitySettings(context)
                                hapticHelper.performStepClick(hapticFeedback)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityEnabled) Color(0xFF065F46) else LensCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.SettingsAccessibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAccessibilityEnabled) "Lupa OS Ativa" else "Lupa no Sistema",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                PermissionHelper.openDisplaySettings(context)
                                hapticHelper.performStepClick(hapticFeedback)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                tint = LensCyanBright,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fonte e Tela",
                                color = TextPrimaryLight,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reference Content Area
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF23324E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TESTE DE AMPLIAÇÃO INTERNA",
                        color = LensCyanBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "TEXTO ORIGINAL SOB A TELA (FONTE 6pt):",
                                color = HighContrastYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "A Lente Digital amplia elementos e textos com nitidez contínua de 1.0x até 10.0x. Ao arrastar a janela de foco sobre esta caixa, o conteúdo no interior da lente é ampliado imediatamente.",
                                color = TextPrimaryLight,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Toque no botão 'LUPA' na borda esquerda para abrir o menu rápido.\n• A janela de foco abaixo exibe o texto ampliado em tempo real.",
                        color = TextSecondaryLight,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // ==========================================
        // 1. DOCK LATERAL / ABA LATERAL
        // ==========================================
        if (!isMenuExpanded) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, handleOffsetY.roundToInt()) }
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .background(Color(0xF20B132B))
                    .border(2.dp, LensCyanBright, RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .shadow(16.dp)
                    .clickable {
                        isMenuExpanded = true
                        hapticHelper.performStepClick(hapticFeedback)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            handleOffsetY = (handleOffsetY + dragAmount.y).coerceIn(80f, 1300f)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Expandir Menu de Ampliação",
                        tint = LensCyanBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "LUPA",
                        color = LensCyanBright,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // ==========================================
        // 2. MENU RÁPIDO DE CONTROLES
        // ==========================================
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(16, (handleOffsetY - 40f).roundToInt().coerceIn(60, 1000)) }
                    .width(310.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xF50F172A))
                    .border(2.dp, LensCyanBright, RoundedCornerShape(22.dp))
                    .shadow(24.dp)
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = LensCyanBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AMPLIAÇÃO",
                                color = LensCyanBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                isMenuExpanded = false
                                hapticHelper.performStepClick(hapticFeedback)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Ocultar", color = TextPrimaryLight, fontSize = 10.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Zoom toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Zoom da Tela",
                            color = TextPrimaryLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Switch(
                            checked = isZoomActive,
                            onCheckedChange = { active ->
                                isZoomActive = active
                                hapticHelper.performStepClick(hapticFeedback)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LensCyanBright,
                                checkedTrackColor = LensCyanDark
                            ),
                            modifier = Modifier.size(width = 46.dp, height = 28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scale controls (+ and -)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val newZoom = (zoomLevel - 0.5f).coerceAtLeast(minZoom)
                                zoomLevel = newZoom
                                hapticHelper.performStepClick(hapticFeedback)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Reduzir",
                                tint = TextPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${String.format("%.1f", zoomLevel)}x",
                                color = if (zoomLevel >= maxZoom - 0.05f) HighContrastYellow else LensCyanBright,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Nível de Aumento",
                                color = TextSecondaryLight,
                                fontSize = 9.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                val newZoom = (zoomLevel + 0.5f).coerceAtMost(maxZoom)
                                zoomLevel = newZoom
                                hapticHelper.onZoomChanged(newZoom, maxZoom, hapticFeedback)
                                if (newZoom < maxZoom) {
                                    hapticHelper.performStepClick(hapticFeedback)
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LensCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ampliar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Slider(
                        value = zoomLevel,
                        onValueChange = { newZoom ->
                            zoomLevel = newZoom
                            hapticHelper.onZoomChanged(newZoom, maxZoom, hapticFeedback)
                        },
                        valueRange = minZoom..maxZoom,
                        colors = SliderDefaults.colors(
                            thumbColor = if (zoomLevel >= maxZoom - 0.05f) HighContrastYellow else LensCyanBright,
                            activeTrackColor = LensCyanBright
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Mode: Janela vs Tela Cheia
                    Text(
                        text = "MODO DE LENTE",
                        color = LensCyanBright,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isFullScreenMode) LensCyan else Color(0xFF1E293B))
                                .clickable {
                                    isFullScreenMode = false
                                    hapticHelper.performStepClick(hapticFeedback)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CropSquare,
                                    contentDescription = null,
                                    tint = if (!isFullScreenMode) Color.White else TextSecondaryLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Janela",
                                    color = if (!isFullScreenMode) Color.White else TextSecondaryLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isFullScreenMode) LensCyan else Color(0xFF1E293B))
                                .clickable {
                                    isFullScreenMode = true
                                    hapticHelper.performStepClick(hapticFeedback)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    tint = if (isFullScreenMode) Color.White else TextSecondaryLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tela Cheia",
                                    color = if (isFullScreenMode) Color.White else TextSecondaryLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. JANELA DE FOCO AJUSTÁVEL (LENTE COM CONTEÚDO AMPLIADO)
        // ==========================================
        if (isZoomActive && !isFullScreenMode) {
            val windowWidth = if (isSquareLens) 250.dp else 310.dp
            val windowHeight = if (isSquareLens) 250.dp else 190.dp

            Box(
                modifier = Modifier
                    .offset { IntOffset(windowOffsetX.roundToInt(), windowOffsetY.roundToInt()) }
                    .size(width = windowWidth, height = windowHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF5080E1A))
                    .border(2.5.dp, LensCyanBright, RoundedCornerShape(20.dp))
                    .shadow(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with no text wrapping
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    windowOffsetX = (windowOffsetX + dragAmount.x).coerceIn(0f, 400f)
                                    windowOffsetY = (windowOffsetY + dragAmount.y).coerceIn(40f, 1200f)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = LensCyanBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lente",
                                color = LensCyanBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Toggle Format
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, HighContrastYellow, RoundedCornerShape(8.dp))
                                .clickable {
                                    isSquareLens = !isSquareLens
                                    hapticHelper.performStepClick(hapticFeedback)
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isSquareLens) "▢ 1:1" else "▭ 16:9",
                                color = HighContrastYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Optical Focus Viewport with REALLY AMPLIFIED text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF000000))
                            .border(1.dp, LensCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        // Dynamically magnified content under the lens
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomLevel
                                    scaleY = zoomLevel
                                    translationX = -(windowOffsetX * (zoomLevel - 1f) * 0.4f)
                                    translationY = -(windowOffsetY * (zoomLevel - 1f) * 0.4f)
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "CONTEÚDO DA TELA (AMPLIADO):",
                                    color = HighContrastYellow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "A Lente Digital amplia elementos e textos com nitidez contínua de 1.0x até 10.0x. O conteúdo renderizado sob a lente é exibido em tamanho ampliado com fidelidade visual.",
                                    color = TextPrimaryLight,
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Reticle lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawCircle(
                                color = LensCyanBright.copy(alpha = 0.35f),
                                radius = 28.dp.toPx()
                            )
                            drawLine(
                                color = LensCyanBright,
                                start = Offset(cx - 12.dp.toPx(), cy),
                                end = Offset(cx + 12.dp.toPx(), cy),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawLine(
                                color = LensCyanBright,
                                start = Offset(cx, cy - 12.dp.toPx()),
                                end = Offset(cx, cy + 12.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // Zoom Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xEE000000))
                                .border(1.dp, LensCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", zoomLevel)}x",
                                color = if (zoomLevel >= maxZoom - 0.05f) HighContrastYellow else LensCyanBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
