package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PetState::class, ShopItem::class, PetNotification::class],
    version = 1,
    exportSchema = false
)
abstract class PetDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
}
