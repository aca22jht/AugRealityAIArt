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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import android.Manifest


/**
 * Instrumented tests for MainActivity.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val mainTestRule = createAndroidComposeRule<MainActivity>()
    val activityRule = ActivityScenarioRule(PaintingActivity::class.java)

    @Test
    fun cameraConsentTest() {
    }

    @Test
    fun openAppForTheFirstTimeAndAcceptConsentPrompt_cameraScreenOpensWithCameraOn() {
        // Grant the camera permission before starting the test
        val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

        // Assuming the camera view has a content description "Camera View"
        onView(withContentDescription("Camera View")).check(matches(isDisplayed()))
    }

    @Test
    fun openAppForTheSecondTimeHavingPreviouslyAcceptedConsentPrompt_cameraScreenOpensWithCameraOn() {
        // Grant the camera permission before starting the test
        val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

        // Assuming the camera view has a content description "Camera View"
        onView(withContentDescription("Camera View")).check(matches(isDisplayed()))
    }



}