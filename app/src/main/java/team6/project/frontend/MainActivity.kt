package team6.project.frontend

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.FatalException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.Sceneform
import team6.project.R

/**
 * MainActivity.kt
 *
 * App entry point, handles preloading, camera permissions and AR install
 *
 * @since 1.0 03/05/2023
 *
 * @author Jessica Leatherland
 * @author Zongyang Cai
 */
class MainActivity : ComponentActivity() {
    private var userRequestInstall = true
    private var preloadChatbot = true
    private var requestedCamera = false

    // Save variables to be accessed elsewhere in the app
    companion object {
        val chatbotViewModel = ChatbotViewModel()
        var usingAR = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload Chatbot WebView
        if (preloadChatbot) {
            setContent {
                ChatbotWebView("file:///android_asset/chatbot.html", chatbotViewModel, false)
            }
            preloadChatbot = false
        }

        requestCameraPermissions()
    }

    // Trigger installAR when the app resumes after installing
    override fun onResume() {
        super.onResume()
        if (requestedCamera) {
            installAR()
        }
    }

    // Request permission to access the user's camera
    private fun requestCameraPermissions() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // If access granted, try to install AR, else transition to downgraded app experience
            if (isGranted) {
                requestedCamera = true
                installAR()
            } else {
                requestedCamera = true
                startPaintingNoArActivity()
            }
        }
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Try to install Google Play Services For AR on the user's device
    private fun installAR() {
        // If the install is successful, go to the AR session,
        // else transition to the downgraded app experience
        try {
            when (ArCoreApk.getInstance()
                .requestInstall(this, userRequestInstall, ArCoreApk.InstallBehavior.OPTIONAL,
                        ArCoreApk.UserMessageType.APPLICATION)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    if (Sceneform.isSupported(this)) {
                        usingAR = true
                        startPaintingWithArActivity()
                    } else {
                        startPaintingNoArActivity()
                    }
                }
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> userRequestInstall = false
                else -> {
                    userRequestInstall = false
                    startPaintingNoArActivity()
                }
            }
        } catch (e: FatalException) {
            startPaintingNoArActivity()
        } catch (e: UnavailableUserDeclinedInstallationException) {
            startPaintingNoArActivity()
        } catch (e: UnavailableArcoreNotInstalledException) {
            startPaintingNoArActivity()
        }
    }

    // Switch to the Painting Screen with AR
    private fun startPaintingWithArActivity() {
        val intent = Intent(this, PaintingWithArActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.no_animation, R.anim.fade_out)
        finish()
    }

    // Switch to the Painting Screen without AR
    private fun startPaintingNoArActivity() {
        val intent = Intent(this, PaintingNoArActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.no_animation, R.anim.fade_out)
        finish()
    }
}

// Class to save the Chatbot WebView
class ChatbotViewModel : ViewModel() {
    var webView: WebView? = null
}