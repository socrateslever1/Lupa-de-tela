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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInFull
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

    // Floating UI state
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isZoomActive by remember { mutableStateOf(true) }

    // Dynamic Pull-to-Resize lens dimensions
    var lensWidth by remember { mutableFloatStateOf(270f) }
    var lensHeight by remember { mutableFloatStateOf(230f) }

    var handleOffsetY by remember { mutableFloatStateOf(260f) }
    var windowOffsetX by remember { mutableFloatStateOf(24f) }
    var windowOffsetY by remember { mutableFloatStateOf(160f) }
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
            // Simplified Service Control Card
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
                                    text = "Lente Flutuante na Tela",
                                    color = TextPrimaryLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isServiceRunning) "Ativo • Arraste e puxe a borda" else "Ative para usar sobre qualquer aplicativo",
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

                    // Simplified shortcut button
                    OutlinedButton(
                        onClick = {
                            PermissionHelper.openAccessibilitySettings(context)
                            hapticHelper.performStepClick(hapticFeedback)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsAccessibility,
                            contentDescription = null,
                            tint = HighContrastYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configurações de Acessibilidade do Android",
                            color = TextPrimaryLight,
                            fontSize = 11.5.sp
                        )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ÁREA DE TESTE",
                            color = LensCyanBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Puxe o canto ⤡ da lente",
                            color = HighContrastYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "TEXTO SOB A LENTE (FONTE REDUZIDA):",
                                color = HighContrastYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Composição: Cloridrato de magnésio 250mg, zinco quelatado 15mg. Posologia: 1 comprimido por via oral a cada 12 horas. Conservar em local fresco entre 15°C e 30°C. Lote: MG2026-X9. Fabricação: 08/2026. Validade: 08/2029.",
                                color = TextPrimaryLight,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Mova a lente arrastando o topo.\n• Puxe a borda inferior ou o canto amarelo ⤡ para deixar do tamanho exato que desejar.",
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
        // 2. MENU RÁPIDO SIMPLIFICADO
        // ==========================================
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(16, (handleOffsetY - 40f).roundToInt().coerceIn(60, 1000)) }
                    .width(280.dp)
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
                        Text(
                            text = "LUPA DE TELA",
                            color = LensCyanBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

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

                    // Controles de Escala (+ e -)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
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
                                .background(Color(0xFF334155))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Reduzir",
                                tint = TextPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${String.format("%.1f", zoomLevel)}x",
                            color = if (zoomLevel >= maxZoom - 0.05f) HighContrastYellow else LensCyanBright,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

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

                    // Ativar/Desativar Janela
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Janela Visível",
                            color = TextSecondaryLight,
                            fontSize = 12.sp
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
                }
            }
        }

        // ==========================================
        // 3. JANELA DE FOCO COM REDIMENSIONAMENTO DIRETO NA BORDA / CANTO
        // ==========================================
        if (isZoomActive) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(windowOffsetX.roundToInt(), windowOffsetY.roundToInt()) }
                    .size(width = lensWidth.dp, height = lensHeight.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF5080E1A))
                    .border(2.5.dp, LensCyanBright, RoundedCornerShape(20.dp))
                    .shadow(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar: Arraste para mover a lente
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lente",
                                color = LensCyanBright,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${lensWidth.roundToInt()}×${lensHeight.roundToInt()}",
                                color = TextSecondaryLight,
                                fontSize = 10.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar Lente",
                            tint = TextSecondaryLight,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    isZoomActive = false
                                    hapticHelper.performStepClick(hapticFeedback)
                                }
                        )
                    }

                    // Optical Focus Viewport com ampliação dinâmica
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF000000))
                            .border(1.dp, LensCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        // Conteúdo ampliado dinamicamente sob a lente
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomLevel
                                    scaleY = zoomLevel
                                    translationX = -(windowOffsetX * (zoomLevel - 1f) * 0.4f)
                                    translationY = -(windowOffsetY * (zoomLevel - 1f) * 0.4f)
                                }
                                .padding(10.dp)
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
                                    text = "Composição: Cloridrato de magnésio 250mg, zinco quelatado 15mg. Posologia: 1 comprimido por via oral a cada 12 horas. Conservar em local fresco entre 15°C e 30°C.",
                                    color = TextPrimaryLight,
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Retícula central
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawCircle(
                                color = LensCyanBright.copy(alpha = 0.3f),
                                radius = 24.dp.toPx()
                            )
                            drawLine(
                                color = LensCyanBright,
                                start = Offset(cx - 10.dp.toPx(), cy),
                                end = Offset(cx + 10.dp.toPx(), cy),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawLine(
                                color = LensCyanBright,
                                start = Offset(cx, cy - 10.dp.toPx()),
                                end = Offset(cx, cy + 10.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // Zoom Badge no rodapé
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // ==========================================
                // 4. ALÇA DE PUXAR NA BORDA / CANTO INFERIOR DIREITO
                // O usuário toca e puxa para redimensionar a lente
                // ==========================================
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(44.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomEnd = 18.dp))
                        .background(Color(0xEE0F172A))
                        .border(1.5.dp, HighContrastYellow, RoundedCornerShape(topStart = 14.dp, bottomEnd = 18.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                lensWidth = (lensWidth + dragAmount.x).coerceIn(160f, 380f)
                                lensHeight = (lensHeight + dragAmount.y).coerceIn(130f, 500f)
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
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Borda inferior (puxar para baixo/cima para ajustar a altura)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(12.dp)
                        .padding(horizontal = 46.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                lensHeight = (lensHeight + dragAmount.y).coerceIn(130f, 500f)
                            }
                        }
                )

                // Borda direita (puxar para os lados para ajustar a largura)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(12.dp)
                        .fillMaxSize()
                        .padding(vertical = 46.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                lensWidth = (lensWidth + dragAmount.x).coerceIn(160f, 380f)
                            }
                        }
                )
            }
        }
    }
}
