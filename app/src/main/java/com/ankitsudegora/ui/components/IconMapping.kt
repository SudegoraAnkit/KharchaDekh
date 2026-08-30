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
