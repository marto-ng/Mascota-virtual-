package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PetRepository(private val petDao: PetDao) {

    val petState: Flow<PetState?> = petDao.getPetState()
    val allShopItems: Flow<List<ShopItem>> = petDao.getAllShopItems()
    val allNotifications: Flow<List<PetNotification>> = petDao.getAllNotifications()

    suspend fun getPetStateOnce(): PetState? = petDao.getPetStateOnce()

    suspend fun updatePetState(state: PetState) {
        petDao.updatePetState(state)
    }

    suspend fun updateShopItem(item: ShopItem) {
        petDao.updateShopItem(item)
    }

    suspend fun insertNotification(message: String, type: String = "info") {
        val notification = PetNotification(message = message, type = type)
        petDao.insertNotification(notification)
    }

    suspend fun markAllNotificationsAsRead() {
        petDao.markNotificationsAllRead()
    }

    suspend fun clearAllNotifications() {
        petDao.clearAllNotifications()
    }

    /**
     * Verifies and inserts default pet state and shop items if database is empty.
     */
    suspend fun initializeDefaultDataIfNeeded() {
        try {
            // Check if PetState is empty
            val currentState = petDao.getPetStateOnce()
            if (currentState == null) {
                val defaultPet = PetState(
                    name = "Mochi",
                    hunger = 80f,
                    sleep = 85f,
                    happiness = 80f,
                    xp = 0,
                    level = 1,
                    coins = 120, // Give them initial capital to shop
                    evolutionStage = "Bebé",
                    evolutionPath = "Normal",
                    skinColor = "Lilac",
                    equippedAccessory = "none",
                    isSleeping = false,
                    lastUpdateTime = System.currentTimeMillis()
                )
                petDao.updatePetState(defaultPet)
                
                // Add first greeting notification
                insertNotification(
                    message = "¡Hola! Soy tu nueva mascota virtual, Mochi. ¡Cuídame bien! 💖",
                    type = "info"
                )
            }

            // Check if Shop Items are empty
            val items = petDao.getAllShopItems()
            // Using firstOrNull with short suspension
            val currentItemsList = petDao.getAllShopItems()
            // If we have none, pre-populate
            // We can just query a single count or use an auxiliary query, but loading the list once is standard
            val isItemsEmpty = currentItemsList.firstOrNull()?.isEmpty() ?: true
            if (isItemsEmpty) {
                val defaultShopItems = listOf(
                    ShopItem("color_lilac", isUnlocked = true, "color", "Violeta Mochi", 0),
                    ShopItem("color_sky", isUnlocked = false, "color", "Cielo Azul 🌌", 50),
                    ShopItem("color_mint", isUnlocked = false, "color", "Menta Fresca 🌿", 75),
                    ShopItem("color_peach", isUnlocked = false, "color", "Melocotón Suave 🍑", 100),
                    ShopItem("color_gold", isUnlocked = false, "color", "Oro Celestial ✨", 200),
                    
                    ShopItem("acc_party_hat", isUnlocked = false, "accessory", "Gorro de Fiesta 🎉", 50),
                    ShopItem("acc_sunglasses", isUnlocked = false, "accessory", "Gafas de Sol Cool 😎", 80),
                    ShopItem("acc_bowtie", isUnlocked = false, "accessory", "Pajarita Elegante 🎀", 60),
                    ShopItem("acc_crown", isUnlocked = false, "accessory", "Corona de Rey 👑", 150),
                    ShopItem("acc_wizard_hat", isUnlocked = false, "accessory", "Sombrero de Mago 🧙‍♂️", 120)
                )
                petDao.insertShopItems(defaultShopItems)
                Log.d("PetRepository", "Shop items successfully database initialized.")
            }
        } catch (e: Exception) {
            Log.e("PetRepository", "Failed to initialize default data: ${e.message}", e)
        }
    }
}
