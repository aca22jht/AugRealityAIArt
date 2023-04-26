package team6.project.frontend

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.opencv.android.OpenCVLoader
import team6.project.R

class MainActivity : ComponentActivity() {
    private val REQUEST_CAMERA_PERMISSION = 100

    companion object {
        val chatbotViewModel = ChatbotViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect Python to the app
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this@MainActivity))
        }

        // Connect openCV
        Log.d("OpenCV", "OpenCV loaded Successfully!${OpenCVLoader.initDebug()}")

        // Preload Chatbot WebView
        //setContent {
        //    ChatbotWebView("file:///android_asset/chatbot.html", chatbotViewModel, false)
        //}

        // Handle camera permissions and load painting screen
        requestCameraPermission()
    }

    // Request camera access if the user hasn't already given permission
    private fun requestCameraPermission() {
        // If user hasn't given camera permission, show camera consent prompt
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            // Otherwise, if user has already given camera permission, load painting screen
            startPaintingActivity()
        }
    }

    // Switch to painting screen when user answers camera consent prompt
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startPaintingActivity()
    }

    // Switch to the Painting Screen
    fun startPaintingActivity() {
        val intent = Intent(this, PaintingActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.no_animation, R.anim.fade_out)
        finish()
    }
}

// Class to save the WebView
class ChatbotViewModel : ViewModel() {
    var webView: WebView? = null
}