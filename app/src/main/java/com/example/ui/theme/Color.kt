package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// PDFGo Palette - Slate & Deep Navy
val BrandBlue = Color(0xFF2563EB)
val BrandBlueDark = Color(0xFF1D4ED8)
val BrandBlueLight = Color(0xFFDBEAFE)
val BrandBlueGlow = Color(0xFF3B82F6)
val BrandIndigo = Color(0xFF4F46E5)
val BrandCyan = Color(0xFF06B6D4)
val BrandViolet = Color(0xFF7C3AED)

val Slate950 = Color(0xFF090D16)
val Slate900 = Color(0xFF0F172A)
val Slate850 = Color(0xFF131C2E)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)
val Rose500 = Color(0xFFF43F5E)
val Rose600 = Color(0xFFE11D48)
val Rose950 = Color(0xFF4C0519)

// Modern Brand Gradients
val PrimaryGradient = Brush.horizontalGradient(
  colors = listOf(BrandBlue, BrandIndigo)
)

val PrimaryGradientVertical = Brush.verticalGradient(
  colors = listOf(BrandBlue, BrandIndigo)
)

val HeroTextGradient = Brush.horizontalGradient(
  colors = listOf(BrandBlue, BrandIndigo, BrandViolet)
)

val HeroBadgeGradient = Brush.horizontalGradient(
  colors = listOf(BrandBlue.copy(alpha = 0.15f), BrandCyan.copy(alpha = 0.15f))
)

val CardBorderGradient = Brush.horizontalGradient(
  colors = listOf(BrandBlue.copy(alpha = 0.65f), BrandIndigo.copy(alpha = 0.35f))
)

val DangerGradient = Brush.horizontalGradient(
  colors = listOf(Rose500, Rose600)
)

val DarkSurfaceGradient = Brush.verticalGradient(
  colors = listOf(Slate850, Slate950)
)

val TopBarGradient = Brush.verticalGradient(
  colors = listOf(Slate900, Slate950)
)

val LightMeshGradient = Brush.radialGradient(
  colors = listOf(BrandBlueLight.copy(alpha = 0.35f), Color.Transparent),
  radius = 1200f
)

val PillControlGradient = Brush.horizontalGradient(
  colors = listOf(Slate900, Slate950)
)

// Home Screen Background Gradients
val HomeDarkBaseGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFF0C162D), // Deep brand navy
    Color(0xFF0F172A), // Slate 900
    Color(0xFF090D16), // Slate 950
  )
)

val HomeLightBaseGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFFEFF6FF), // Soft azure blue 50 tint
    Color(0xFFF1F5F9), // Slate 100
    Color(0xFFF8FAFC), // Slate 50 base
  )
)

