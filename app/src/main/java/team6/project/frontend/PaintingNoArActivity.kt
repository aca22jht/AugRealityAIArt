package team6.project.frontend

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import team6.project.R
import team6.project.frontend.theme.AugRealityAIArtTheme

class PaintingNoArActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up theme and add PaintingNoArScreen composables
        setContent {
            AugRealityAIArtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PaintingNoArScreen({ startChatbotActivity() })
                }
            }
        }
    }

    // Switch from the Painting Screen to the Chatbot Screen
    private fun startChatbotActivity() {
        val intent = Intent(this, ChatbotActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
        finish()
    }
}


// Assemble all elements on the Painting Screen
@Composable
fun PaintingNoArScreen(toChatbotScreen: () -> Unit) {
    Box (
        modifier = Modifier.fillMaxSize()
    ) {
        // Show a static image of the painting
        StaticPaintingImage()

        // Overlay the button to the Chatbot Screen
        Column (
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 16.dp)
        ) {
            ChatbotScreenButton(onClick = { toChatbotScreen() })
        }
    }
}

// Display a static image of the painting
@Composable
fun StaticPaintingImage() {
    val paintingImage = painterResource(R.drawable.painting_on_wall)
    Image(
        painter = paintingImage,
        contentDescription = "The girl with a blue ribbon painting",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}


// Display the button for navigating to the Chatbot Screen
@Composable
fun ChatbotScreenButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "Talk to the painting")
    }
}


