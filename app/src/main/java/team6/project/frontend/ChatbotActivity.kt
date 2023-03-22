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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import team6.project.R
import team6.project.frontend.theme.AugRealityAIArtTheme

class ChatbotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AugRealityAIArtTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ChatbotScreen({ startCameraActivity() })
                }
            }
        }
    }
    fun startCameraActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_bottom)
    }
}

@Composable
fun ChatbotScreen(toCameraScreen: () -> Unit, modifier: Modifier = Modifier) {
    Box {
        ChatbotWebView("file:///android_asset/chatbot.html")
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

@Composable
fun RetractScreenButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.keyboard_down_arrow),
        contentDescription = "To Camera Screen",
        contentScale = ContentScale.Fit,
        modifier = Modifier.clickable { onClick() }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatbotWebView(url: String){
    // Adding a WebView inside AndroidView
    // with layout as full screen
    AndroidView(
        factory = {
            WebView(it).apply {
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
        },
        update = {
            it.loadUrl(url)
        }
    )
}