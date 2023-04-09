package team6.project.frontend

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import team6.project.R
import team6.project.frontend.theme.AugRealityAIArtTheme
import androidx.compose.ui.platform.LocalContext
import android.graphics.Color
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    private val REQUEST_CAMERA_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect Python to the app
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform (this@MainActivity))
        }
        //Connect openCV
        Log.d("OpenCV", "OpenCV loaded Successfully!${OpenCVLoader.initDebug()}")

        // Set theme and add composable to screen
        setContent {
            AugRealityAIArtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CameraScreen({ startChatbotActivity() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Request permission to access the user's camera
        requestCameraPermission()
    }
    private fun updateBackgroundColor(color: Int) {
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        rootView.setBackgroundColor(color)
    }

    // Request camera access if the user hasn't already given permission
    private fun requestCameraPermission() {
        val sharedPreferences = getSharedPreferences("camera_preferences", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            updateBackgroundColor(Color.BLACK) // Set the background color to black when showing the dialog
            AlertDialog.Builder(this)
                .setTitle("Camera Permission")
                .setMessage("Do you want to grant access to your camera?")
                .setPositiveButton("Allow") { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
                    editor.putBoolean("camera_permission_asked", true).apply()
                    updateBackgroundColor(Color.TRANSPARENT) // Set the background color to transparent when dismissing
                }
                .setNegativeButton("Deny") { _, _ ->
                    editor.putBoolean("camera_permission_asked", true).apply()
                    updateBackgroundColor(Color.TRANSPARENT) // Set the background color to transparent when dismissing
                }
                .setNeutralButton("Ask me later") { _, _ ->
                    editor.putBoolean("camera_permission_asked", true).apply()
                    updateBackgroundColor(Color.TRANSPARENT) // Set the background color to transparent when dismissing
                }
                .setOnDismissListener {
                    updateBackgroundColor(Color.TRANSPARENT) // Set the background color to transparent when dismissing
                }
                .create()
                .show()
        }
    }

    // Switch from the Camera screen to the Chatbot Screen
    fun startChatbotActivity() {
        val intent = Intent(this, ChatbotActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.no_animation)
    }
}

// Assemble all elements on the Camera Screen
@Composable
fun CameraScreen(toChatbotScreen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Check if the user has given camera permissions
        if (hasCameraPermission) {
            CameraView()
        } else {
            StaticPaintingImage()
        }
        Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            ChatbotScreenButton(onClick = { toChatbotScreen() })
        }
    }
}



// Display the camera view
@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Preview is incorrectly scaled in Compose on some devices without this
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                try {
                    // Must unbind the use-cases before rebinding them.
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview
                    )
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        })
}

// Display a static image of the painting
@Composable
fun StaticPaintingImage() {
//    val paintingImage = painterResource(R.drawable.painting_image) //Replace R.drawable.static_image with the resource ID of the image we will use
//    Image(
//        painter = paintingImage,
//        contentDescription = "Image of painting",
//        modifier = Modifier.fillMaxSize()
//    )
//
}

// Display the button for navigating to the Chatbot Screen
@Composable
fun ChatbotScreenButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "Talk to the painting")
    }
}
