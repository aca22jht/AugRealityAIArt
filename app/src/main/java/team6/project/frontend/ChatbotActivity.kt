package team6.project.frontend

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import team6.project.R
import team6.project.backend.TextToSpeechInterface
import team6.project.frontend.ChatbotActivity.Companion.textToSpeechInterface
import team6.project.frontend.MainActivity.Companion.usingAR
import team6.project.frontend.theme.AugRealityAIArtTheme
import team6.project.frontend.theme.Purple500

class ChatbotActivity : ComponentActivity() {

    companion object {
        val textToSpeechInterface = TextToSpeechInterface()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme and add composables to screen
        setContent {
            AugRealityAIArtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ChatbotScreen { startPaintingActivity() }
                }
            }
        }
    }

    /*
     * WebView and Android back button navigation
     * Paulo Pereira (06 Oct 2022)
     * https://blog.logrocket.com/customize-androids-back-button-navigation-webview/
     * [accessed 05 Apr 2023]
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val webView = MainActivity.chatbotViewModel.webView
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        // If it wasn't the Back key or there's no webpage history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }

    // Switch from the Chatbot screen to the Painting Screen
    private fun startPaintingActivity() {
        textToSpeechInterface.release()
        val intent: Intent = if (usingAR) {
            Intent(this, PaintingWithArActivity::class.java)
        } else {
            Intent(this, PaintingNoArActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom)
        finish()
    }
}

// Assemble all elements on the Chatbot Screen
@Composable
fun ChatbotScreen(toPaintingScreen: () -> Unit) {
    Column {
        Row (
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(SolidColor(Purple500), RectangleShape)
        ) {
            MuteButton()
            RetractScreenButton(onClick = { toPaintingScreen() })
        }
        ChatbotWebView("file:///android_asset/chatbot.html", MainActivity.chatbotViewModel, true)
    }
}

// Display the button for closing the Chatbot Screen (and going back to the Painting Screen)
@Composable
fun RetractScreenButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(id = R.drawable.keyboard_down_arrow),
        contentDescription = "Retract chatbot screen button",
        contentScale = ContentScale.Fit,
        modifier = Modifier.clickable { onClick() }
    )
}

// Display the button for muting/unmuting the Chatbot Text-to-speech
@Composable
fun MuteButton() {
    var onMute by remember { mutableStateOf(false) }
    Image(
        painter = if (onMute) painterResource(R.drawable.speaker_off) else painterResource(R.drawable.speaker_on),
        contentDescription = "Mute/Unmute button",
        modifier = Modifier
            .clickable {
                onMute = !onMute
                if (onMute) {
                    textToSpeechInterface.mute()
                } else {
                    textToSpeechInterface.unMute()
                }
            }
    )
}

/*
 * Display IBM Watson Assistant WebView - code taken and modified from the following sources:
 *
 * Compose WebView Part 4 | OFFLINE Load from Assets folder
 * Bolt Uix (27 Jul 2022)
 * https://www.boltuix.com/2022/07/compose-webview-part-4-offline.html [accessed 16 Mar 2023]
 *
 * WebView and Android back button navigation
 * Paulo Pereira (06 Oct 2022)
 * https://blog.logrocket.com/customize-androids-back-button-navigation-webview/
 * [accessed 05 Apr 2023]
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatbotWebView(url: String, viewModel: ChatbotViewModel, isActive: Boolean) {
    val context = LocalContext.current
    val webView = remember {
        viewModel.webView ?: WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (view?.contentHeight == 0) {
                        view.reload()
                    } else {
                        super.onPageFinished(view, url)
                    }
                }
            }

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

    // Connect to interface to use Text-to-speech functions
    webView.addJavascriptInterface(textToSpeechInterface, "JSInterface")

    if (isActive) {
        AndroidView(
            factory = { webView }
        )
        textToSpeechInterface.unMute()
    }
}
