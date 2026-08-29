package com.ankitsudegora.ui.theme

import androidx.compose.ui.graphics.Color

// --- BRAND & PRIMARY ACCENT TOKENS ---
// Fresh vibrant lime / natural green for financial health, positive actions, confirmations
val BrandLime = Color(0xFF22C55E)            // Green 500
val BrandLimeLight = Color(0xFF4ADE80)       // Green 400
val BrandLimeDark = Color(0xFF16A34A)        // Green 600
val BrandLimeContainer = Color(0xFF052E16)   // Deep translucent green
val BrandOnLimeContainer = Color(0xFF86EFAC) // Green 300

// --- DARK THEME COLOR PALETTE (Sophisticated Dark-First Design) ---
val DarkBackground = Color(0xFF090D14)       // Near-black rich charcoal background
val DarkOnBackground = Color(0xFFF1F5F9)     // Slate 100 high-contrast white text

val DarkSurface = Color(0xFF131823)          // Elevated dark card surface
val DarkOnSurface = Color(0xFFF8FAFC)        // Slate 50 crisp text
val DarkSurfaceVariant = Color(0xFF1A2232)   // Secondary item card / input well
val DarkOnSurfaceVariant = Color(0xFF94A3B8) // Slate 400 for secondary descriptions

val DarkSurfaceElevated = Color(0xFF222D40)  // Active chips, interactive items
val DarkBorder = Color(0xFF222C3E)           // Subtle 1dp border for crisp card grouping
val DarkBorderSubtle = Color(0xFF18202F)     // Low-contrast divider

val DarkSecondary = Color(0xFF94A3B8)        // Slate 400
val DarkOnSecondary = Color(0xFF0F172A)      // Slate 900
val DarkSecondaryContainer = Color(0xFF1E293B)
val DarkOnSecondaryContainer = Color(0xFFE2E8F0)

val DarkError = Color(0xFFF87171)            // Red 400
val DarkOnError = Color(0xFF450A0A)          // Red 950
val DarkErrorContainer = Color(0xFF3B1219)    // Deep dark red container
val DarkOnErrorContainer = Color(0xFFFCA5A5)  // Red 300

// --- LIGHT THEME COLOR PALETTE (Accessible Companion) ---
val LightPrimary = Color(0xFF16A34A)
val LightOnPrimary = Color.White
val LightPrimaryContainer = Color(0xFFDCFCE7)
val LightOnPrimaryContainer = Color(0xFF14532D)

val LightBackground = Color(0xFFF8FAFC)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color.White
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF475569)

val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)
val LightError = Color(0xFFDC2626)
val LightOnError = Color.White
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF991B1B)

// --- CONTROLLED SEMANTIC CATEGORY ACCENTS ---
val CategoryFood = Color(0xFFF97316)          // Warm Orange
val CategoryTransport = Color(0xFF38BDF8)     // Sky Blue
val CategoryGroceries = Color(0xFF10B981)     // Emerald Teal
val CategoryBills = Color(0xFFA855F7)         // Purple
val CategoryShopping = Color(0xFFEC4899)      // Pink
val CategoryHealth = Color(0xFFEF4444)        // Coral Red
val CategoryEntertainment = Color(0xFFF43F5E) // Rose
val CategoryInvest = Color(0xFF06B6D4)        // Cyan
val CategorySalary = Color(0xFF22C55E)        // Lime Green
val CategoryRent = Color(0xFF8B5CF6)          // Violet
val CategoryOthers = Color(0xFF94A3B8)        // Slate Gray

object CategoryColors {
    fun getCategoryColor(categoryName: String?): Color {
        if (categoryName == null) return CategoryOthers
        val lower = categoryName.lowercase()
        return when {
            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("cafe") -> CategoryFood
            lower.contains("fuel") || lower.contains("travel") || lower.contains("car") || lower.contains("taxi") || lower.contains("uber") || lower.contains("ola") -> CategoryTransport
            lower.contains("grocer") || lower.contains("supermarket") || lower.contains("blinkit") || lower.contains("zepto") -> CategoryGroceries
            lower.contains("bill") || lower.contains("utilit") || lower.contains("electricity") || lower.contains("recharge") -> CategoryBills
            lower.contains("shop") || lower.contains("cloth") || lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> CategoryShopping
            lower.contains("health") || lower.contains("medic") || lower.contains("doctor") || lower.contains("pharmacy") -> CategoryHealth
            lower.contains("entertain") || lower.contains("movie") || lower.contains("netflix") || lower.contains("spotify") || lower.contains("game") -> CategoryEntertainment
            lower.contains("invest") || lower.contains("sip") || lower.contains("stock") || lower.contains("mutual") -> CategoryInvest
            lower.contains("salary") || lower.contains("interest") || lower.contains("inflow") || lower.contains("freelance") || lower.contains("cashback") -> CategorySalary
            lower.contains("rent") || lower.contains("maintenance") || lower.contains("home") -> CategoryRent
            else -> CategoryOthers
        }
    }

    fun getCategoryContainerColor(categoryName: String?): Color {
        return getCategoryColor(categoryName).copy(alpha = 0.15f)
    }
}

