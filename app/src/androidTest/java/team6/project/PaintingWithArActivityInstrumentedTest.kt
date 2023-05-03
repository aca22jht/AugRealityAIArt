package team6.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import team6.project.frontend.PaintingNoArActivity
import team6.project.frontend.PaintingWithArActivity

/**
 * Instrumented tests for PaintingActivity.
 */
@RunWith(AndroidJUnit4::class)
class PaintingWithArActivityInstrumentedTest {

    @get:Rule
    val paintingNoArTestRule = createAndroidComposeRule<PaintingNoArActivity>()

    @Test
    fun paintingNoArToChatbotTest() {
        paintingNoArTestRule.onNodeWithText("Talk to the painting").performClick()
        paintingNoArTestRule.onNodeWithContentDescription("To Painting Screen").assertIsDisplayed()
    }

}