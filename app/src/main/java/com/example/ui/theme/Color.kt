package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Brand Palette: High-craft Emerald, Deep Marine & Mint accents
val EmeraldPrimary = Color(0xFF0F664C)
val EmeraldDark = Color(0xFF074533)
val EmeraldLight = Color(0xFF2CB686)

val TealSecondary = Color(0xFF1B6A78)
val TealDark = Color(0xFF0E4650)
val TealLight = Color(0xFF38A8BA)

val MintAccent = Color(0xFF4EE8B1)
val WarmCoral = Color(0xFFFF6F59)
val GoldenAmber = Color(0xFFF59E0B)

// Soft Surface & Background hierarchy (Warm Slate / Clean Ceramic)
val BackgroundLight = Color(0xFFF4F7F5)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE8EFEA)
val SurfaceSubtle = Color(0xFFF0F5F2)

// Card & Stroke accents
val HairlineBorder = Color(0x1A0F664C)
val CardShadowTint = Color(0x140F3E30)

// Text & Content Hierarchy
val OnSurfacePrimary = Color(0xFF13231D)
val OnSurfaceSecondary = Color(0xFF4A6357)
val OnSurfaceTertiary = Color(0xFF7E978C)

// Semantic Status Pill Colors (Light Mode)
val StatusVerifiedBg = Color(0xFFD7F5E8)
val StatusVerifiedFg = Color(0xFF0C6B4B)
val StatusNeedsReviewBg = Color(0xFFFEF3C7)
val StatusNeedsReviewFg = Color(0xFFB45309)
val StatusDeadBg = Color(0xFFFEE2E2)
val StatusDeadFg = Color(0xFFB91C1C)

// Semantic Status Pill Colors (Dark Mode)
val StatusVerifiedBgDark = Color(0xFF0D3D2E)
val StatusVerifiedFgDark = Color(0xFF6EE7B7)
val StatusNeedsReviewBgDark = Color(0xFF3B2508)
val StatusNeedsReviewFgDark = Color(0xFFFDE68A)
val StatusDeadBgDark = Color(0xFF3D1414)
val StatusDeadFgDark = Color(0xFFFCA5A5)

// Dark Theme Colors
val EmeraldPrimaryDarkTheme = Color(0xFF4EE8B1)
val TealSecondaryDarkTheme = Color(0xFF5EEAD4)
val BackgroundDark = Color(0xFF0C1612)
val SurfaceDark = Color(0xFF13221C)
val SurfaceVariantDark = Color(0xFF1C3128)
val OnSurfaceDark = Color(0xFFE4EDE7)
val OnSurfaceVariantDark = Color(0xFFA5BDB0)
val HairlineBorderDark = Color(0x334EE8B1)
val CardShadowTintDark = Color(0x66000000)

// Premium Gradient Brushes
val HeroGradientBrush = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0D5E46), Color(0xFF157861), Color(0xFF1B8A6E))
)

val CardGlowGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF6FAF7))
)

val StatCardGradient1 = Brush.linearGradient(
    colors = listOf(Color(0xFF0E6349), Color(0xFF1E8264))
)

val StatCardGradient2 = Brush.linearGradient(
    colors = listOf(Color(0xFF185D6F), Color(0xFF287D94))
)
