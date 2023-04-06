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
import team6.project.frontend.PaintingActivity
import team6.project.frontend.PaintingScreen
import team6.project.frontend.theme.AugRealityAIArtTheme

/**
 * Instrumented tests for PaintingActivity.
 */
@RunWith(AndroidJUnit4::class)
class PaintingActivityInstrumentedTest {

    @get:Rule
    val paintingTestRule = createAndroidComposeRule<PaintingActivity>()

    @Test
    fun mainToChatbotTest() {
        paintingTestRule.onNodeWithText("Talk to the painting").performClick()
        paintingTestRule.onNodeWithContentDescription("To Painting Screen").assertIsDisplayed()
    }

}