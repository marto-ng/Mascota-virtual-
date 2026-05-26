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
import com.example.data.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class PetViewModel(
    application: Application,
    private val repository: PetRepository
) : AndroidViewModel(application) {

    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

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

    // Sync telemetry states for external server
    private val _syncStatus = MutableStateFlow("Listo para sincronizar")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncTelemetry = MutableStateFlow<List<String>>(emptyList())
    val syncTelemetry: StateFlow<List<String>> = _syncTelemetry.asStateFlow()

    private val PREFS_NAME = "mochi_user_prefs"
    private val KEY_SAVED_USER = "saved_username"
    private val KEY_SAVED_SERVER_URL = "saved_server_url"

    private val _serverUrlInput = MutableStateFlow("https://mochivirtualpet-default-rtdb.firebaseio.com")
    val serverUrlInput: StateFlow<String> = _serverUrlInput.asStateFlow()

    fun updateServerUrlInput(newUrl: String) {
        _serverUrlInput.value = newUrl
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_SERVER_URL, newUrl).apply()
    }

    private fun getRemoteUrl(path: String): String {
        var baseUrl = _serverUrlInput.value.trim()
        if (baseUrl.isEmpty() || baseUrl.contains("jsonplaceholder")) {
            baseUrl = "https://mochivirtualpet-default-rtdb.firebaseio.com"
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }
        return "$baseUrl/$path.json"
    }

    fun autoUploadToCloud() {
        val current = _petStateLocal.value ?: return
        val url = getRemoteUrl("pets/${current.ownerUsername}")
        if (url.contains("jsonplaceholder")) {
            return
        }

        viewModelScope.launch {
            try {
                val response = com.example.network.MochiSyncClient.service.uploadPetState(url, current)
                if (response.isSuccessful) {
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    _syncStatus.value = "¡Sincronizado!"
                    _syncTelemetry.value = _syncTelemetry.value.takeLast(12) + listOf(
                        "☁️ [Auto-Sync $timeStr] ✅ Mascota guardada automáticamente en la nube."
                    )
                }
            } catch (e: Exception) {
                Log.e("PetViewModel", "Auto-upload pet state failed: ${e.message}")
            }
        }
    }

    private var decayJob: Job? = null
    private var petStateJob: Job? = null
    private var notificationsJob: Job? = null
    private val CHANNEL_ID = "pet_care_notifications"

    // Cooldown and limiting tracking for care actions
    private var lastStrokeTime: Long = 0L
    private var lastFeedTime: Long = 0L
    private var lastCleanTime: Long = 0L

    init {
        createNotificationChannel()

        // Load saved server URL from SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedServerUrl = prefs.getString(KEY_SAVED_SERVER_URL, "https://mochivirtualpet-default-rtdb.firebaseio.com") ?: "https://mochivirtualpet-default-rtdb.firebaseio.com"
        _serverUrlInput.value = savedServerUrl

        // Collect shop items (shop items are global and shared across users)
        viewModelScope.launch {
            repository.allShopItems
                .catch { Log.e("PetViewModel", "Error fetching shop items: ${it.message}") }
                .collectLatest { items ->
                    _shopItems.value = items
                }
        }

        // Auto login check: check if there's a stored authenticated user from a previous session
        val savedUser = prefs.getString(KEY_SAVED_USER, null)
        if (savedUser != null) {
            _currentUser.value = savedUser
            startUserSession(savedUser)
        }

        // Start active periodic decay loop (runs every 10 seconds)
        startDecayLoop()

        // Flow debouncer to automatically upload state changes to Firebase in background without blocking or spamming
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch {
            _petStateLocal
                .filterNotNull()
                .debounce(2000)
                .collectLatest { latestState ->
                    val url = getRemoteUrl("pets/${latestState.ownerUsername}")
                    if (url.contains("jsonplaceholder")) return@collectLatest
                    try {
                        com.example.network.MochiSyncClient.service.uploadPetState(url, latestState)
                        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        _syncStatus.value = "¡Sincronizado!"
                        _syncTelemetry.value = _syncTelemetry.value.takeLast(12) + listOf(
                            "☁️ [Auto-Sync $timeStr] Mascota guardada en tiempo real automáticamente."
                        )
                    } catch (e: Exception) {
                        Log.e("PetViewModel", "Debounced auto-upload failed: ${e.message}")
                    }
                }
        }
    }

    fun changeScreen(index: Int) {
        _currentScreen.value = index
    }

    // ==========================================
    // USER AUTH SESSION MANAGEMENT
    // ==========================================
    fun login(email: String, passwordHash: String, onResult: (Boolean) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || passwordHash.isEmpty()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            // 1. Try cloud verification first to enable cross-device synchronization out-of-the-box
            try {
                val cloudUrl = getRemoteUrl("users")
                val response = com.example.network.MochiSyncClient.service.downloadAllUsers(cloudUrl)
                if (response.isSuccessful) {
                    val usersMap = response.body()
                    if (usersMap != null) {
                        val matchingUser = usersMap.values.find {
                            it.email.trim().lowercase() == cleanEmail && it.passwordHash == passwordHash
                        }
                        if (matchingUser != null) {
                            // Perfect match! Download their virtual pet state or merge
                            val petUrl = getRemoteUrl("pets/${matchingUser.username}")
                            val petResponse = com.example.network.MochiSyncClient.service.downloadPetState(petUrl)
                            val cloudPet = if (petResponse.isSuccessful) petResponse.body() else null
                            
                            // Synchronize Cloud data down into local SQLite Database via Room!
                            repository.insertDownloadedUserAndPet(matchingUser, cloudPet)

                            // Save user to Local session
                            val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            prefs.edit().putString(KEY_SAVED_USER, matchingUser.username).apply()

                            _currentUser.value = matchingUser.username
                            startUserSession(matchingUser.username)
                            
                            _syncStatus.value = "¡Sincronizado!"
                            _syncTelemetry.value = _syncTelemetry.value + listOf(
                                "✅ ¡Sincronizado con la Nube!",
                                "🎮 Mascota de ${matchingUser.username} recuperada con éxito."
                            )
                            onResult(true)
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PetViewModel", "Cloud login lookup failed, falling back to offline local login: ${e.message}")
            }

            // 2. Offline Fallback: check local Room database
            val localUser = repository.loginUser(email.trim(), passwordHash)
            if (localUser != null) {
                val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_SAVED_USER, localUser.username).apply()

                _currentUser.value = localUser.username
                startUserSession(localUser.username)
                
                _syncStatus.value = "Offline Mode"
                _syncTelemetry.value = _syncTelemetry.value + listOf(
                    "⚠️ Sin conexión, usando sesión local fuera de línea."
                )
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun register(username: String, email: String, passwordHash: String, onResult: (Boolean, String?) -> Unit) {
        val cleanName = username.trim()
        val cleanEmail = email.trim()
        if (cleanName.length < 3) {
            onResult(false, "El nombre de usuario debe tener al menos 3 caracteres.")
            return
        }
        if (cleanEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onResult(false, "Por favor, ingresa un correo electrónico válido.")
            return
        }
        if (passwordHash.length < 4) {
            onResult(false, "La clave debe tener al menos 4 caracteres.")
            return
        }
        viewModelScope.launch {
            val errorMsg = repository.registerUser(User(email = cleanEmail, username = cleanName, passwordHash = passwordHash))
            if (errorMsg == null) {
                // Save user to Local session preferences
                val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_SAVED_USER, cleanName).apply()

                _currentUser.value = cleanName
                startUserSession(cleanName)

                // 3. Automatically upload new User Details and Egg PetState to Cloud Database!
                viewModelScope.launch {
                    try {
                        val userUrl = getRemoteUrl("users/$cleanName")
                        com.example.network.MochiSyncClient.service.uploadUser(userUrl, User(email = cleanEmail, username = cleanName, passwordHash = passwordHash))
                    } catch (e: Exception) {
                        Log.e("PetViewModel", "Auto cloud register failed: ${e.message}")
                    }
                }

                viewModelScope.launch {
                    try {
                        kotlinx.coroutines.delay(800)
                        val petUrl = getRemoteUrl("pets/$cleanName")
                        val petState = repository.getPetStateOnceForUser(cleanName)
                        if (petState != null) {
                            com.example.network.MochiSyncClient.service.uploadPetState(petUrl, petState)
                        }
                    } catch (e: Exception) {
                        Log.e("PetViewModel", "Auto cloud pet upload on register failed: ${e.message}")
                    }
                }

                _syncStatus.value = "¡Sincronizado!"
                onResult(true, null)
            } else {
                onResult(false, errorMsg)
            }
        }
    }

    fun logout() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SAVED_USER).apply()

        _currentUser.value = null
        _petStateLocal.value = null
        _notifications.value = emptyList()
        _currentScreen.value = 0
        petStateJob?.cancel()
        notificationsJob?.cancel()
    }

    private fun startUserSession(username: String) {
        // Reset subviews to main home page
        _currentScreen.value = 0
        _petStateLocal.value = null

        // Collect individual pet state
        petStateJob?.cancel()
        petStateJob = viewModelScope.launch {
            repository.getPetStateForUser(username).collectLatest { dbState ->
                if (dbState != null) {
                    if (_petStateLocal.value == null) {
                        // First load: perform time-based offline decay calculation
                        val elapsedState = calculateOfflineDecay(dbState)
                        _petStateLocal.value = elapsedState
                        repository.updatePetState(elapsedState)
                    } else {
                        // Keep synced
                        _petStateLocal.value = dbState
                    }
                }
            }
        }

        // Collect individual notifications
        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            repository.getAllNotificationsForUser(username)
                .catch { Log.e("PetViewModel", "Error fetching notifications: ${it.message}") }
                .collectLatest { notifyList ->
                    _notifications.value = notifyList
                }
        }
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
            .setSmallIcon(android.R.drawable.stat_notify_chat)
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
            if (state.isSleeping && elapsedMs > 0) {
                val hours = elapsedMs / 3600000.0
                val sleepRecovered = (hours * 22f).toFloat()
                var newSleep = min(100f, state.sleep + sleepRecovered)
                var wasSleeping = state.isSleeping
                if (newSleep >= 100f) {
                    newSleep = 100f
                    wasSleeping = false
                }
                
                val hungerDecay = (hours * 4.5f).toFloat()
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

        val cappedMs = min(elapsedMs, 12 * 3600000L)
        val hours = cappedMs / 3600000.0

        val hungerDecay = (hours * 6.5).toFloat()
        val sleepDecay = (hours * 4.5).toFloat()

        val newHunger = max(0f, state.hunger - hungerDecay)
        val newSleep = max(0f, state.sleep - sleepDecay)

        val overallWellness = (newHunger + newSleep) / 2f
        val happinessDecay = if (overallWellness < 40f) {
            (hours * 10f).toFloat()
        } else {
            (hours * 2f).toFloat()
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
                delay(10000)
                val current = _petStateLocal.value ?: continue
                
                if (current.evolutionStage == "Huevo") {
                    continue
                }
                
                var hunger = current.hunger
                var sleep = current.sleep
                var happiness = current.happiness
                var health = current.health
                var hygiene = current.hygiene
                var isSleeping = current.isSleeping
                var currentEmotion = current.currentEmotion

                if (isSleeping) {
                    sleep = min(100f, sleep + 2.5f)
                    if (sleep >= 100f) {
                        isSleeping = false
                        viewModelScope.launch {
                            repository.insertNotification(current.ownerUsername, "💤 ¡${current.name} se ha despertado totalmente recuperado y lleno de energía!", "info")
                            sendSystemNotification("⚡ ¡Mascota Despierta!", "¡${current.name} está lleno de energía!")
                        }
                    }
                    hunger = max(0f, hunger - 0.2f)
                    hygiene = max(0f, hygiene - 0.1f)
                } else {
                    hunger = max(0f, hunger - 0.4f)
                    sleep = max(0f, sleep - 0.3f)
                    hygiene = max(0f, hygiene - 0.5f)
                }

                // Low hygiene affects happiness negatively
                if (hygiene < 30f) {
                    happiness = max(0f, happiness - 0.5f)
                }

                // Health decay / recovery rules based on general care quality
                if (hunger < 15f || sleep < 15f || hygiene < 15f) {
                    health = max(0f, health - 1.2f)
                } else {
                    health = min(100f, health + 0.4f)
                }

                // Sick emotional state auto-activation
                if (health < 30f && currentEmotion != "Enfermo") {
                    currentEmotion = "Enfermo"
                    viewModelScope.launch {
                        repository.insertNotification(current.ownerUsername, "🤒 ¡Oh no! ${current.name} se siente enfermo por falta de cuidados. ¡Dale medicina pronto!", "alert")
                    }
                } else if (health >= 80f && currentEmotion == "Enfermo") {
                    currentEmotion = "Normal"
                }

                // Dynamic Passive Emotion calculations based on care and stats:
                if (isSleeping) {
                    currentEmotion = "Durmiendo profundamente"
                } else if (_activeInteraction.value != "none") {
                    // Retain current interaction-triggered emotion
                } else {
                    currentEmotion = when {
                        health < 30f -> "Enfermo"
                        hunger < 25f -> {
                            if (currentEmotion != "Enojo") {
                                viewModelScope.launch {
                                    repository.insertNotification(current.ownerUsername, "😡 ¡${current.name} está de mal humor (Enojo) porque no tiene comida!", "alert")
                                }
                            }
                            "Enojo"
                        }
                        hygiene < 25f -> {
                            if (currentEmotion != "Aburrimiento") {
                                viewModelScope.launch {
                                    repository.insertNotification(current.ownerUsername, "😑 ¡${current.name} se siente aburrido y decaído porque está sucio. ¡Dale un buen baño!", "info")
                                }
                            }
                            "Aburrimiento"
                        }
                        happiness < 30f -> {
                            if (currentEmotion != "Soledad") {
                                viewModelScope.launch {
                                    repository.insertNotification(current.ownerUsername, "🥺 ¡${current.name} siente Soledad y tristeza. ¡Dale mimos o juega con él!", "info")
                                }
                            }
                            "Soledad"
                        }
                        // If everything is healthy, randomize display of interactive states!
                        happiness > 50f && hunger > 50f && hygiene > 50f && health > 50f -> {
                            val interactiveEmotions = listOf("Normal", "Bailando", "Cantando", "Pensando", "Explorando", "Esperando al jugador")
                            interactiveEmotions.random()
                        }
                        // Default fallback: if previously sad or angry, and stats recovered, go back to Normal
                        currentEmotion == "Enojo" || currentEmotion == "Aburrimiento" || currentEmotion == "Soledad" || currentEmotion == "Durmiendo profundamente" -> {
                            "Normal"
                        }
                        else -> currentEmotion // Preserve existing active state (singing, dancing, petting, etc.)
                    }
                }

                // If the pet is performing interactive activities or active emotional states, decrease energy (sleep)
                val activeActivities = setOf("Bailando", "Cantando", "Pensando", "Explorando", "Jugando", "Esperando al jugador", "Emoción")
                if (currentEmotion in activeActivities && !isSleeping) {
                    sleep = max(0f, sleep - 1.2f) // Extra energy decrease for performing activities/interactive states
                }

                val combinedStats = (hunger + sleep + hygiene + health) / 4f
                if (combinedStats < 30f) {
                    happiness = max(0f, happiness - 0.8f)
                } else if (combinedStats > 75f) {
                    happiness = min(100f, happiness + 0.3f)
                }

                triggerSmartWarnings(current, hunger, sleep, combinedStats)

                val updated = current.copy(
                    hunger = hunger,
                    sleep = sleep,
                    happiness = happiness,
                    health = health,
                    hygiene = hygiene,
                    currentEmotion = currentEmotion,
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
                repository.insertNotification(pet.ownerUsername, "🚨 ¡⚠️ Alerta! ${pet.name} tiene demasiada hambre. ¡Dale de comer algo rico pronto!", "alert")
                sendSystemNotification("🍖 ¡Tengo Hambre!", "¡Por favor, alimenta a ${pet.name}! Está perdiendo felicidad.")
            } else if (currentHunger >= 30f) {
                hungerAlertSent = false
            }

            if (currentSleep < 20f && !sleepAlertSent && !pet.isSleeping) {
                sleepAlertSent = true
                repository.insertNotification(pet.ownerUsername, "🚨 ¡⚠️ Alerta! ${pet.name} está exhausto y necesita dormir.", "alert")
                sendSystemNotification("😴 ¡Tengo Sueño!", "¡Pon a dormir a ${pet.name} para que recupere su energía!")
            } else if (currentSleep >= 30f) {
                sleepAlertSent = false
            }
        }
    }

    fun strokePet() {
        val current = _petStateLocal.value ?: return
        if (current.isSleeping) return

        // 1. Limit if happiness is already high
        if (current.happiness >= 95f) {
            viewModelScope.launch {
                repository.insertNotification(current.ownerUsername, "❤️ ¡${current.name} ya está sumamente feliz y lleno de mimos!", "info")
            }
            return
        }

        // 2. Cooldown check
        val now = System.currentTimeMillis()
        if (now - lastStrokeTime < 8000) {
            viewModelScope.launch {
                repository.insertNotification(current.ownerUsername, "⏳ ¡Dale un respiro a ${current.name}! Espera unos segundos entre mimos.", "info")
            }
            return
        }
        lastStrokeTime = now

        viewModelScope.launch {
            _activeInteraction.value = "petting"
            val updatedHappiness = min(100f, current.happiness + 15f)
            val updatedSleep = max(0f, current.sleep - 2f)
            val updatedCoins = current.coins + 5
            val updatedXP = current.xp + 5

            val newState = current.copy(
                happiness = updatedHappiness,
                sleep = updatedSleep,
                currentEmotion = "Cariño/amor",
                coins = updatedCoins,
                xp = updatedXP,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification(current.ownerUsername, "❤️ ¡Le diste mimos a ${current.name}! Ganaste 5 monedas y 5 EXP.", "care")
            
            checkForLevelUp(newState)

            delay(2500)
            _activeInteraction.value = "none"
        }
    }

    fun feedPet() {
        val current = _petStateLocal.value ?: return
        if (current.isSleeping) return

        // 1. Limit if hunger is already high (already full)
        if (current.hunger >= 90f) {
            viewModelScope.launch {
                repository.insertNotification(current.ownerUsername, "🍪 ¡${current.name} ya está satisfecho y lleno de energía! (Hambre: ${current.hunger.toInt()}%)", "info")
            }
            return
        }

        // 2. Cooldown check
        val now = System.currentTimeMillis()
        if (now - lastFeedTime < 10000) {
            viewModelScope.launch {
                repository.insertNotification(current.ownerUsername, "⏳ Deja que ${current.name} termine de masticar antes del siguiente bocado.", "info")
            }
            return
        }
        lastFeedTime = now

        viewModelScope.launch {
            _activeInteraction.value = "eating"
            val updatedHunger = min(100f, current.hunger + 25f)
            val updatedHealth = min(100f, current.health + 5f)
            val updatedSleep = max(0f, current.sleep - 1f)
            val updatedCoins = current.coins + 8
            val updatedXP = current.xp + 10

            val newState = current.copy(
                hunger = updatedHunger,
                health = updatedHealth,
                sleep = updatedSleep,
                currentEmotion = "Emoción",
                coins = updatedCoins,
                xp = updatedXP,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification(current.ownerUsername, "🍎 ¡Alimentaste a ${current.name} con deliciosa comida! Hambre +25, +8 monedas, +10 EXP.", "care")

            checkForLevelUp(newState)

            delay(2500)
            _activeInteraction.value = "none"
        }
    }

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
                repository.insertNotification(current.ownerUsername, "💤 Apagaste las luces. ${current.name} está durmiendo plácidamente... Zzz", "care")
                _activeInteraction.value = "sleeping"
            } else {
                repository.insertNotification(current.ownerUsername, "☀️ ¡Despertaste a ${current.name}!", "care")
                _activeInteraction.value = "none"
            }
        }
    }

    fun cleanPet() {
        val current = _petStateLocal.value ?: return
        if (current.isSleeping) return

        // 1. Cooldown check
        val now = System.currentTimeMillis()
        if (now - lastCleanTime < 20000) {
            viewModelScope.launch {
                repository.insertNotification(current.ownerUsername, "🧼 ¡${current.name} ya está impecable y reluciente! No necesita otro baño tan pronto.", "info")
            }
            return
        }
        lastCleanTime = now

        viewModelScope.launch {
            _activeInteraction.value = "cleaning"
            val updatedHappiness = min(100f, current.happiness + 10f)
            val updatedSleep = max(0f, current.sleep - 4f)
            val updatedCoins = current.coins + 15
            val updatedXP = current.xp + 8

            val newState = current.copy(
                happiness = updatedHappiness,
                hygiene = 100f,
                sleep = updatedSleep,
                currentEmotion = "Bailando",
                coins = updatedCoins,
                xp = updatedXP,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification(current.ownerUsername, "🛁 ¡Bañaste y cepillaste a ${current.name}! Se siente refrescante. +15 monedas, +8 EXP.", "care")

            checkForLevelUp(newState)

            delay(2500)
            _activeInteraction.value = "none"
        }
    }

    fun startMiniGame() {
        if (_gameActive.value) return
        _gameActive.value = true
        _gameFeedback.value = "Elige una caja misteriosa para encontrar el tesoro..."
        _gameTargetId.value = (1..3).random()
        
        val current = _petStateLocal.value
        if (current != null) {
            viewModelScope.launch {
                val newState = current.copy(
                    currentEmotion = "Jugando",
                    lastUpdateTime = System.currentTimeMillis()
                )
                _petStateLocal.value = newState
                repository.updatePetState(newState)
            }
        }
    }

    fun makeMove(boxId: Int) {
        if (!_gameActive.value) return
        val current = _petStateLocal.value ?: return

        viewModelScope.launch {
            if (boxId == _gameTargetId.value) {
                val rewardCoins = 30
                val rewardXP = 20
                val happinessGain = 25f

                _gameFeedback.value = "🎉 ¡Felicidades! Encontraste el Cofre de Oro en la Caja $boxId 👑"
                val updatedSleep = max(0f, current.sleep - 12f)
                
                val newState = current.copy(
                    coins = current.coins + rewardCoins,
                    xp = current.xp + rewardXP,
                    sleep = updatedSleep,
                    happiness = min(100f, current.happiness + happinessGain),
                    currentEmotion = "Cantando",
                    lastUpdateTime = System.currentTimeMillis()
                )

                _petStateLocal.value = newState
                repository.updatePetState(newState)
                repository.insertNotification(current.ownerUsername, "🎮 ¡Mochi se divirtió jugando! Encontraron el tesoro juntos y gastó algo de energía. +30 monedas, +20 EXP.", "care")
                
                checkForLevelUp(newState)
            } else {
                val rewardCoins = 8
                val rewardXP = 5
                val happinessGain = 10f

                _gameFeedback.value = "💨 ¡Oh no! La Caja $boxId estaba vacía. El cofre estaba en la Caja ${_gameTargetId.value}."
                val updatedSleep = max(0f, current.sleep - 8f)

                val newState = current.copy(
                    coins = current.coins + rewardCoins,
                    xp = current.xp + rewardXP,
                    sleep = updatedSleep,
                    happiness = min(100f, current.happiness + happinessGain),
                    currentEmotion = "Confusión",
                    lastUpdateTime = System.currentTimeMillis()
                )

                _petStateLocal.value = newState
                repository.updatePetState(newState)
                repository.insertNotification(current.ownerUsername, "🎮 Jugaste con ${current.name}, gastó energía buscando. +8 monedas, +5 EXP.", "care")
                
                checkForLevelUp(newState)
            }

            delay(4000)
            _gameActive.value = false
            _gameFeedback.value = ""
        }
    }

    fun purchaseItem(item: ShopItem) {
        val current = _petStateLocal.value ?: return
        if (current.coins < item.price) {
            viewModelScope.launch {
                repository.insertNotification(current.ownerUsername, "❌ ¡No tienes suficientes monedas para comprar ${item.displayName}!", "info")
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
            
            repository.insertNotification(current.ownerUsername, "🛍️ ¡Compraste con éxito: ${item.displayName}! Monedas restantes: $updatedCoins", "unlock")
        }
    }

    fun equipItem(item: ShopItem) {
        if (!item.isUnlocked) return
        val current = _petStateLocal.value ?: return

        viewModelScope.launch {
            val newState = if (item.type == "color") {
                val skinName = item.itemId.removePrefix("color_")
                val capSkin = skinName.replaceFirstChar { it.uppercase() }
                current.copy(skinColor = capSkin)
            } else {
                val accName = item.itemId.removePrefix("acc_")
                current.copy(equippedAccessory = accName)
            }

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification(current.ownerUsername, "✨ Equipaste: ${item.displayName} con éxito.", "info")
        }
    }

    fun unequipAccessory() {
        val current = _petStateLocal.value ?: return
        viewModelScope.launch {
            val newState = current.copy(equippedAccessory = "none")
            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification(current.ownerUsername, "✨ Quitaste el accesorio equipado.", "info")
        }
    }

    private suspend fun checkForLevelUp(state: PetState) {
        val xpRequired = state.level * 100
        if (state.xp >= xpRequired) {
            val newLevel = state.level + 1
            val leftOverXP = state.xp - xpRequired
            val levelUpBonus = 100
            
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
                evolutionStage = if (!showEvolution) nextStage else state.evolutionStage,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            
            repository.insertNotification(state.ownerUsername, evolutionMsg, "evolution")
            sendSystemNotification("✨ ¡Subida de Nivel!", "¡${state.name} alcanzó el nivel $newLevel y ganó monedas de recompensa!")

            if (showEvolution) {
                _pendingEvolutionChoice.value = true
            }
        }
    }

    fun evolvePet(chosenPath: String) {
        val current = _petStateLocal.value ?: return
        _pendingEvolutionChoice.value = false

        viewModelScope.launch {
            val isBabyToChild = current.level >= 3 && current.evolutionStage == "Bebé"
            val isChildToTeen = current.level >= 7 && current.evolutionStage == "Niño"
            val isTeenToAdult = current.level >= 12 && current.evolutionStage == "Adolescente"

            val nextStage = when {
                isTeenToAdult -> "Adulto"
                isChildToTeen -> "Adolescente"
                isBabyToChild -> "Niño"
                else -> current.evolutionStage
            }

            val evolvedFormName = getEvolvedFormName(nextStage, chosenPath)

            val newState = current.copy(
                evolutionStage = nextStage,
                evolutionPath = chosenPath,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)

            val congratsMessage = "🔥🧬 ¡Fascinante! Tu mascota ha evolucionado a [$evolvedFormName] (Nivel ${current.level}) eligiendo el sendero de [$chosenPath]!"
            repository.insertNotification(current.ownerUsername, congratsMessage, "evolution")
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

    fun renamePet(newName: String) {
        val current = _petStateLocal.value ?: return
        if (newName.isBlank()) return

        viewModelScope.launch {
            val newState = current.copy(name = newName.trim())
            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification(current.ownerUsername, "✏️ Cambiaste el nombre de tu mascota a: $newName.", "info")
        }
    }

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
            repository.insertNotification(current.ownerUsername, "🐣 ¡Felicidades! Ha nacido tu mascota virtual Mochi ($finalGender), un tierno Bebé. ¡Cuídalo con mucho amor!", "evolution")
            sendSystemNotification("🐣 ¡Huevo Eclocionado!", "¡Ha nacido $finalName! Qué emoción.")
        }
    }

    fun clearLogs() {
        val username = _currentUser.value ?: return
        viewModelScope.launch {
            repository.clearAllNotificationsForUser(username)
        }
    }

    fun markAllRead() {
        val username = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsReadForUser(username)
        }
    }

    fun uploadMochiToCloud() {
        val current = _petStateLocal.value ?: return
        val username = _currentUser.value ?: return
        val url = getRemoteUrl("pets/$username")

        _syncStatus.value = "Subiendo..."
        _syncTelemetry.value = listOf(
            "⏳ Inicializando respaldo en la Nube...",
            "📍 Servidor Realtime URL: $url",
            "📦 Serializando estado actual..."
        )

        viewModelScope.launch {
            try {
                val petJson = try {
                    val adapter = com.example.network.MochiSyncClient.getMoshi().adapter(PetState::class.java)
                    adapter.toJson(current)
                } catch (e: Exception) {
                    "{ \"name\": \"${current.name}\", \"level\": ${current.level} }"
                }

                _syncTelemetry.value = _syncTelemetry.value + listOf(
                    "✏️ Datos a transferir: $petJson"
                )

                val response = com.example.network.MochiSyncClient.service.uploadPetState(url, current)
                if (response.isSuccessful) {
                    val code = response.code()
                    _syncStatus.value = "¡Éxito!"
                    _syncTelemetry.value = _syncTelemetry.value + listOf(
                        "✅ Sincronización exitosa con Realtime Database (Código $code)",
                        "☁️ Estado de tu mascota respaldado correctamente."
                    )
                    repository.insertNotification(
                        username,
                        "☁️ ¡Respaldo de ${current.name} guardado con éxito en la Nube!",
                        "info"
                    )
                } else {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    _syncStatus.value = "Error $code"
                    _syncTelemetry.value = _syncTelemetry.value + listOf(
                        "❌ Error de Servidor (Código $code)",
                        "⚠️ Detalle: $errorBody"
                    )
                }
            } catch (e: Exception) {
                _syncStatus.value = "Error de Red"
                _syncTelemetry.value = _syncTelemetry.value + listOf(
                    "❌ Excepción detectada: ${e.message}",
                    "🔌 Revisa tu conexión de internet o URL del servidor."
                )
            }
        }
    }

    fun downloadMochiFromCloud() {
        val current = _petStateLocal.value ?: return
        val username = _currentUser.value ?: return
        val url = getRemoteUrl("pets/$username")

        _syncStatus.value = "Descargando..."
        _syncTelemetry.value = listOf(
            "⏳ Solicitando datos a la base de datos remota...",
            "📍 URL Realtime: $url"
        )

        viewModelScope.launch {
            try {
                val response = com.example.network.MochiSyncClient.service.downloadPetState(url)
                if (response.isSuccessful) {
                    val downloadedState = response.body()
                    if (downloadedState != null) {
                        val updatedState = downloadedState.copy(ownerUsername = username, lastUpdateTime = System.currentTimeMillis())
                        repository.updatePetState(updatedState)
                        _petStateLocal.value = updatedState
                        _syncStatus.value = "¡Sincronizado!"
                        _syncTelemetry.value = _syncTelemetry.value + listOf(
                            "✅ Datos descargados (Código ${response.code()})",
                            "📝 Mascota: ${updatedState.name} (Nivel ${updatedState.level})",
                            "🔄 Mascota sincronizada en este dispositivo."
                        )
                        repository.insertNotification(
                            username,
                            "☁️ ¡Mascota cargada exitosamente de la Nube: ${updatedState.name}!",
                            "info"
                        )
                    } else {
                        _syncStatus.value = "Sin Datos"
                        _syncTelemetry.value = _syncTelemetry.value + listOf(
                            "⚠️ No se encontró ninguna mascota remota guardada para el usuario: $username"
                        )
                    }
                } else {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Detalles desconocidos"
                    _syncStatus.value = "Error $code"
                    _syncTelemetry.value = _syncTelemetry.value + listOf(
                        "❌ Fallo al descargar (Código de red: $code)",
                        "⚠️ Error: $errorBody"
                    )
                }
            } catch (e: Exception) {
                _syncStatus.value = "Error de Conexión"
                _syncTelemetry.value = _syncTelemetry.value + listOf(
                    "❌ Excepción en red: ${e.message}",
                    "🔌 Asegúrate de estar conectado a internet."
                )
            }
        }
    }

    fun changePetEmotion(emotion: String) {
        val current = _petStateLocal.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                currentEmotion = emotion,
                lastUpdateTime = System.currentTimeMillis()
            )
            _petStateLocal.value = updated
            repository.updatePetState(updated)
            repository.insertNotification(
                current.ownerUsername,
                "🎭 Cambiaste el estado de ánimo de ${current.name} a [$emotion].",
                "info"
            )
        }
    }

    fun healPet() {
        val current = _petStateLocal.value ?: return
        if (current.isSleeping) return

        if (current.coins < 15) {
            viewModelScope.launch {
                repository.insertNotification(current.ownerUsername, "❌ No tienes suficientes monedas para comprar medicina (requiere 15 g).", "info")
            }
            return
        }

        viewModelScope.launch {
            _activeInteraction.value = "healing"
            val updatedCoins = current.coins - 15
            val updatedXP = current.xp + 5

            val newState = current.copy(
                health = 100f,
                currentEmotion = if (current.currentEmotion == "Enfermo") "Normal" else current.currentEmotion,
                coins = updatedCoins,
                xp = updatedXP,
                lastUpdateTime = System.currentTimeMillis()
            )

            _petStateLocal.value = newState
            repository.updatePetState(newState)
            repository.insertNotification(current.ownerUsername, "❤️ ¡Le diste medicina y cuidados a ${current.name}! Salud restaurada al 100%. -15 monedas, +5 EXP.", "care")

            checkForLevelUp(newState)

            delay(2500)
            _activeInteraction.value = "none"
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
