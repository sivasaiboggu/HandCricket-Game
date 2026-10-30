package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.ConfigureMatchScreen
import com.example.ui.screens.CoinTossScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MatchPlayScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class UiScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun viewModel(): GameViewModel =
        GameViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun mainMenu_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainMenuScreen(viewModel(), {}, {}, {})
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/ui_main_menu.png")
    }

    @Test
    fun configureMatch_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ConfigureMatchScreen(viewModel(), {})
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/ui_configure_match.png")
    }

    @Test
    fun matchPlay_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MatchPlayScreen(viewModel(), {})
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/ui_match_play.png")
    }

    @Test
    fun coinToss_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CoinTossScreen(viewModel(), {})
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/ui_coin_toss.png")
    }
}
