package team6.project.frontend

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import team6.project.R
import team6.project.frontend.theme.AugRealityAIArtTheme

class ChatbotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme and add composables to screen
        setContent {
            AugRealityAIArtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ChatbotScreen({ startCameraActivity() })
                }
            }
        }
    }
    // Switch from the Chatbot screen to the Camera Screen
    fun startCameraActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_out_bottom, R.anim.no_animation)
    }
}

// Assemble all elements on the Chatbot Screen
@Composable
fun ChatbotScreen(toCameraScreen: () -> Unit, modifier: Modifier = Modifier) {
    Box {
        ChatbotWebView("file:///android_asset/chatbot.html", MainActivity.chatbotViewModel, true)
        Row (
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .padding(top = 5.dp)
        ) {
            RetractScreenButton(onClick = { toCameraScreen() })
        }
    }
}

// Display the button for closing the Chatbot Screen (and going back to the Camera Screen)
@Composable
fun RetractScreenButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.keyboard_down_arrow),
        contentDescription = "To Camera Screen",
        contentScale = ContentScale.Fit,
        modifier = Modifier.clickable { onClick() }
    )
}

// Class to save the WebView
class ChatbotViewModel : ViewModel() {
    var webView: WebView? = null
}

// Display IBM Watson Assistant WebView
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatbotWebView(url: String, viewModel: ChatbotViewModel, isVisible: Boolean) {
    val context = LocalContext.current
    val webView = remember {
        viewModel.webView ?: WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = WebViewClient()

            // to play video on a web view
            settings.javaScriptEnabled = true

            // to verify that the client requesting your web page is actually your Android app.
            settings.userAgentString = System.getProperty("http.agent")

            loadUrl(url)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            // Do nothing, since the WebView is stored in the ViewModel and should not be destroyed here
        }
    }

    viewModel.webView = webView

    if (isVisible) {
        AndroidView(
            factory = { webView }
        )
    }
}