package team6.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import team6.project.frontend.ChatbotActivity

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented tests for ChatbotActivity.
 */
@RunWith(AndroidJUnit4::class)
class ChatbotActivityInstrumentedTest {

    @get:Rule
    val chatbotTestRule = createAndroidComposeRule<ChatbotActivity>()

    @Test
    fun chatbotToMainTest() {
        chatbotTestRule.onNodeWithContentDescription("To Painting Screen").performClick()
        chatbotTestRule.onNodeWithText("Talk to the painting").assertIsDisplayed()
    }
}