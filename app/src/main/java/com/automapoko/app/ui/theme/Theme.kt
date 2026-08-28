package com.automapoko.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta AutomaPoko — dark mode exclusivo
// Cor de destaque: laranja âmbar — remete a automações, energia, ação
// Fundo: cinza muito escuro, quase preto — confortável para uso noturno e no carro

private val AutomaPokoColorScheme = darkColorScheme(
    // Primária — laranja âmbar vibrante
    primary = Color(0xFFFFB300),
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFF3D2C00),
    onPrimaryContainer = Color(0xFFFFDC7A),

    // Secundária — âmbar acinzentado
    secondary = Color(0xFFD4AA50),
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = Color(0xFF2C2200),
    onSecondaryContainer = Color(0xFFF0CC70),

    // Terciária — teal sutil para contrapontos
    tertiary = Color(0xFF4FC3F7),
    onTertiary = Color(0xFF00131F),
    tertiaryContainer = Color(0xFF00344D),
    onTertiaryContainer = Color(0xFFB3E5FC),

    // Erro
    error = Color(0xFFFF5449),
    onError = Color(0xFF2D0001),
    errorContainer = Color(0xFF5C0004),
    onErrorContainer = Color(0xFFFFDAD5),

    // Superfícies — hierarquia clara de profundidade
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFE8E1D9),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFE8E1D9),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFADA49A),
    surfaceTint = Color(0xFFFFB300),

    // Contornos
    outline = Color(0xFF3A3530),
    outlineVariant = Color(0xFF2A2520),

    // Inversão
    inverseSurface = Color(0xFFE8E1D9),
    inverseOnSurface = Color(0xFF141414),
    inversePrimary = Color(0xFF6B4E00),

    // Scrim
    scrim = Color(0xFF000000),
)

@Composable
fun AutomaPokoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AutomaPokoColorScheme,
        typography = AutomaPokoTypography,
        content = content
    )
}
