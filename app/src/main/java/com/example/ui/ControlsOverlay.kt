package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MagnifierMode
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.HighContrastYellow
import com.example.ui.theme.LensCyan
import com.example.ui.theme.LensCyanBright
import com.example.ui.theme.LensCyanDark
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

enum class LensShape(val title: String, val shortLabel: String, val icon: ImageVector) {
    SQUARE("Lupa Quadrada", "Quadrada", Icons.Default.CropSquare),
    CIRCULAR("Lupa Redonda", "Redonda", Icons.Default.Lens),
    RECTANGULAR("Quadro Leitura", "Retangular", Icons.Default.CropSquare),
    FULL_SCREEN("Tela Inteira", "Tela Cheia", Icons.Default.Fullscreen)
}

@Composable
fun ControlsOverlay(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    maxZoom: Float,
    activeMode: MagnifierMode,
    onModeChange: (MagnifierMode) -> Unit,
    lensShape: LensShape,
    onLensShapeChange: (LensShape) -> Unit,
    onRecenterLens: () -> Unit,
    onHapticTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAtMaxZoom = zoomLevel >= (maxZoom - 0.05f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LensCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Row 1: Zoom Readout, Max Zoom Indicator, Lens Shapes & Recenter Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Zoom Factor Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAtMaxZoom) AmberAccent else LensCyanDark)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", zoomLevel)}x",
                            color = if (isAtMaxZoom) Color.Black else TextPrimaryLight,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isAtMaxZoom) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33F59E0B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MÁXIMO",
                                color = AmberAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Recenter Lens Button
                IconButton(
                    onClick = {
                        onRecenterLens()
                        onHapticTrigger()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkNavySurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Recentralizar Lupa",
                        tint = LensCyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated Shape Selection: Quadrada, Redonda, Retangular, Tela Cheia
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LensShape.entries.forEach { shape ->
                    val isSelected = shape == lensShape
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) LensCyan else DarkNavySurface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) LensCyanBright else Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onLensShapeChange(shape)
                                onHapticTrigger()
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = shape.icon,
                                contentDescription = shape.title,
                                tint = if (isSelected) Color.White else TextSecondaryLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = shape.shortLabel,
                                color = if (isSelected) Color.White else TextSecondaryLight,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Precision Zoom Slider with +/- buttons and quick presets
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        val newZ = (zoomLevel - 0.5f).coerceAtLeast(1.0f)
                        onZoomChange(newZ)
                        onHapticTrigger()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Diminuir",
                        tint = TextPrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Slider(
                    value = zoomLevel,
                    onValueChange = { onZoomChange(it) },
                    valueRange = 1.0f..maxZoom,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = if (isAtMaxZoom) HighContrastYellow else LensCyanBright,
                        activeTrackColor = if (isAtMaxZoom) AmberAccent else LensCyanBright,
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                IconButton(
                    onClick = {
                        val newZ = (zoomLevel + 0.5f).coerceAtMost(maxZoom)
                        onZoomChange(newZ)
                        onHapticTrigger()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar",
                        tint = TextPrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Quick Preset Zoom stops: 1x, 2.5x, 5x, 10x MAX
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                val presets = listOf(1.0f, 2.5f, 5.0f, maxZoom)
                presets.forEach { preset ->
                    val isSelected = Math.abs(zoomLevel - preset) < 0.2f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) LensCyan.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable {
                                onZoomChange(preset)
                                onHapticTrigger()
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (preset == maxZoom) "10x MÁX" else "${preset}x",
                            color = if (isSelected) LensCyanBright else TextSecondaryLight,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Custom View Modes Carousel (Alternância Rápida entre Modos Personalizados)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MODOS DE VISUALIZAÇÃO",
                    color = LensCyanBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = activeMode.title,
                    color = HighContrastYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MagnifierMode.entries.forEach { mode ->
                    val isSelected = mode == activeMode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onModeChange(mode)
                            onHapticTrigger()
                        },
                        label = {
                            Text(
                                text = mode.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.title,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LensCyan,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = DarkNavySurface,
                            labelColor = TextSecondaryLight,
                            iconColor = TextSecondaryLight
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) LensCyanBright else Color(0xFF334155)
                        )
                    )
                }
            }

            // Mode hint banner
            Text(
                text = activeMode.description,
                color = TextSecondaryLight,
                fontSize = 10.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
