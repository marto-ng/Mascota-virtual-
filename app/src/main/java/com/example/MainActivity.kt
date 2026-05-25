package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.data.PetDatabase
import com.example.data.PetRepository
import com.example.ui.PetViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize local Room Database instance
        val database = Room.databaseBuilder(
            applicationContext,
            PetDatabase::class.java,
            "pet_database"
        )
        .fallbackToDestructiveMigration() // ensures safety during structure modifications
        .build()

        val repository = PetRepository(database.petDao())

        // Initialise database defaults in background coroutine thread on startup
        lifecycleScope.launch {
            repository.initializeDefaultDataIfNeeded()
        }

        // Setup ViewModel utilizing the custom factory
        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(application, repository)
        )[PetViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) { // Forcing dark theme for retro screen emulator vibes!
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
