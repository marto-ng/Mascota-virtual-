package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_items")
data class ShopItem(
    @PrimaryKey val itemId: String, // e.g., "color_sky", "color_mint", "acc_sunglasses"
    val isUnlocked: Boolean = false,
    val type: String, // "color" or "accessory"
    val displayName: String,
    val price: Int
)
