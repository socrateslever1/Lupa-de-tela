package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MagnifierMode
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.HighContrastYellow
import com.example.ui.theme.LensCyan
import com.example.ui.theme.LensCyanBright
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.HapticFeedbackHelper

enum class AppTab(val title: String, val icon: ImageVector) {
    LUPA_TELA("Lente de Tela", Icons.Default.Search),
    CONTROLE_FLUTUANTE("Controle Flutuante", Icons.Default.Layers),
    ACESSIBILIDADE("Acessibilidade", Icons.Default.Accessibility)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticHelper = remember { HapticFeedbackHelper(context) }

    var currentTab by remember { mutableStateOf(AppTab.LUPA_TELA) }
    var zoomLevel by remember { mutableFloatStateOf(2.5f) }
    val maxZoom = 10.0f
    var activeMode by remember { mutableStateOf(MagnifierMode.NORMAL) }
    var lensShape by remember { mutableStateOf(LensShape.SQUARE) }
    var lensPositionOffset by remember { mutableStateOf(Offset(80f, 160f)) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepNavyBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkNavySurface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(LensCyan)
                            .border(1.5.dp, LensCyanBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Ícone de Ampliação",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LENTE DE AUMENTO",
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Ampliação visual digital e acessibilidade",
                            color = LensCyanBright,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x2510B981))
                            .border(1.dp, GreenSuccess.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Acessibilidade Ativa",
                            color = GreenSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            showInfoDialog = true
                            hapticHelper.performStepClick(hapticFeedback)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Informações",
                            tint = TextSecondaryLight
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkNavySurface,
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentTab = tab
                            hapticHelper.performStepClick(hapticFeedback)
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = LensCyanBright,
                            indicatorColor = LensCyan,
                            unselectedIconColor = TextSecondaryLight,
                            unselectedTextColor = TextSecondaryLight
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContent"
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.LUPA_TELA -> {
                            InspectionSheetView(
                                activeMode = activeMode,
                                zoomLevel = zoomLevel,
                                onZoomChange = { newZoom ->
                                    zoomLevel = newZoom
                                    hapticHelper.onZoomChanged(newZoom, maxZoom, hapticFeedback)
                                },
                                maxZoom = maxZoom,
                                lensShape = lensShape,
                                onLensShapeChange = { newShape ->
                                    lensShape = newShape
                                },
                                lensPositionOffset = lensPositionOffset,
                                onLensPositionChange = { newOffset ->
                                    lensPositionOffset = newOffset
                                }
                            )
                        }
                        AppTab.CONTROLE_FLUTUANTE -> {
                            FloatingHandleSimulationView(
                                activeMode = activeMode,
                                onModeChange = { activeMode = it }
                            )
                        }
                        AppTab.ACESSIBILIDADE -> {
                            AccessibilityGuideView()
                        }
                    }
                }
            }

            if (currentTab == AppTab.LUPA_TELA) {
                ControlsOverlay(
                    zoomLevel = zoomLevel,
                    onZoomChange = { newZoom ->
                        zoomLevel = newZoom
                        hapticHelper.onZoomChanged(newZoom, maxZoom, hapticFeedback)
                    },
                    maxZoom = maxZoom,
                    activeMode = activeMode,
                    onModeChange = { newMode ->
                        activeMode = newMode
                        hapticHelper.performStepClick(hapticFeedback)
                    },
                    lensShape = lensShape,
                    onLensShapeChange = { newShape ->
                        lensShape = newShape
                        hapticHelper.performStepClick(hapticFeedback)
                    },
                    onRecenterLens = {
                        lensPositionOffset = Offset(80f, 160f)
                        zoomLevel = 2.5f
                        hapticHelper.performStepClick(hapticFeedback)
                    },
                    onHapticTrigger = {
                        hapticHelper.performStepClick(hapticFeedback)
                    }
                )
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LensCyan)
                ) {
                    Text("Entendido")
                }
            },
            containerColor = DarkNavySurface,
            title = {
                Text(
                    text = "Como Usar a Lente de Aumento",
                    color = TextPrimaryLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "• Lente na Tela: Deslize a janela de foco quadrada, redonda ou retangular sobre qualquer texto. O conteúdo sob a lente é ampliado imediatamente em alta nitidez.",
                        color = TextSecondaryLight,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• Ajuste de Zoom: Use o controle deslizante ou botões + e - para regular a escala contínua de 1.0x até 10.0x.",
                        color = TextSecondaryLight,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• Resposta Tátil: Ao atingir 10.0x (escala máxima), o aparelho vibra para fornecer confirmação sensorial.",
                        color = HighContrastYellow,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• Controle Flutuante: Na aba 'Controle Flutuante', ative a aba lateral para sobrepor a lente a outros aplicativos e ao sistema operacional.",
                        color = LensCyanBright,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        )
    }
}
