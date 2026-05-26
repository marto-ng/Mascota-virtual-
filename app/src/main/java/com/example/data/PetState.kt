package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_state")
data class PetState(
    @PrimaryKey val ownerUsername: String = "guest",
    val name: String = "Mochi",
    val hunger: Float = 80f, // 0 to 100 (100 is full, 0 is starving)
    val sleep: Float = 80f,  // 0 to 100 (100 is fully rested, 0 is exhausted)
    val happiness: Float = 80f, // 0 to 100 (100 is joyful, 0 is depressed)
    val health: Float = 80f, // 0 to 100
    val hygiene: Float = 80f, // 0 to 100
    val currentEmotion: String = "Normal", // Normal, Enojo, Aburrimiento, Miedo, Vergüenza, Timidez, Cariño/amor, Celos, Soledad, Emoción, Confusión, Jugando, Bailando, Cantando, Durmiendo profundamente, Enfermo, Pensando, Explorando, Esperando al jugador
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 150, // Starts with some coins to let user spend in the shop
    val evolutionStage: String = "Bebé", // Bebé, Niño, Adolescente, Adulto
    val evolutionPath: String = "Normal", // Normal, Fuego, Agua, Cosmos
    val skinColor: String = "Lilac", // Lilac, Sky, Mint, Peach, Gold
    val equippedAccessory: String = "none", // none, party_hat, sunglasses, bowtie, crown, wizard_hat
    val isSleeping: Boolean = false,
    val gender: String = "Ninguno", // Ninguno, Macho, Hembra
    val lastUpdateTime: Long = System.currentTimeMillis()
)
