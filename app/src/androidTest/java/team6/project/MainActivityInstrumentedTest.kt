package team6.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import team6.project.frontend.MainActivity

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import team6.project.frontend.CameraScreen
import team6.project.frontend.theme.AugRealityAIArtTheme

/**
 * Instrumented tests for MainActivity.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val mainTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainToChatbotTest() {
        mainTestRule.onNodeWithText("Talk to the painting").performClick()
        mainTestRule.onNodeWithContentDescription("To Camera Screen").assertIsDisplayed()
    }

    @Test
    fun cameraScreenTest() {
        mainTestRule.setContent {
            AugRealityAIArtTheme {
                CameraScreen(toChatbotScreen = { /* Intentionally empty */ })
            }
        }

        // Check if "Talk to the painting" button is displayed
        mainTestRule.onNodeWithText("Talk to the painting").assertIsDisplayed()

        // Add more assertions to test other components or behaviors in the CameraScreen Composable
    }

}