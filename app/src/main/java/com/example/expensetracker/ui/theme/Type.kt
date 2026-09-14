package com.example.expensetracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetracker.R


// 1) Define your custom font family from res/font
val PlusJakartaSans = FontFamily(
    Font(R.font.regular, FontWeight.Normal),
    Font(R.font.medium, FontWeight.Medium),
    Font(R.font.semibold, FontWeight.SemiBold),
    Font(R.font.bold, FontWeight.Bold),
)

// 2) Keep Material defaults, just swap fontFamily (minimal-risk)
private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = PlusJakartaSans),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = PlusJakartaSans),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = PlusJakartaSans),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = PlusJakartaSans),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = PlusJakartaSans),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = PlusJakartaSans),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = PlusJakartaSans),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = PlusJakartaSans),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = PlusJakartaSans),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = PlusJakartaSans),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = PlusJakartaSans),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = PlusJakartaSans),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = PlusJakartaSans),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = PlusJakartaSans),
)