package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.CoinTossScreen
import com.example.ui.screens.ConfigureMatchScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MatchPlayScreen
import com.example.ui.screens.StatsAndHistoryScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Dark slate background matches sports stadiums theme
                ) {
                    when (currentScreen) {
                        Screen.MENU -> {
                            MainMenuScreen(
                                viewModel = viewModel,
                                onStartMatchConfig = { viewModel.navigateTo(Screen.CONFIG) },
                                onOpenStats = { viewModel.navigateTo(Screen.STATS) },
                                onOpenAchievements = { viewModel.navigateTo(Screen.ACHIEVEMENTS) },
                                onOpenThemes = { viewModel.navigateTo(Screen.THEMES) }
                            )
                        }
                        Screen.CONFIG -> {
                            ConfigureMatchScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.MENU) }
                            )
                        }
                        Screen.TOSS -> {
                            CoinTossScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.CONFIG) }
                            )
                        }
                        Screen.PLAY -> {
                            MatchPlayScreen(
                                viewModel = viewModel,
                                onBackToMenu = { viewModel.restartSetup() }
                            )
                        }
                        Screen.STATS -> {
                            StatsAndHistoryScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.MENU) }
                            )
                        }
                        Screen.ACHIEVEMENTS -> {
                            AchievementsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.MENU) }
                            )
                        }
                        Screen.THEMES -> {
                            ThemesScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.MENU) }
                            )
                        }
                    }
                }
            }
        }
    }
}
