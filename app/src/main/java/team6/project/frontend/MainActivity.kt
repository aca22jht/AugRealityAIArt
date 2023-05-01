package team6.project.frontend

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import team6.project.R

class MainActivity : ComponentActivity() {

    companion object {
        val chatbotViewModel = ChatbotViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload Chatbot WebView
        setContent {
            ChatbotWebView("file:///android_asset/chatbot.html", chatbotViewModel, false)
        }

        startPaintingActivity()
    }

    // Switch to the Painting Screen
    fun startPaintingActivity() {
        val intent = Intent(this, TestARActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.no_animation, R.anim.fade_out)
        finish()
    }
}

// Class to save the WebView
class ChatbotViewModel : ViewModel() {
    var webView: WebView? = null
}