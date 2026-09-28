package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.HighContrastYellow
import com.example.ui.theme.LensCyan
import com.example.ui.theme.LensCyanBright
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.HapticFeedbackHelper
import com.example.util.PermissionHelper

@Composable
fun AccessibilityGuideView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticHelper = remember { HapticFeedbackHelper(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // System Accessibility Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LensCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LensCyan.copy(alpha = 0.2f))
                            .border(1.5.dp, LensCyanBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = LensCyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Acessibilidade do Sistema",
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Ajustes globais de visão e escala",
                            color = LensCyanBright,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "O sistema operacional Android disponibiliza ferramentas complementares de ampliação de tela, escala de exibição e tamanho de texto para facilitar a leitura em todas as telas.",
                    color = TextSecondaryLight,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            hapticHelper.performStepClick(hapticFeedback)
                            PermissionHelper.openAccessibilitySettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LensCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Acessibilidade",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            hapticHelper.performStepClick(hapticFeedback)
                            PermissionHelper.openDisplaySettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = LensCyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fonte e Tela",
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "RECURSOS DISPONÍVEIS",
            color = LensCyanBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        FeatureItemCard(
            icon = Icons.Default.Search,
            iconTint = LensCyanBright,
            title = "Lente de Aumento Ajustável",
            subtitle = "Janela de foco móvel nos formatos quadrado, circular ou retangular, permitindo ampliar qualquer detalhe ou parágrafo com precisão óptica."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureItemCard(
            icon = Icons.Default.Layers,
            iconTint = HighContrastYellow,
            title = "Aba Lateral e Janela Flutuante",
            subtitle = "Controle discreto ancorado na borda da tela com menu rápido para ativar o zoom, alterar a escala e alternar entre janela móvel ou tela inteira."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureItemCard(
            icon = Icons.Default.TouchApp,
            iconTint = LensCyanBright,
            title = "Controle Contínuo de Escala",
            subtitle = "Ampliação ajustável de 1.0x a 10.0x através de botões graduais, controle deslizante ou gestos de pinça na tela."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureItemCard(
            icon = Icons.Default.Vibration,
            iconTint = AmberAccent,
            title = "Confirmação Sensorial e Tátil",
            subtitle = "Resposta tátil e vibração imediata ao atingir o limite máximo de ampliação para garantir retorno claro ao usuário."
        )
    }
}

@Composable
private fun FeatureItemCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = TextPrimaryLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSecondaryLight,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
