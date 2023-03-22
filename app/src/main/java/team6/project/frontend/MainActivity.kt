package team6.project.frontend

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.tooling.preview.Preview
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import team6.project.frontend.theme.AugRealityAIArtTheme
import team6.project.R
import team6.project.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val REQUEST_CAMERA_PERMISSION = 100
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform (this@MainActivity))
        }
        setContent {
            AugRealityAIArtTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CameraScreen({ startChatbotActivity() })
                }
            }
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted
                    startCamera()
                } else {
                    // Camera permission not granted, display a static image
                    setContent {
                        AugRealityAIArtTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colors.background
                            ) {
                                StaticImageScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        processCameraProvider.addListener({
            try {
                val cameraProvider = processCameraProvider.get()
                val previewUseCase = buildPreviewUseCase()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase)
            } catch (e: Exception) {
                Toast.makeText(this, "error starting the camera", Toast.LENGTH_LONG).show()
            }
        }, cameraExecutor)
    }

    private fun buildPreviewUseCase(){
        return Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider)}
    }

    fun startChatbotActivity() {
        val intent = Intent(this, ChatbotActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.no_animation)
    }
}

@Composable
fun CameraScreen(toChatbotScreen: () -> Unit, modifier: Modifier = Modifier) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Greeting("Camera Screen")

        ChatbotScreenButton(onClick = { toChatbotScreen() })
    }
}

@Composable
fun ChatbotScreenButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "Talk to the painting")
    }
}

@Composable
fun StaticImageScreen() {
//    Box(modifier = Modifier.fillMaxSize()) {
//        val staticImage = painterResource(R.drawable.static_image) //Replace R.drawable.static_image with the resource ID of the image we will use
//        Image(
//            painter = staticImage,
//            contentDescription = "Static image",
//            modifier = Modifier.fillMaxSize()
//        )
//    }
}


@Composable
fun Greeting(name: String) {
    val py = Python.getInstance()
    val pyFile = py.getModule("test")
    val message = pyFile.callAttr("conf", name).toString()
    Text(text = message)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AugRealityAIArtTheme {
        CameraScreen(toChatbotScreen = {})
    }
}