package team6.project.frontend

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import team6.project.R
import team6.project.frontend.theme.AugRealityAIArtTheme

class PaintingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up theme and add PaintingScreen composables
        setContent {
            AugRealityAIArtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PaintingScreen({ startChatbotActivity() })
                }
            }
        }
    }

    // Switch from the Painting Screen to the Chatbot Screen
    fun startChatbotActivity() {
        val intent = Intent(this, ChatbotActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
        finish()
    }
}


// Assemble all elements on the Painting Screen
@Composable
fun PaintingScreen(toChatbotScreen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cameraPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)

    var augRealityOn by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier.fillMaxSize()
    ) {
        // If the user has granted camera access, show camera view
        // Otherwise, show static image of painting
        if (cameraPermissions == PackageManager.PERMISSION_GRANTED) {
            CameraView()
        } else {
            StaticPaintingImage()
        }

        // If the AR button is on, overlay the AR Animation
        if (augRealityOn) {
            // TODO: replace this placeholder
            Box (
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.White)
                    .padding(top = 5.dp, bottom = 5.dp, start = 5.dp, end = 5.dp)
            ) {
                ARAnimation()
            }
        }

        // Overlay the buttons
        Column (
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 16.dp)
        ) {
            Row (
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            ) {
                ARButton { augRealityOn = !augRealityOn }
            }
            ChatbotScreenButton(onClick = { toChatbotScreen() })
        }
    }
}


// Display the camera view
@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier.semantics { contentDescription = "Camera View" },
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


// Display AR on/off button
@Composable
fun ARButton(onClick: () -> Unit) {
    val availability = ArCoreApk.getInstance().checkAvailability(LocalContext.current)

    Button (
        onClick = onClick,
        enabled = availability.isSupported
    ) {
        Text(text = "AR")
    }
}


// Display AR animation
@Composable
fun ARAnimation() {
    // TODO: replace with AR implementation
    Text(text = "Need to add AR")
}


// Display a static image of the painting
@Composable
fun StaticPaintingImage() {
    val paintingImage = painterResource(R.drawable.girl_with_a_blue_ribbon)
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

private boolean setupAugmentedImageDb(Config config) {
    AugmentedImageDatabase augmentedImageDatabase
    Bitmap augmentedImageBitmap = loadAugmentedImage()
    if (augmentedImageBitmap == null) {
        return false
    }
    augmentedImageDatabase = new AugmentedImageDatabase(mSession)
    augmentedImageDatabase.addImage("girlWithTheBlueRibbon", augmentedImageBitmap)
    config.setAugmentedImageDatabase(augmentedImageDatabase)
    return true
}
private Bitmap loadAugmentedImage(){
    try (InputStream is = getAssets().open("blueRibbon.jpg")){
        return BitmapFactory.decodeStream(is)
    }
        catch (IOException e){
            Log.e("ImageLoad", "IO Exception while loading", e)
        }
        return null
