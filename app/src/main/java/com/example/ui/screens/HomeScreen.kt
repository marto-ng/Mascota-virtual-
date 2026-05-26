package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.PetNotification
import com.example.data.PetState
import com.example.data.ShopItem
import com.example.ui.PetViewModel
import com.example.ui.components.PetCanvas
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PetViewModel,
    modifier: Modifier = Modifier
) {
    val petState by viewModel.petState.collectAsStateWithLifecycle()
    val shopItems by viewModel.shopItems.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val activeScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeInteraction by viewModel.activeInteraction.collectAsStateWithLifecycle()
    val pendingEvolutionChoice by viewModel.pendingEvolutionChoice.collectAsStateWithLifecycle()

    var showRenameDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    // Handheld console background theme colors (retro futuristic look!)
    val primaryBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E1B4B)  // Indigo 950
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (petState?.evolutionStage != "Huevo") {
                // High-fidelity dynamic bottom navigation bar with material pill indicators
                NavigationBar(
                    containerColor = Color(0xFF1E293B), // Slate 800
                    tonalElevation = 8.dp
                ) {
                NavigationBarItem(
                    selected = activeScreen == 0,
                    onClick = { viewModel.changeScreen(0) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Casa") },
                    label = { Text("Mochi", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFB082FF),
                        selectedTextColor = Color(0xFFB082FF),
                        indicatorColor = Color(0x33B180FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = activeScreen == 1,
                    onClick = { viewModel.changeScreen(1) },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Tienda") },
                    label = { Text("Tienda", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE9D5FF),
                        selectedTextColor = Color(0xFFE9D5FF),
                        indicatorColor = Color(0x33B180FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_shop")
                )
                NavigationBarItem(
                    selected = activeScreen == 2,
                    onClick = { viewModel.changeScreen(2) },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Juegos") },
                    label = { Text("Minijuegos", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE9D5FF),
                        selectedTextColor = Color(0xFFE9D5FF),
                        indicatorColor = Color(0x33B180FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_games")
                )
                NavigationBarItem(
                    selected = activeScreen == 3,
                    onClick = { viewModel.changeScreen(3) },
                    icon = { 
                        BadgedBox(
                            badge = {
                                val unreadCount = notifications.count { !it.isRead }
                                if (unreadCount > 0) {
                                    Badge(containerColor = Color(0xFFFF5252)) {
                                        Text(unreadCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Alertas")
                        }
                    },
                    label = { Text("Alertas", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE9D5FF),
                        selectedTextColor = Color(0xFFE9D5FF),
                        indicatorColor = Color(0x33B180FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_alerts")
                )
            }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(primaryBgBrush)
                .padding(innerPadding)
        ) {
            petState?.let { state ->
                if (state.evolutionStage == "Huevo") {
                    HatchingScreen(
                        state = state,
                        onHatch = { name, gender -> viewModel.hatchEgg(name, gender) }
                    )
                } else {
                    AnimatedContent(
                        targetState = activeScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenNavController"
                    ) { screenId ->
                        when (screenId) {
                            0 -> DashboardView(
                                state = state,
                                activeInteraction = activeInteraction,
                                onPetAction = { viewModel.strokePet() },
                                onFeedAction = { viewModel.feedPet() },
                                onSleepAction = { viewModel.toggleSleep() },
                                onCleanAction = { viewModel.cleanPet() },
                                onRenameClick = {
                                    inputName = state.name
                                    showRenameDialog = true
                                }
                            )
                            1 -> ShopView(
                                state = state,
                                shopItems = shopItems,
                                onBuyItem = { viewModel.purchaseItem(it) },
                                onEquipItem = { viewModel.equipItem(it) },
                                onUnequipAcc = { viewModel.unequipAccessory() }
                            )
                            2 -> MiniGameView(
                                viewModel = viewModel
                            )
                            3 -> NotificationHistoryView(
                                notifications = notifications,
                                onClearHistory = { viewModel.clearLogs() },
                                onMarkAllRead = { viewModel.markAllRead() }
                            )
                        }
                    }
                }

                // Dynamic Evolution Overlay Portal (Activated when leveling up hits a branching threshold)
                if (pendingEvolutionChoice) {
                    EvolutionPortalChoice(
                        state = state,
                        onEvolveSelect = { chosenPath ->
                            viewModel.evolvePet(chosenPath)
                        },
                        onDismiss = { viewModel.dismissEvolutionModal() }
                    )
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFB082FF))
            }
        }
    }

    // Modal dialogue to modify pet's name
    if (showRenameDialog) {
        Dialog(onDismissRequest = { showRenameDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, Color(0xFFB082FF)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Renombrar Mascota ✏️",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { if (it.length <= 16) inputName = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x1A000000),
                            unfocusedContainerColor = Color(0x1A000000),
                            focusedIndicatorColor = Color(0xFFB082FF)
                        ),
                        placeholder = { Text("Ingresa nuevo nombre...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().testTag("rename_input")
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { showRenameDialog = false },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Cancelar", color = Color.LightGray)
                        }
                        Button(
                            onClick = {
                                if (inputName.isNotBlank()) {
                                    viewModel.renamePet(inputName)
                                    showRenameDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB082FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("rename_confirm")
                        ) {
                            Text("Guardar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEWS & VIEW COMPONENTS
// ==========================================

@Composable
fun DashboardView(
    state: PetState,
    activeInteraction: String,
    onPetAction: () -> Unit,
    onFeedAction: () -> Unit,
    onSleepAction: () -> Unit,
    onCleanAction: () -> Unit,
    onRenameClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Master top card: Levels and Stats Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF475569)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Profile/Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.name + if (state.gender == "Macho") " ♂️" else if (state.gender == "Hembra") " ♀️" else "",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Renombrar",
                                    tint = Color(0xFFB082FF),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onRenameClick() }
                                )
                            }
                            // Evolution stage and class badge
                            Surface(
                                color = Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Fase: ${state.evolutionStage} (${state.evolutionPath})",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Level badge & coins counter
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = Color(0xFFB082FF),
                                shape = CircleShape
                            ) {
                                Text(
                                    "Nivel ${state.level}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Monedas",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${state.coins} g",
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // XP Level Progress Bar
                    val xpRequired = state.level * 100
                    val progressFraction = state.xp.toFloat() / xpRequired.toFloat()
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Experiencia (EXP)", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("${state.xp}/$xpRequired", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            color = Color(0xFFB082FF),
                            trackColor = Color(0xFF334155),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    }
                }
            }
        }

        // Animated screen frame centering Mochi (Handheld monitor look)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(4.dp, Color(0xFF334155)), RoundedCornerShape(32.dp))
                    .padding(8.dp)
            ) {
                // Screen Glass Glow Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x1F80DEEA),
                                    Color(0x05000000)
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sleep overlay mask
                        val contentColor = if (state.isSleeping) Color(0xCC050515) else Color.Transparent
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(contentColor)
                        ) {
                            // Dynamic canvas draw
                            PetCanvas(
                                petState = state,
                                activeInteraction = activeInteraction,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Status Mood Overlay Pill (Top center of display)
                            val moodLabel = when {
                                state.isSleeping -> "Zzz... Durmiendo 💤"
                                state.hunger < 25f -> "¡Tiene Hambre! 🍎😫"
                                state.sleep < 25f -> "¡Exhausto! Necesita dormir 😴"
                                state.happiness < 40f -> "Está Triste... ¡Dale mimos! 😢"
                                state.happiness > 75f -> "¡Súper Alegre y Sano! 🥰🌟"
                                else -> "Tranquilo y Feliz 🙂🌸"
                            }
                            Surface(
                                color = Color(0xCC1E293B),
                                border = BorderStroke(1.dp, Color(0x33B180FF)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    text = moodLabel,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live stats sliders (Hambre, Sueño, Felicidad)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF475569)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Estado de Necesidad",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    // Hunger item
                    StatMeterRow(
                        label = "Nutrición (Hambre)",
                        value = state.hunger,
                        activeColor = Color(0xFFFF7043),
                        icon = Icons.Default.ShoppingCart
                    )

                    // Sleep / Energy item
                    StatMeterRow(
                        label = "Energía (Sueño)",
                        value = state.sleep,
                        activeColor = Color(0xFF29B6F6),
                        icon = Icons.Default.Star
                    )

                    // Happiness / Core Mood
                    StatMeterRow(
                        label = "Felicidad",
                        value = state.happiness,
                        activeColor = Color(0xFF66BB6A),
                        icon = Icons.Default.Favorite
                    )
                }
            }
        }

        // Quad Interactive Command Center (Control Buttons with touch target respect)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Acciones de Cuidado",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Acariciar
                CareConsoleButton(
                    text = "Acariciar",
                    badge = "Mimos",
                    icon = Icons.Default.FavoriteBorder,
                    color = Color(0x33FF4081),
                    borderColor = Color(0xFFFF4081),
                    onClick = onPetAction,
                    enabled = !state.isSleeping,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_pet")
                )

                // Alimentar
                CareConsoleButton(
                    text = "Alimentar",
                    badge = "Fruta",
                    icon = Icons.Default.ShoppingCart,
                    color = Color(0x33FFA726),
                    borderColor = Color(0xFFFFA726),
                    onClick = onFeedAction,
                    enabled = !state.isSleeping && state.hunger < 100f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_feed")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dormir
                val sleepBtnText = if (state.isSleeping) "Despertar" else "Dormir"
                val sleepBtnIcon = if (state.isSleeping) Icons.Default.PlayArrow else Icons.Default.Lock
                val sleepBtnColor = if (state.isSleeping) Color(0x33FFD54F) else Color(0x333F51B5)
                val sleepBorder = if (state.isSleeping) Color(0xFFFFD54F) else Color(0xFF5C6BC0)
                CareConsoleButton(
                    text = sleepBtnText,
                    badge = if (state.isSleeping) "Sol" else "Luna",
                    icon = sleepBtnIcon,
                    color = sleepBtnColor,
                    borderColor = sleepBorder,
                    onClick = onSleepAction,
                    enabled = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_sleep")
                )

                // Bañar
                CareConsoleButton(
                    text = "Bañar",
                    badge = "Limpieza",
                    icon = Icons.Default.Refresh,
                    color = Color(0x3326A69A),
                    borderColor = Color(0xFF26A69A),
                    onClick = onCleanAction,
                    enabled = !state.isSleeping,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_clean")
                )
            }
        }
    }
}

@Composable
fun StatMeterRow(
    label: String,
    value: Float,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = activeColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("${value.toInt()}%", color = activeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { value / 100f },
                color = activeColor,
                trackColor = Color(0xFF334155),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

// Custom tactile feedback button styled like consoles
@Composable
fun CareConsoleButton(
    text: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    borderColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.4f
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) borderColor else Color(0xFF475569)
        ),
        modifier = modifier
            .height(72.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = color,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = borderColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = alpha)
                )
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8).copy(alpha = alpha)
                )
            }
        }
    }
}

// ==========================================
// SHOP / STYLING VIEW
// ==========================================

@Composable
fun ShopView(
    state: PetState,
    shopItems: List<ShopItem>,
    onBuyItem: (ShopItem) -> Unit,
    onEquipItem: (ShopItem) -> Unit,
    onUnequipAcc: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Skins/Colores, 1: Accesorios

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Shop header summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Boutique de Mochi 🛍️",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Personaliza apariencias y colores",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            // Balance bubble
            Surface(
                color = Color(0xFF334155),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Monedas",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${state.coins} g",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dual item tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFFB082FF)
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Pieles / Colores", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                unselectedContentColor = Color(0xFF94A3B8),
                selectedContentColor = Color(0xFFB082FF),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Accesorios 🧢", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                unselectedContentColor = Color(0xFF94A3B8),
                selectedContentColor = Color(0xFFB082FF),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unequip Accessory strip if Accessories Tab is selected
        if (selectedTab == 1 && state.equippedAccessory != "none") {
            Button(
                onClick = onUnequipAcc,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quitar Accesorio Equipado", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            val categorisedItems = if (selectedTab == 0) {
                shopItems.filter { it.type == "color" }
            } else {
                shopItems.filter { it.type == "accessory" }
            }

            if (categorisedItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No hay artículos disponibles.", color = Color.Gray)
                    }
                }
            } else {
                items(categorisedItems) { item ->
                    val isEquipped = if (item.type == "color") {
                        val skinColorName = item.itemId.removePrefix("color_")
                        state.skinColor.lowercase() == skinColorName.lowercase()
                    } else {
                        val accName = item.itemId.removePrefix("acc_")
                        state.equippedAccessory.lowercase() == accName.lowercase()
                    }

                    ShopItemRow(
                        item = item,
                        currentCoins = state.coins,
                        isEquipped = isEquipped,
                        onBuy = { onBuyItem(item) },
                        onEquip = { onEquipItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShopItemRow(
    item: ShopItem,
    currentCoins: Int,
    isEquipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isEquipped) 2.dp else 1.dp,
            color = if (isEquipped) Color(0xFFB082FF) else Color(0xFF475569)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left content item details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular icon representing the color swatch or accessory
                Surface(
                    color = getPreviewSwatchColor(item.itemId),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.White),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (item.type == "accessory") {
                            Text(getAccessoryEmoji(item.itemId), fontSize = 18.sp)
                        } else {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        item.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (item.isUnlocked) "Desbloqueado ✓" else "Precio de compra",
                        fontSize = 11.sp,
                        color = if (item.isUnlocked) Color(0xFF66BB6A) else Color(0xFF94A3B8)
                    )
                }
            }

            // Right interact state button
            Box {
                when {
                    isEquipped -> {
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Equipado 👑",
                                color = Color(0xFFB082FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    item.isUnlocked -> {
                        Button(
                            onClick = onEquip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("equip_" + item.itemId)
                        ) {
                            Text("Equipar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        // Needs unlock buying
                        val canAfford = currentCoins >= item.price
                        Button(
                            onClick = onBuy,
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD54F),
                                disabledContainerColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("buy_" + item.itemId)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (canAfford) Color(0xFF5D4037) else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${item.price} g",
                                fontSize = 12.sp,
                                color = if (canAfford) Color(0xFF5D4037) else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getPreviewSwatchColor(id: String): Color {
    return when (id) {
        "color_lilac" -> Color(0xFFD7A1FC)
        "color_sky" -> Color(0xFF8CC1F0)
        "color_mint" -> Color(0xFF7FE6B2)
        "color_peach" -> Color(0xFFFC9E8D)
        "color_gold" -> Color(0xFFFFD54F)
        else -> Color(0xFF5A6E85)
    }
}

private fun getAccessoryEmoji(id: String): String {
    return when (id) {
        "acc_party_hat" -> "🎉"
        "acc_sunglasses" -> "😎"
        "acc_bowtie" -> "🎀"
        "acc_crown" -> "👑"
        "acc_wizard_hat" -> "🧙‍♂️"
        else -> "🧢"
    }
}

// ==========================================
// INTERACTIVE MINI-GAME VIEW
// ==========================================

@Composable
fun MiniGameView(viewModel: PetViewModel) {
    val gameActive by viewModel.gameActive.collectAsStateWithLifecycle()
    val gameFeedback by viewModel.gameFeedback.collectAsStateWithLifecycle()
    val petState by viewModel.petState.collectAsStateWithLifecycle()

    val isSleeping = petState?.isSleeping ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Caja Misteriosa 📦🎮",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            "¡Elige una caja y encuentra el tesoro de monedas!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        if (isSleeping) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x33FF5252)),
                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "💤 Tu mascota está durmiendo. ¡Despiértala en el panel principal para poder jugar juntos!",
                    color = Color(0xFFFF8A80),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (!gameActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, Color(0xFFB082FF)),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFB082FF),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "¿Listo para Ganar?",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Atiende la Felicidad y gana hasta +30 monedas por jugada con tu Mochi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.startMiniGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB082FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("start_game")
                    ) {
                        Text("Iniciar Juego (Muelle 🎮)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Live Interactive Boxes guessing screen
            Text(
                text = gameFeedback,
                color = Color(0xFFE2E8F0),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 32.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (boxId in 1..3) {
                    MysteryBoxWidget(
                        boxId = boxId,
                        onBoxClick = { viewModel.makeMove(boxId) }
                    )
                }
            }
        }
    }
}

@Composable
fun MysteryBoxWidget(
    boxId: Int,
    onBoxClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, Color(0xFFFFD54F)),
        modifier = Modifier
            .size(90.dp)
            .clickable { onBoxClick() }
            .testTag("mystery_box_$boxId")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📦", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Caja $boxId",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// NOTIFICATIONS IN-APP ARCHIVE VIEW
// ==========================================

@Composable
fun NotificationHistoryView(
    notifications: List<PetNotification>,
    onClearHistory: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Centro de Alertas 🚨",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Mascota cuidador smart log",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            // Control Menu Icons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("notifications_read_all")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Marcar todas", tint = Color(0xFF81C784))
                }
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("notifications_clear")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar historial", tint = Color(0xFFE57373))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Vacio",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Historial Limpio",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Las alertas importantes aparecerán aquí.",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 2.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { item ->
                    NotificationLogCard(item)
                }
            }
        }
    }
}

@Composable
fun NotificationLogCard(item: PetNotification) {
    val bColor = if (!item.isRead) Color(0xFF1E293B) else Color(0x331E293B)
    val strokeColor = if (!item.isRead) Color(0x66B082FF) else Color(0xFF334155)
    
    val bulletColor = when (item.type) {
        "alert" -> Color(0xFFFF5252)
        "evolution" -> Color(0xFFE040FB)
        "unlock" -> Color(0xFFFFD54F)
        else -> Color(0xFF80DEEA)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, strokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = bulletColor,
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.message,
                    color = if (!item.isRead) Color.White else Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
                Text(
                    text = sdf.format(Date(item.timestamp)),
                    color = Color(0xFF475569),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ==========================================
// PORTAL DE ELECCIÓN DE EVOLUCIÓN DINÁMICA
// ==========================================

@Composable
fun EvolutionPortalChoice(
    state: PetState,
    onEvolveSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Backdrop shadow overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6050510)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(3.dp, Color(0xFFFFD54F)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                item {
                    Text(
                        "✨ ¡MOMENTO DE EVOLUCIÓN! ✨",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${state.name} ha alcanzado el Nivel ${state.level} y está listo para transformarse. ¡Tus decisiones dictarán su forma futura!",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Selecciona un Sendero Elemental:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Four Branch paths options
                item {
                    EvolutionPathCard(
                        title = "Sendero Ígneo 🔥",
                        desc = "Evolución activa, temperamento fuerte y llamas de calor.",
                        pathId = "Fuego",
                        onSelect = onEvolveSelect
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    EvolutionPathCard(
                        title = "Sendero Náyade 💧",
                        desc = "Evolución serena, templanza de agua y fluidez cristalina.",
                        pathId = "Agua",
                        onSelect = onEvolveSelect
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    EvolutionPathCard(
                        title = "Sendero Forestal 🌿",
                        desc = "Evolución orgánica, vigor de la naturaleza y renacer verde.",
                        pathId = "Natura",
                        onSelect = onEvolveSelect
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    EvolutionPathCard(
                        title = "Sendero Astral/Cósmico 🌌",
                        desc = "Evolución estelar, polvillo del vacío y nebulosas flotantes.",
                        pathId = "Cosmos",
                        onSelect = onEvolveSelect
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Elegir Más Tarde", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EvolutionPathCard(
    title: String,
    desc: String,
    pathId: String,
    onSelect: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF4338CA)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(pathId) }
            .testTag("evolve_to_" + pathId)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when(pathId) {
                    "Fuego" -> "🟥"
                    "Agua" -> "🟦"
                    "Natura" -> "🟩"
                    else -> "🟪"
                },
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    desc,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun HatchingScreen(
    state: PetState,
    onHatch: (String, String) -> Unit
) {
    var petName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Ninguno") } // "Ninguno", "Macho", "Hembra"
    var tapCounter by remember { mutableStateOf(5) }
    var incubationActive by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¡NUEVO HUEVO DETECTADO! 🐣",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Configura a tu Mochi y toca el huevo para incubarlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }

        // Egg screen display container
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(4.dp, Color(0xFF334155)), RoundedCornerShape(32.dp))
                    .padding(8.dp)
                    .clickable(enabled = incubationActive) {
                        if (tapCounter > 1) {
                            tapCounter--
                        } else {
                            tapCounter = 0
                            onHatch(petName, selectedGender)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x1F80DEEA),
                                    Color(0x05000000)
                                )
                            )
                        )
                ) {
                    PetCanvas(
                        petState = state,
                        activeInteraction = if (incubationActive) "cleaning" else "none",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Small indicator overlays
                    Surface(
                        color = Color(0xCC1E293B),
                        border = BorderStroke(1.dp, Color(0x33B180FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = if (!incubationActive) "💤 Huevo durmiendo..." else "🥚 ¡Toca el huevo! Taps restantes: $tapCounter",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF475569)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Identidad de Mochi 📝",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Pet Name Input Field
                    OutlinedTextField(
                        value = petName,
                        onValueChange = {
                            if (it.length <= 16) {
                                petName = it
                                errorMessage = ""
                            }
                        },
                        label = { Text("Nombre de la mascota") },
                        placeholder = { Text("Ej: Mochi, Chobi, etc.") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x1A000000),
                            unfocusedContainerColor = Color(0x1A000000),
                            focusedIndicatorColor = Color(0xFFB082FF),
                            focusedLabelColor = Color(0xFFB082FF)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("hatch_name_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gender Selector Title
                    Text(
                        "Selecciona el Sexo ♂️ ♀️",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    // Gender Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Macho Button
                        val activeM = selectedGender == "Macho"
                        Button(
                            onClick = { selectedGender = "Macho"; errorMessage = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeM) Color(0x3340C4FF) else Color(0xFF334155),
                                contentColor = if (activeM) Color(0xFF40C4FF) else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (activeM) 2.dp else 1.dp,
                                color = if (activeM) Color(0xFF40C4FF) else Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("♂️ Macho", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // Hembra Button
                        val activeH = selectedGender == "Hembra"
                        Button(
                            onClick = { selectedGender = "Hembra"; errorMessage = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeH) Color(0x33FF4081) else Color(0xFF334155),
                                contentColor = if (activeH) Color(0xFFFF4081) else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (activeH) 2.dp else 1.dp,
                                color = if (activeH) Color(0xFFFF4081) else Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("♀️ Hembra", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Incubator activator / Launch button
                    if (!incubationActive) {
                        Button(
                            onClick = {
                                if (petName.isBlank()) {
                                    errorMessage = "⚠️ Por favor, ingresa un nombre para tu Mochi."
                                } else if (selectedGender == "Ninguno") {
                                    errorMessage = "⚠️ Por favor, selecciona el sexo (Macho o Hembra)."
                                } else {
                                    incubationActive = true
                                    errorMessage = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB082FF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("activate_incubator")
                        ) {
                            Text(
                                "Activar Incubadora 🔌",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        // Tapping instruction
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "⚡ ¡La incubadora está activa! ⚡",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Toca el huevo arriba para abrir el cascarón.",
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Simple manual Hatch button just in case tapping is unpractical in some environments
                            TextButton(
                                onClick = { onHatch(petName, selectedGender) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Forzar Eclosión 🐣", color = Color(0xFFB082FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
