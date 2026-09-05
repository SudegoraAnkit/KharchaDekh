package com.ankitsudegora.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconVector(name: String): ImageVector {
    return when (name.lowercase()) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        "home" -> Icons.Default.Home
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_bag" -> Icons.Default.ShoppingBag
        "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
        "movie" -> Icons.Default.Movie
        "medical_services" -> Icons.Default.MedicalServices
        "account_balance" -> Icons.Default.AccountBalance
        "category" -> Icons.Default.Category
        "payment" -> Icons.Default.Payment
        "settings" -> Icons.Default.Settings
        "pie_chart" -> Icons.Default.PieChart
        "add" -> Icons.Default.Add
        "notifications" -> Icons.Default.Notifications
        "warning" -> Icons.Default.Warning
        "check" -> Icons.Default.Check
        "arrow_back" -> Icons.AutoMirrored.Filled.ArrowBack
        "star" -> Icons.Default.Star
        "card_membership" -> Icons.Default.CardMembership
        "local_taxi" -> Icons.Default.LocalTaxi
        "celebration" -> Icons.Default.Celebration
        "savings" -> Icons.Default.Savings
        "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
        "redeem" -> Icons.Default.Redeem
        "payments" -> Icons.Default.Payments
        "restore" -> Icons.Default.Restore
        "credit_card" -> Icons.Default.CreditCard
        "school" -> Icons.Default.School
        "build" -> Icons.Default.Build
        "real_estate_agent" -> Icons.Default.RealEstateAgent
        "subscriptions" -> Icons.Default.Subscriptions
        "groups" -> Icons.Default.Groups
        "shield" -> Icons.Default.Shield
        "description" -> Icons.Default.Description
        "pets" -> Icons.Default.Pets
        "card_giftcard" -> Icons.Default.CardGiftcard
        "local_offer" -> Icons.Default.LocalOffer
        "laptop" -> Icons.Default.Laptop
        else -> Icons.Default.Category
    }
}

fun getCurrencySymbol(code: String?): String {
    if (code == null) return "₹"
    return when (code.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "AED" -> "د.إ"
        "AUD" -> "A$"
        "CAD" -> "C$"
        "SGD" -> "S$"
        else -> code
    }
}

/**
 * Dynamic Category Emoji Resolver.
 * Fixes static burger icons by matching real category names to suitable emojis.
 */
fun getCategoryEmoji(categoryName: String?): String {
    if (categoryName == null) return "💳"
    val lower = categoryName.lowercase()
    return when {
        lower.contains("invest") || lower.contains("sip") || lower.contains("mutual") || lower.contains("stock") -> "📈"
        lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("lunch") -> "🍽️"
        lower.contains("snack") || lower.contains("chai") || lower.contains("tea") || lower.contains("coffee") -> "☕"
        lower.contains("grocer") || lower.contains("kirana") || lower.contains("ration") -> "🛒"
        lower.contains("fuel") || lower.contains("petrol") || lower.contains("diesel") || lower.contains("cab") || lower.contains("taxi") || lower.contains("auto") -> "🚗"
        lower.contains("travel") || lower.contains("metro") || lower.contains("flight") || lower.contains("train") -> "🚆"
        lower.contains("bill") || lower.contains("utilit") || lower.contains("recharge") || lower.contains("electricity") || lower.contains("bijli") -> "⚡"
        lower.contains("rent") || lower.contains("home") || lower.contains("flat") || lower.contains("society") -> "🏠"
        lower.contains("movie") || lower.contains("entertain") || lower.contains("ott") || lower.contains("netflix") -> "🎬"
        lower.contains("shop") || lower.contains("cloth") || lower.contains("lifestyle") -> "🛍️"
        lower.contains("health") || lower.contains("medic") || lower.contains("doctor") || lower.contains("pharmacy") -> "💊"
        lower.contains("emi") || lower.contains("loan") -> "🏦"
        lower.contains("domestic") || lower.contains("maid") || lower.contains("cook") || lower.contains("driver") -> "🧹"
        lower.contains("insurance") || lower.contains("policy") -> "🛡️"
        lower.contains("tax") || lower.contains("gst") -> "📄"
        lower.contains("gift") || lower.contains("charity") || lower.contains("pooja") || lower.contains("shagun") -> "🎁"
        lower.contains("creditcard") || lower.contains("card") -> "💳"
        lower.contains("salary") || lower.contains("income") -> "💼"
        lower.contains("freelance") || lower.contains("hustle") -> "💻"
        lower.contains("cashback") || lower.contains("reward") -> "🎉"
        lower.contains("refund") -> "↩️"
        lower.contains("subscript") -> "📱"
        lower.contains("pet") -> "🐾"
        lower.contains("course") || lower.contains("school") || lower.contains("study") -> "🎓"
        else -> "🏷️"
    }
}

/**
 * Localized Display Names tailored for Indian Audience without altering database keys.
 */
fun getLocalizedCategoryDisplayName(name: String?): String {
    if (name == null) return "General"
    val lower = name.lowercase().trim()
    return when (lower) {
        "food & dining" -> "Food & Dining"
        "groceries" -> "Groceries & Kirana"
        "fuel & travel" -> "Fuel, Auto & Metro"
        "bills & utilities" -> "Bills, Recharge & Bijli"
        "rent & maintenance" -> "Rent & Society Maintenance"
        "sip/invest" -> "SIP & Investments"
        "emi & loans" -> "EMI & Loans"
        "domestic help" -> "Domestic Help & Maid"
        "health & medical" -> "Health & Medical"
        "entertainment" -> "Entertainment & OTT"
        "shopping" -> "Shopping & Lifestyle"
        "gifts & charity" -> "Gifts, Shagun & Charity"
        "creditcard payment" -> "Credit Card Repayment"
        "salary" -> "Salary & Inflow"
        "freelance/side hustle" -> "Freelance & Side Hustle"
        "cashback & rewards" -> "Cashback & Rewards"
        "refund" -> "Refunds & Reversals"
        else -> name
    }
}
