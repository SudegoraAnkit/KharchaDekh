package com.ankitsudegora.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconVector(name: String): ImageVector {
    return when (name.lowercase()) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        "home" -> Icons.Default.Home
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_bag" -> Icons.Default.ShoppingBag
        "receipt_long" -> Icons.Default.ReceiptLong
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
        "arrow_back" -> Icons.Default.ArrowBack
        "star" -> Icons.Default.Star
        "card_membership" -> Icons.Default.CardMembership
        "local_taxi" -> Icons.Default.LocalTaxi
        "celebration" -> Icons.Default.Celebration
        "savings" -> Icons.Default.Savings
        "trending_up" -> Icons.Default.TrendingUp
        "redeem" -> Icons.Default.Redeem
        "payments" -> Icons.Default.Payments
        "restore" -> Icons.Default.Restore
        else -> Icons.Default.Category
    }
}
