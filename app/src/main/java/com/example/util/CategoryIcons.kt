package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Propane
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {

    val AVAILABLE_ICONS = listOf(
        "groceries" to "Groceries",
        "milk" to "Milk & Dairy",
        "fruits" to "Produce",
        "petrol" to "Fuel/Petrol",
        "diesel" to "Diesel",
        "electricity" to "Electricity",
        "water" to "Water",
        "phone" to "Phone Recharge",
        "wifi" to "Internet/Wi-Fi",
        "gas" to "LPG Gas",
        "housing" to "Rent/Housing",
        "medical" to "Medical",
        "education" to "Education",
        "transport" to "Transport",
        "shopping" to "Shopping",
        "restaurant" to "Food & Dining",
        "entertainment" to "Entertainment",
        "travel" to "Travel",
        "personal_care" to "Personal Care",
        "household" to "Household",
        "repairs" to "Repairs",
        "loan" to "Loan/EMI",
        "insurance" to "Insurance",
        "bank" to "Bank Fees",
        "other" to "General"
    )

    fun getIcon(key: String): ImageVector {
        return when (key.lowercase()) {
            "groceries" -> Icons.Default.ShoppingCart
            "milk" -> Icons.Default.LocalDrink
            "fruits" -> Icons.Default.Eco
            "petrol" -> Icons.Default.LocalGasStation
            "diesel" -> Icons.Default.LocalGasStation
            "electricity" -> Icons.Default.ElectricBolt
            "water" -> Icons.Default.WaterDrop
            "phone" -> Icons.Default.PhoneAndroid
            "wifi" -> Icons.Default.Wifi
            "gas" -> Icons.Default.Propane
            "housing" -> Icons.Default.Home
            "medical" -> Icons.Default.LocalHospital
            "education" -> Icons.Default.School
            "transport" -> Icons.Default.DirectionsBus
            "shopping" -> Icons.Default.ShoppingBag
            "restaurant" -> Icons.Default.Restaurant
            "entertainment" -> Icons.Default.Movie
            "travel" -> Icons.Default.Flight
            "personal_care" -> Icons.Default.Spa
            "household" -> Icons.Default.CleaningServices
            "repairs" -> Icons.Default.Build
            "loan" -> Icons.Default.Payment
            "insurance" -> Icons.Default.Security
            "bank" -> Icons.Default.AccountBalance
            else -> Icons.Default.Category
        }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF2563EB)): Color {
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            val colorInt = when (cleanHex.length) {
                6 -> ("FF$cleanHex").toLong(16)
                8 -> cleanHex.toLong(16)
                else -> return fallback
            }
            Color(colorInt)
        } catch (_: Exception) {
            fallback
        }
    }
}
