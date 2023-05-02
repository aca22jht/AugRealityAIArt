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

class MainActivity : ComponentActivity() {
    private var userRequestInstall = true
    private var preloadChatbot = true
    private var requestedCamera = false

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

    override fun onResume() {
        super.onResume()
        if (requestedCamera) {
            installAR()
        }
    }

    private fun requestCameraPermissions() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
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

    private fun installAR() {
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

// Class to save the WebView
class ChatbotViewModel : ViewModel() {
    var webView: WebView? = null
}