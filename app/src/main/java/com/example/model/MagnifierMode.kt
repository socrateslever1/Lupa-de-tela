package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector

enum class MagnifierMode(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeLabel: String
) {
    NORMAL(
        title = "Cores Naturais",
        description = "Visualização ótica padrão sem filtros",
        icon = Icons.Default.Visibility,
        badgeLabel = "1x NAT"
    ),
    HIGH_CONTRAST(
        title = "Alto Contraste",
        description = "Aumenta o contraste para leitura de textos opacos ou desbotados",
        icon = Icons.Default.Contrast,
        badgeLabel = "CONTRASTE"
    ),
    INVERTED(
        title = "Invertido / Escuro",
        description = "Texto branco sobre fundo preto para eliminar reflexos de luz",
        icon = Icons.Default.InvertColors,
        badgeLabel = "INVERT"
    ),
    YELLOW_ON_BLACK(
        title = "Amarelo em Preto",
        description = "Modo de alta acessibilidade para manuais e bulas farmacêuticas",
        icon = Icons.Default.DarkMode,
        badgeLabel = "AMAR/PTO"
    ),
    MONOCHROME(
        title = "P&B Nítido",
        description = "Elimina cores e foca no contorno dos caracteres tipográficos",
        icon = Icons.Default.BrightnessMedium,
        badgeLabel = "P&B"
    ),
    WARM_SEPIA(
        title = "Luz Quente / Sépia",
        description = "Filtro anti-luz azul para leitura confortável e prolongada",
        icon = Icons.Default.WbSunny,
        badgeLabel = "SÉPIA"
    );

    fun getColorMatrix(): ColorMatrix? {
        return when (this) {
            NORMAL -> null
            HIGH_CONTRAST -> {
                // High contrast matrix (scale > 1 and negative bias)
                val c = 1.6f
                val t = -0.3f * 255f
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            INVERTED -> {
                // Invert colors matrix
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            YELLOW_ON_BLACK -> {
                // Luminance calculation then map to bright yellow (R=1, G=0.92, B=0.1) on black
                // Gray = 0.299R + 0.587G + 0.114B
                // Inverted or threshold luminance to bright yellow
                ColorMatrix(
                    floatArrayOf(
                        0.35f, 0.65f, 0.15f, 0f, 10f,
                        0.32f, 0.60f, 0.12f, 0f, 5f,
                        0.05f, 0.05f, 0.02f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            MONOCHROME -> {
                // Desaturate grayscale
                val matrix = ColorMatrix()
                matrix.setToSaturation(0f)
                matrix
            }
            WARM_SEPIA -> {
                // Warm sepia tone
                ColorMatrix(
                    floatArrayOf(
                        0.393f * 1.2f, 0.769f * 1.1f, 0.189f, 0f, 20f,
                        0.349f * 1.1f, 0.686f, 0.168f, 0f, 15f,
                        0.272f * 0.8f, 0.534f * 0.8f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }
    }

    fun getColorFilter(): ColorFilter? {
        val matrix = getColorMatrix() ?: return null
        return ColorFilter.colorMatrix(matrix)
    }
}
