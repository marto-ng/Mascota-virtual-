package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_notifications")
data class PetNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerUsername: String = "guest",
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "info" // info, alert, evolution, unlock
)
