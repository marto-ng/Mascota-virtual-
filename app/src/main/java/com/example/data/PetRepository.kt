package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PetRepository(private val petDao: PetDao) {

    val allShopItems: Flow<List<ShopItem>> = petDao.getAllShopItems()

    fun getPetStateForUser(username: String): Flow<PetState?> = petDao.getPetState(username)
    fun getAllNotificationsForUser(username: String): Flow<List<PetNotification>> = petDao.getAllNotifications(username)

    suspend fun getPetStateOnceForUser(username: String): PetState? = petDao.getPetStateOnce(username)

    suspend fun updatePetState(state: PetState) {
        petDao.updatePetState(state)
    }

    suspend fun updateShopItem(item: ShopItem) {
        petDao.updateShopItem(item)
    }

    suspend fun insertNotification(username: String, message: String, type: String = "info") {
        val notification = PetNotification(ownerUsername = username, message = message, type = type)
        petDao.insertNotification(notification)
    }

    suspend fun markAllNotificationsAsReadForUser(username: String) {
        petDao.markNotificationsAllRead(username)
    }

    suspend fun clearAllNotificationsForUser(username: String) {
        petDao.clearAllNotifications(username)
    }

    // User authentication and registration
    suspend fun registerUser(user: User): String? {
        return try {
            val existingByEmail = petDao.getUserByEmail(user.email)
            if (existingByEmail != null) {
                return "El correo electrónico ya está registrado."
            }
            val existingByUsername = petDao.getUserByUsername(user.username)
            if (existingByUsername != null) {
                return "El nombre de usuario ya está registrado."
            }
            petDao.insertUser(user)
            // Create their initial unhatched egg
            createEggForUser(user.username)
            null
        } catch (e: Exception) {
            Log.e("PetRepository", "Failed to register user: ${e.message}", e)
            "Error al registrar usuario: ${e.message}"
        }
    }

    suspend fun loginUser(email: String, passwordHash: String): User? {
        val user = petDao.getUserByEmail(email) ?: return null
        return if (user.passwordHash == passwordHash) user else null
    }

    suspend fun createEggForUser(username: String): PetState {
        val egg = PetState(
            ownerUsername = username,
            name = "Huevo Mochi",
            hunger = 100f,
            sleep = 100f,
            happiness = 100f,
            xp = 0,
            level = 1,
            coins = 120,
            evolutionStage = "Huevo",
            evolutionPath = "Normal",
            skinColor = "Lilac",
            equippedAccessory = "none",
            isSleeping = false,
            gender = "Ninguno",
            lastUpdateTime = System.currentTimeMillis()
        )
        petDao.updatePetState(egg)
        insertNotification(username, "¡Se ha detectado un nuevo Huevo Mochi! Prepárate para eclosionarlo. 🥚✨", "info")
        return egg
    }

    /**
     * Verifies and inserts default shop items if database is empty.
     */
    suspend fun initializeDefaultDataIfNeeded() {
        try {
            // Check if Shop Items are empty
            val currentItemsList = petDao.getAllShopItems()
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
