package com.ankitsudegora.ui.theme

import androidx.compose.ui.graphics.Color

// --- LIGHT THEME COLOR PALETTE (High Contrast) ---
val PrimaryTeal = Color(0xFF0D9488)           // Teal 600 (Accessible Primary)
val OnPrimaryTeal = Color.White
val PrimaryContainerTeal = Color(0xFFE6F4F1)  // Clean, visible container tint
val OnPrimaryContainerTeal = Color(0xFF0A5C54) // Dark teal for readability

val SecondarySlate = Color(0xFF475569)        // Slate 600 (Accessible secondary)
val OnSecondarySlate = Color.White
val SecondaryContainerSlate = Color(0xFFF1F5F9) // Slate 100
val OnSecondaryContainerSlate = Color(0xFF0F172A) // Slate 900

val GeoBackground = Color(0xFFF8FAFC) // Solid slate-white for pleasant, clean light mode background
val GeoOnBackground = Color(0xFF0F172A)       // Slate 900 for high-contrast text

val GeoSurface = Color.White
val GeoOnSurface = Color(0xFF0F172A)
val GeoSurfaceVariant = Color(0xFFF1F5F9)     // Slate 100 (for secondary items)
val GeoOnSurfaceVariant = Color(0xFF1E293B)    // Slate 800 (Ensures WCAG text contrast)

val GeoOutline = Color(0xFFCBD5E1)            // Slate 300
val GeoOutlineVariant = Color(0xFFE2E8F0)     // Slate 200

val GeoError = Color(0xFFB91C1C)              // Red 700 (High contrast)
val GeoOnError = Color.White
val GeoErrorContainer = Color(0xFFFEE2E2)      // Red 100
val GeoOnErrorContainer = Color(0xFF7F1D1D)    // Red 900

// --- DARK THEME COLOR PALETTE (High Contrast Deep Dark Mode) ---
val DarkPrimaryTeal = Color(0xFF2DD4BF)       // Teal 400 (Vibrant, high-contrast teal)
val DarkOnPrimary = Color(0xFF042F2E)          // Deepest teal
val DarkPrimaryContainer = Color(0xFF115E59)   // Teal 800
val DarkOnPrimaryContainer = Color(0xFFCCFBF1) // Teal 100 (high readability)

val DarkSecondary = Color(0xFF94A3B8)          // Slate 400
val DarkOnSecondary = Color(0xFF0F172A)        // Slate 900
val DarkSecondaryContainer = Color(0xFF334155) // Slate 700
val DarkOnSecondaryContainer = Color(0xFFF8FAFC) // Slate 50

val DarkBackground = Color(0xFF090D16)         // True dark background (Slate-based pitch)
val DarkOnBackground = Color(0xFFF8FAFC)       // High contrast white text

val DarkSurface = Color(0xFF151D2A)            // Slightly lighter slate surface card
val DarkOnSurface = Color(0xFFF8FAFC)          // High contrast white text
val DarkSurfaceVariant = Color(0xFF1E293B)      // Slate 800 card variant
val DarkOnSurfaceVariant = Color(0xFFCBD5E1)    // Slate 300 for readable secondary text

val DarkOutline = Color(0xFF475569)            // Slate 600
val DarkOutlineVariant = Color(0xFF334155)     // Slate 700

val DarkError = Color(0xFFF87171)              // Red 400 (Vibrant red)
val DarkOnError = Color(0xFF7F1D1D)            // Red 900
val DarkErrorContainer = Color(0xFF991B1B)      // Red 800
val DarkOnErrorContainer = Color(0xFFFEE2E2)    // Red 100
