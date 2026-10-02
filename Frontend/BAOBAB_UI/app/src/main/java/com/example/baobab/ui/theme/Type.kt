package com.example.baobab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private fun TextStyle.appFont() = copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp)
private val defaults = Typography()
val Typography = defaults.copy(
    displayLarge = defaults.displayLarge.appFont(),
    displayMedium = defaults.displayMedium.appFont(),
    displaySmall = defaults.displaySmall.appFont(),
    headlineLarge = defaults.headlineLarge.appFont(),
    headlineMedium = defaults.headlineMedium.appFont(),
    headlineSmall = defaults.headlineSmall.appFont(),
    titleLarge = defaults.titleLarge.appFont(),
    titleMedium = defaults.titleMedium.appFont(),
    titleSmall = defaults.titleSmall.appFont(),
    bodyLarge = defaults.bodyLarge.appFont(),
    bodyMedium = defaults.bodyMedium.appFont(),
    bodySmall = defaults.bodySmall.appFont(),
    labelLarge = defaults.labelLarge.appFont(),
    labelMedium = defaults.labelMedium.appFont(),
    labelSmall = defaults.labelSmall.appFont()
)
