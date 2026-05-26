package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PetNotification
import com.example.data.PetRepository
import com.example.data.PetState
import com.example.data.ShopItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class PetViewModel(
    application: Application,
    private val repository: PetRepository
) : AndroidViewModel(application) {

    private val _petStateLocal = MutableStateFlow<PetState?>(null)
    val petState: StateFlow<PetState?> = _petStateLocal.asStateFlow()

    private val _shopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    private val _notifications = MutableStateFlow<List<PetNotification>>(emptyList())
    val notifications: StateFlow<List<PetNotification>> = _notifications.asStateFlow()

    // Temporary active interaction trigger (controls animations like eating, cleaning, loving)
    private val _activeInteraction = MutableStateFlow<String>("none")
    val activeInteraction: StateFlow<String> = _activeInteraction.asStateFlow()

    // Show dialog/choice or notice for pending evolution milestone
    private val _pendingEvolutionChoice = MutableStateFlow<Boolean>(false)
    val pendingEvolutionChoice: StateFlow<Boolean> = _pendingEvolutionChoice.asStateFlow()

    // Active screen index (0: Casa/Home, 1: Tienda/Shop, 2: Minijuego/Games, 3: Historial/Alerts)
    private val _currentScreen = MutableStateFlow(0)
    val currentScreen: StateFlow<Int> = _currentScreen.asStateFlow()

    // Game stats
    private val _gameActive = MutableStateFlow(false)
    val gameActive: StateFlow<Boolean> = _gameActive.asStateFlow()
    private val _gameTargetId = MutableStateFlow(0) // correct option in the mini-game
    val gameTargetId: StateFlow<Int> = _gameTargetId.asStateFlow()
    private val _gameFeedback = MutableStateFlow("")
    val gameFeedback: StateFlow<String> = _gameFeedback.asStateFlow()

    private var decayJob: Job? = null
    private val CHANNEL_ID = "pet_care_notifications"

    init {
        createNotificationChannel()
        
        // Start collecting database state flow
        viewModelScope.launch {
            repository.petState.collectLatest { dbState ->
                if (dbState != null) {
                    if (_petStateLocal.value == null) {
                        // First load: perform time-based offline decay calculation
                        val elapsedState = calculateOfflineDecay(dbState)
                        _petStateLocal.value = elapsedState
                        repository.updatePetState(elapsedState)
                    } else {
                        // Keep synced without overwriting faster local transitions instantly to avoid race conditions
                        _petStateLocal.value = dbState
                    }
                }
            }
        }

        // Collect shop items
        viewModelScope.launch {
            repository.allShopItems
                .catch { Log.e("PetViewModel", "Error fetching shop items: ${it.message}") }
                .collectLatest { items ->
                    _shopItems.value = items
                }
        }

        // Collect notifications
        viewModelScope.launch {
            repository.allNotifications
                .catch { Log.e("PetViewModel", "Error fetching notifications: ${it.message}") }
                .collectLatest { notifyList ->
                    _notifications.value = notifyList
                }
        }

        // Start active periodic decay loop (runs every 8 seconds)
        startDecayLoop()
    }

    fun changeScreen(index: Int) {
        _currentScreen.value = index
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Cuidados de Mascota"
            val descriptionText = "Recordatorios inteligentes para cuidar a tu mascota"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendSystemNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(getApplication(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat) // default system drawable for compatibility
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val notificationManager = getApplication<Application>()
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e("PetViewModel", "No permission to post notification: ${e.message}")
        }
    }

    private fun calculateOfflineDecay(state: PetState): PetState {
        val now = System.currentTimeMillis()
        val elapsedMs = now - state.lastUpdateTime
        if (elapsedMs <= 0 || state.isSleeping) {
            // If sleeping, sleep state regenerates continuously while app is closed
            if (state.isSleeping && elapsedMs > 0) {
                val hours = elapsedMs / 3600000.0
                val sleepRecovered = (hours * 22f).toFloat() // Recover 22 points per hour
                var newSleep = min(100f, state.sleep + sleepRecovered)
                var wasSleeping = state.isSleeping
                if (newSleep >= 100f) {
                    newSleep = 100f
                    wasSleeping = false
                }
                
                // Hunger decay is reduced while sleeping
                val hungerDecay = (hours * 4.5f).toFloat() // smaller decay during sleep
                val newHunger = max(0f, state.hunger - hungerDecay)
                
                return state.copy(
                    sleep = newSleep,
                    hunger = newHunger,
                    isSleeping = wasSleeping,
                    lastUpdateTime = now
                )
            }
            return state.copy(lastUpdateTime = now)
        }

        // Limit maximum elapsed calculation to 12 hours to prevent pet from starving completely if gone for long
        val cappedMs = min(elapsedMs, 12 * 3600000L)
        val hours = cappedMs / 3600000.0

        // Starving rate: 6.5 units per hour, exhaustion 4.5 units per hour
        val hungerDecay = (hours * 6.5).toFloat()
        val sleepDecay = (hours * 4.5).toFloat()

        val newHunger = max(0f, state.hunger - hungerDecay)
        val newSleep = max(0f, state.sleep - sleepDecay)

        // Happiness calculation
        val overallWellness = (newHunger + newSleep) / 2f
        val happinessDecay = if (overallWellness < 40f) {
            (hours * 10f).toFloat() // drops rapidly if neglected
        } else {
            (hours * 2f).toFloat() // natural drop
        }
        val newHappiness = max(0f, state.happiness - happinessDecay)

        return state.copy(
            hunger = newHunger,
            sleep = newSleep,
            happiness = newHappiness,
            lastUpdateTime = now
        )
    }

    private fun startDecayLoop() {
        decayJob?.cancel()
        decayJob = viewModelScope.launch {
            while (true) {
                delay(10000) // update stats every 10 seconds
                val current = _petStateLocal.value ?: continue
                
                if (current.evolutionStage == "Huevo") {
                    continue
                }
                
                var hunger = current.hunger
                var sleep = current.sleep
                var happiness = current.happiness
                var isSleeping = current.isSleeping

                if (isSleeping) {
                    // Sleep recovers quickly
                    sleep = min(100f, sleep + 2.5f)
                    if (sleep >= 100f) {
                        isSleeping = false
                        viewModelScope.launch {
                            repository.insertNotification("💤 ¡${current.name} se ha despertado totalmente recuperado y lleno de energía!", "info")
                            sendSystemNotification("⚡ ¡Mascota Despierta!", "¡${current.name} está lleno de energía!")
                        }
                    }
                    // Lesser hunger decay during sleep
                    hunger = max(0f, hunger - 0.2f)
                } else {
                    // Normal awake decay
                    hunger = max(0f, hunger - 0.4f)
                    sleep = max(0f, sleep - 0.3f)
                }

                // Wellness modifiers
                val combinedStats = (hunger + sleep) / 2f
                if (combinedStats < 30f) {
                    happiness = max(0f, happiness - 0.8f) // gets sad/depressed if hungry or sleepy
                } else if (combinedStats > 75f) {
                    happiness = min(100f, happiness + 0.3f) // recovers naturally if clean + fed
                }

                // Trigger alerts and notifications when stats cross thresholds
                triggerSmartWarnings(current, hunger, sleep, combinedStats)

                val updated = current.copy(
                    hunger = hunger,
                    sleep = sleep,
                    happiness = happiness,
                    isSleeping = isSleeping,
                    lastUpdateTime = System.currentTimeMillis()
                )
                
                _petStateLocal.value = updated
                repository.updatePetState(updated)
            }
        }
    }

    private var hungerAlertSent = false
    private var sleepAlertSent = false

    private fun triggerSmartWarnings(pet: PetState, currentHunger: Float, currentSleep: Float, wellness: Float) {
        viewModelScope.launch {
            if (currentHunger < 20f && !hungerAlertSent) {
                hungerAlertSent = true
                repository.insertNotification("🚨 ¡⚠️ Alerta! ${pet.name} tiene demasiada hambre. ¡Dale de comer algo rico pronto!", "alert")
                sendSystemNotification("🍖 ¡Tengo Hambre!", "¡Por favor, alimenta a ${pet.name}! Está perdiendo felicidad.")
            } else if (currentHunger >= 30f) {
                hungerAlertSent = false
            }

            if (currentSleep < 20f && !sleepAlertSent && !pet.isSleeping) {
                sleepAlertSent = true
                repository.insertNotification("🚨 ¡⚠️ Alerta! ${pet.name} está exhausto y necesita dormir.", "alert")
                sendSystemNotification("😴 ¡Tengo Sueño!", "¡Pon a dormir a ${pet.name} para que recupere su energía!")
            } else if (currentSleep >= 30f) {
                sleepAlertSent = false
            }
        }
    }

    // Care action: Acariciar (Petting/Love)
    fun strokePet() {
        val current = _petStateLocal.value ?: return
        if (current.isSleeping) return // can't pet while sleeping

        viewModelScope.launch {
            _activeInteraction.value = "petting"
            val updatedHappiness = min(100f, current.happiness + 15f)
            val updatedCoins = current.coins + 5
            val updatedXP = current.xp + 5

            val newState = current.copy(
                happiness = updatedHappiness,
                coins = updatedCoins,
                xp = updatedXP,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification("❤️ ¡Le diste mimos a ${current.name}! Ganaste 5 monedas y 5 EXP.", "care")
            
            checkForLevelUp(newState)

            delay(2500)
            _activeInteraction.value = "none"
        }
    }

    // Care action: Alimentar (Feed)
    fun feedPet() {
        val current = _petStateLocal.value ?: return
        if (current.isSleeping) return // can't eat while sleeping

        if (current.hunger >= 98f) {
            viewModelScope.launch {
                repository.insertNotification("🍪 ¡${current.name} está demasiado lleno en este momento!", "info")
            }
            return
        }

        viewModelScope.launch {
            _activeInteraction.value = "eating"
            val updatedHunger = min(100f, current.hunger + 25f)
            val updatedCoins = current.coins + 8
            val updatedXP = current.xp + 10

            val newState = current.copy(
                hunger = updatedHunger,
                coins = updatedCoins,
                xp = updatedXP,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification("🍎 ¡Alimentaste a ${current.name} con deliciosa comida! Hambre +25, +8 monedas, +10 EXP.", "care")

            checkForLevelUp(newState)

            delay(2500)
            _activeInteraction.value = "none"
        }
    }

    // Care action: Dormir (Sleep Toggle)
    fun toggleSleep() {
        val current = _petStateLocal.value ?: return
        
        viewModelScope.launch {
            val isEnteringSleep = !current.isSleeping
            val newState = current.copy(
                isSleeping = isEnteringSleep,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)

            if (isEnteringSleep) {
                repository.insertNotification("💤 Apagaste las luces. ${current.name} está durmiendo plácidamente... Zzz", "care")
                _activeInteraction.value = "sleeping"
            } else {
                repository.insertNotification("☀️ ¡Despertaste a ${current.name}!", "care")
                _activeInteraction.value = "none"
            }
        }
    }

    // Care action: Bañar (Clean / Bath)
    fun cleanPet() {
        val current = _petStateLocal.value ?: return
        if (current.isSleeping) return

        viewModelScope.launch {
            _activeInteraction.value = "cleaning"
            val updatedHappiness = min(100f, current.happiness + 10f)
            val updatedCoins = current.coins + 15
            val updatedXP = current.xp + 8

            val newState = current.copy(
                happiness = updatedHappiness,
                coins = updatedCoins,
                xp = updatedXP,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification("🛁 ¡Bañaste y cepillaste a ${current.name}! Se siente refrescante. +15 monedas, +8 EXP.", "care")

            checkForLevelUp(newState)

            delay(2500)
            _activeInteraction.value = "none"
        }
    }

    // Mini-game Management: Interactive Box Guess Game
    fun startMiniGame() {
        if (_gameActive.value) return
        _gameActive.value = true
        _gameFeedback.value = "Elige una caja misteriosa para encontrar el tesoro oculto..."
        _gameTargetId.value = (1..3).random()
    }

    fun makeMove(boxId: Int) {
        if (!_gameActive.value) return
        val current = _petStateLocal.value ?: return

        viewModelScope.launch {
            if (boxId == _gameTargetId.value) {
                // Correct guess!
                val rewardCoins = 30
                val rewardXP = 20
                val happinessGain = 25f

                _gameFeedback.value = "🎉 ¡Felicidades! Encontraste el Cofre de Oro en la Caja $boxId 👑"
                
                val newState = current.copy(
                    coins = current.coins + rewardCoins,
                    xp = current.xp + rewardXP,
                    happiness = min(100f, current.happiness + happinessGain),
                    lastUpdateTime = System.currentTimeMillis()
                )

                _petStateLocal.value = newState
                repository.updatePetState(newState)
                repository.insertNotification("🎮 ¡Mochi se divirtió jugando! Encontraron el tesoro juntos. +30 monedas, +20 EXP.", "care")
                
                checkForLevelUp(newState)
            } else {
                // Wrong guess
                val rewardCoins = 8 // small consolidation prize
                val rewardXP = 5
                val happinessGain = 10f

                _gameFeedback.value = "💨 ¡Oh no! La Caja $boxId estaba vacía. El cofre estaba en la Caja ${_gameTargetId.value}."

                val newState = current.copy(
                    coins = current.coins + rewardCoins,
                    xp = current.xp + rewardXP,
                    happiness = min(100f, current.happiness + happinessGain),
                    lastUpdateTime = System.currentTimeMillis()
                )

                _petStateLocal.value = newState
                repository.updatePetState(newState)
                repository.insertNotification("🎮 Jugaste con ${current.name}, aunque no ganaron el premio gordo, se divirtió. +8 monedas, +5 EXP.", "care")
                
                checkForLevelUp(newState)
            }

            // Keep the results visible for 4 seconds then close game state automatically
            delay(4000)
            _gameActive.value = false
            _gameFeedback.value = ""
        }
    }

    // Shop actions
    fun purchaseItem(item: ShopItem) {
        val current = _petStateLocal.value ?: return
        if (current.coins < item.price) {
            viewModelScope.launch {
                repository.insertNotification("❌ ¡No tienes suficientes monedas para comprar ${item.displayName}!", "info")
            }
            return
        }

        viewModelScope.launch {
            val updatedCoins = current.coins - item.price
            val updatedItem = item.copy(isUnlocked = true)
            
            repository.updateShopItem(updatedItem)
            
            val newState = current.copy(
                coins = updatedCoins
            )
            _petStateLocal.value = newState
            repository.updatePetState(newState)
            
            repository.insertNotification("🛍️ ¡Compraste con éxito: ${item.displayName}! Monedas restantes: $updatedCoins", "unlock")
        }
    }

    fun equipItem(item: ShopItem) {
        if (!item.isUnlocked) return
        val current = _petStateLocal.value ?: return

        viewModelScope.launch {
            val newState = if (item.type == "color") {
                val skinName = item.itemId.removePrefix("color_")
                // Map ID to readable color name
                val capSkin = skinName.replaceFirstChar { it.uppercase() }
                current.copy(skinColor = capSkin)
            } else {
                val accName = item.itemId.removePrefix("acc_")
                current.copy(equippedAccessory = accName)
            }

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification("✨ Equipaste: ${item.displayName} con éxito.", "info")
        }
    }

    fun unequipAccessory() {
        val current = _petStateLocal.value ?: return
        viewModelScope.launch {
            val newState = current.copy(equippedAccessory = "none")
            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification("✨ Quitaste el accesorio equipado.", "info")
        }
    }

    // Level-up checking system
    private suspend fun checkForLevelUp(state: PetState) {
        val xpRequired = state.level * 100
        if (state.xp >= xpRequired) {
            val newLevel = state.level + 1
            val leftOverXP = state.xp - xpRequired
            val levelUpBonus = 100 // Extra bonus coins
            
            // Check if this level correlates to a milestone
            val showEvolution = when {
                newLevel == 3 && state.evolutionStage == "Bebé" -> true
                newLevel == 7 && state.evolutionStage == "Niño" -> true
                newLevel == 12 && state.evolutionStage == "Adolescente" -> true
                else -> false
            }

            val nextStage = when {
                newLevel >= 12 -> "Adulto"
                newLevel >= 7 -> "Adolescente"
                newLevel >= 3 -> "Niño"
                else -> state.evolutionStage
            }

            val evolutionMsg = if (showEvolution) {
                "🌟 ¡${state.name} ha subido al Nivel $newLevel y está listo para elegir su camino de EVOLUCIÓN DINÁMICA! Elige en la pantalla principal."
            } else {
                "🎉 ¡${state.name} subió al Nivel $newLevel! ¡Qué bien está creciendo!"
            }

            val newState = state.copy(
                level = newLevel,
                xp = leftOverXP,
                coins = state.coins + levelUpBonus,
                evolutionStage = if (!showEvolution) nextStage else state.evolutionStage, // keep stage until path is chosen if choice is active
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            
            repository.insertNotification(evolutionMsg, "evolution")
            sendSystemNotification("✨ ¡Subida de Nivel!", "¡${state.name} alcanzó el nivel $newLevel y ganó monedas de recompensa!")

            if (showEvolution) {
                _pendingEvolutionChoice.value = true
            }
        }
    }

    // Run dynamic evolution based on choice selection
    fun evolvePet(chosenPath: String) {
        val current = _petStateLocal.value ?: return
        _pendingEvolutionChoice.value = false

        viewModelScope.launch {
            // Determine new stage name based on current level milestone
            val isBabyToChild = current.level >= 3 && current.evolutionStage == "Bebé"
            val isChildToTeen = current.level >= 7 && current.evolutionStage == "Niño"
            val isTeenToAdult = current.level >= 12 && current.evolutionStage == "Adolescente"

            val nextStage = when {
                isTeenToAdult -> "Adulto"
                isChildToTeen -> "Adolescente"
                isBabyToChild -> "Niño"
                else -> current.evolutionStage
            }

            // Nice readable display name of evolved form in Spanish
            val evolvedFormName = getEvolvedFormName(nextStage, chosenPath)

            val newState = current.copy(
                evolutionStage = nextStage,
                evolutionPath = chosenPath,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)

            val congratsMessage = "🔥🧬 ¡Fascinante! Tu mascota ha evolucionado a [$evolvedFormName] (Nivel ${current.level}) eligiendo el sendero de [$chosenPath]!"
            repository.insertNotification(congratsMessage, "evolution")
            sendSystemNotification("⚡ ¡Evolución Completa!", "${current.name} evolucionó exitosamente en un $evolvedFormName.")
        }
    }

    fun dismissEvolutionModal() {
        _pendingEvolutionChoice.value = false
    }

    private fun getEvolvedFormName(stage: String, path: String): String {
        return when (stage) {
            "Niño" -> when (path) {
                "Fuego" -> "Fugaz Ígneo 🔥"
                "Agua" -> "Gota Cristalina 💧"
                "Natura" -> "Retoño Brote 🌿"
                "Cosmos" -> "Polvillo de Estrellas 🌌"
                else -> "Mochi Niño"
            }
            "Adolescente" -> when (path) {
                "Fuego" -> "Fénix Errante 🔥"
                "Agua" -> "Tritón Sabio 💧"
                "Natura" -> "Guardián del Bosque 🌿"
                "Cosmos" -> "Estela Estelar 🌌"
                else -> "Mochi Adolescente"
            }
            "Adulto" -> when (path) {
                "Fuego" -> "Dragón del Sol Divino 🐲☀️"
                "Agua" -> "Leviatán de los Abisales 🐉🌊"
                "Natura" -> "Espíritu Eterno de Gaia 🌳✨"
                "Cosmos" -> "Arcano del Vacío Celestial 🌟🪐"
                else -> "Señor Mochi"
            }
            else -> "Mochi Bebé"
        }
    }

    // Rename pet
    fun renamePet(newName: String) {
        val current = _petStateLocal.value ?: return
        if (newName.isBlank()) return

        viewModelScope.launch {
            val newState = current.copy(name = newName.trim())
            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification("✏️ Cambiaste el nombre de tu mascota a: $newName.", "info")
        }
    }

    // Hatch egg into a newborn pet
    fun hatchEgg(hatchName: String, selectedGender: String) {
        val current = _petStateLocal.value ?: return
        val finalName = if (hatchName.isNotBlank()) hatchName.trim() else "Mochi"
        val finalGender = if (selectedGender == "Macho" || selectedGender == "Hembra") selectedGender else "Macho"
        
        viewModelScope.launch {
            val newState = current.copy(
                name = finalName,
                gender = finalGender,
                evolutionStage = "Bebé",
                hunger = 85f,
                sleep = 85f,
                happiness = 85f,
                lastUpdateTime = System.currentTimeMillis()
            )
            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification("🐣 ¡Felicidades! Ha nacido tu mascota virtual Mochi ($finalGender), un tierno Bebé. ¡Cuídalo con mucho amor!", "evolution")
            sendSystemNotification("🐣 ¡Huevo Eclocionado!", "¡Ha nacido $finalName! Qué emoción.")
        }
    }

    // Clear alerts log safely
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }
}

class ViewModelFactory(
    private val application: Application,
    private val repository: PetRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PetViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
