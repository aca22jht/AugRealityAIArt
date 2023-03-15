package team6.project.frontend

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import team6.project.frontend.theme.AugRealityAIArtTheme
import team6.project.R

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
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun ChatbotScreen(toCameraScreen: () -> Unit, modifier: Modifier = Modifier) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RetractScreenButton(onClick = { toCameraScreen() })
        Greeting("Camera Screen")
    }
}

@Composable
fun RetractScreenButton(onClick: () -> Unit) {
    val image = painterResource(id = R.drawable.keyboard_down_arrow)
    Image(
        painter = image,
        contentDescription = "To Camera Screen",
        modifier = Modifier.clickable { onClick() }
    )
}
