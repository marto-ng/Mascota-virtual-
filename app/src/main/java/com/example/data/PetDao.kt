package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_state WHERE id = 1")
    fun getPetState(): Flow<PetState?>

    @Query("SELECT * FROM pet_state WHERE id = 1")
    suspend fun getPetStateOnce(): PetState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePetState(state: PetState)

    @Query("SELECT * FROM shop_items")
    fun getAllShopItems(): Flow<List<ShopItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateShopItem(item: ShopItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopItems(items: List<ShopItem>)

    @Query("SELECT * FROM pet_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<PetNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: PetNotification)

    @Query("UPDATE pet_notifications SET isRead = 1")
    suspend fun markNotificationsAllRead()

    @Query("DELETE FROM pet_notifications")
    suspend fun clearAllNotifications()
}
