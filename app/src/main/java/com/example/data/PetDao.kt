package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_state WHERE ownerUsername = :username")
    fun getPetState(username: String): Flow<PetState?>

    @Query("SELECT * FROM pet_state WHERE ownerUsername = :username")
    suspend fun getPetStateOnce(username: String): PetState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePetState(state: PetState)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM shop_items")
    fun getAllShopItems(): Flow<List<ShopItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateShopItem(item: ShopItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopItems(items: List<ShopItem>)

    @Query("SELECT * FROM pet_notifications WHERE ownerUsername = :username ORDER BY timestamp DESC")
    fun getAllNotifications(username: String): Flow<List<PetNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: PetNotification)

    @Query("UPDATE pet_notifications SET isRead = 1 WHERE ownerUsername = :username")
    suspend fun markNotificationsAllRead(username: String)

    @Query("DELETE FROM pet_notifications WHERE ownerUsername = :username")
    suspend fun clearAllNotifications(username: String)
}
