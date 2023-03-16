package team6.project.frontend

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
        finish()
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_bottom)
    }
}

@Composable
fun ChatbotScreen(toCameraScreen: () -> Unit, modifier: Modifier = Modifier) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row (
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            RetractScreenButton(onClick = { toCameraScreen() })
        }
        Greeting("Camera Screen")
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
