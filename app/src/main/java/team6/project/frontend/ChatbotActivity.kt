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

/**
 * ChatbotActivity.kt
 *
 * Compose the Chatbot Screen elements and handle the chatbot.html embed script with a WebView
 *
 * @since 1.0 03/05/2023
 *
 * @author Jessica Leatherland
 */
class ChatbotActivity : ComponentActivity() {

    // Save an instance of TextToSpeechInterface to connect the WebView to
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

    // Switch from the Chatbot screen to the Painting Screen that the user came from
    private fun startPaintingActivity() {
        // Release the Text-to-speech resources
        textToSpeechInterface.release()

        // Get the intent for which screen to go to based on whether the user is using AR
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
        // Switch the button image based on whether the Text-to-speech is on mute
        painter = if (onMute) {
            painterResource(R.drawable.speaker_off)
        } else {
            painterResource(R.drawable.speaker_on)
        },
        contentDescription = "Mute/Unmute button",
        modifier = Modifier
            .clickable {
                // Flip onMute and call the relevant function from the TextToSpeechInterface
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
 * https://www.boltuix.com/2022/07/compose-webview-part-4-offline.html
 * [accessed 16 Mar 2023]
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

    // Load the webView from the ViewModel in MainActivity or initialise it if it isn't already
    val webView = remember {
        viewModel.webView ?: WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Handle moving between pages in the webView
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

            // So the webView can be connected to the Text-to-speech JavaScriptInterface
            settings.javaScriptEnabled = true

            // To verify that the client requesting the web page is the Android app
            settings.userAgentString = System.getProperty("http.agent")

            // Load HTML embed script for chatbot
            loadUrl(url)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            // Do nothing, since the WebView is stored in the ViewModel and should not be destroyed
        }
    }

    // Save the webView in the ViewModel from MainActivity
    viewModel.webView = webView

    // Connect to JavaScriptInterface to use Text-to-speech functions
    webView.addJavascriptInterface(textToSpeechInterface, "JSInterface")

    // If the WebView is active (i.e. not being preloaded), display it and unmute the Text-to-speech
    if (isActive) {
        AndroidView(
            factory = { webView }
        )
        textToSpeechInterface.unMute()
    }
}
