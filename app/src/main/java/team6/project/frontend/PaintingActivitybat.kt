package team6.project.frontend

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
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
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import team6.project.R
import team6.project.frontend.theme.AugRealityAIArtTheme

private const val TAG = "PaintingActivitybat"

class PaintingActivitybat : ComponentActivity() {

    val transformationSystem by lazy {
        TransformationSystem(resources.displayMetrics, FootprintSelectionVisualizer())
    }

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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        when (ArCoreApk.getInstance().requestInstall(context as Activity, false)) {
            InstallStatus.INSTALL_REQUESTED -> {
                return
            }
            InstallStatus.INSTALLED -> {}
        }

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
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.White)
                    .padding(top = 5.dp, bottom = 5.dp, start = 5.dp, end = 5.dp)
            ) {
                ARAnimation()
            }
        }

        // Overlay the buttons
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 16.dp)
        ) {
            Row(
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

            val parentView = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

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
// TODO:You can listen to images, analyze them, and add an ArSceneView to a PreviewView or parent layout
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview
                    )

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))


            // Set up ARCore session
            val arSession = Session(context).apply {
                val session = this
                configure(Config(this)
                    .apply {
                        augmentedImageDatabase = AugmentedImageDatabase.deserialize(
                            session,
                            context.assets.open("myimages.imgdb")
                        )
                        updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    })

            }

            val arSceneView = ArSceneView(context).apply {
                this.session = arSession
                this.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_LAST)
                this.scene.addOnUpdateListener {
                    Log.i(TAG, "CameraView: ")
                    val frame = arFrame ?: return@addOnUpdateListener
                    if (frame.camera.trackingState != TrackingState.TRACKING) {
                        Log.i(TAG, "CameraView: invalid")
                        return@addOnUpdateListener
                    }

                    // Detect and track AugmentedImages
                    val updatedAugmentedImages =
                        frame.getUpdatedTrackables(AugmentedImage::class.java)

                    updatedAugmentedImages.forEach { augmentedImage ->
                        with(augmentedImage) {
                            when {
                                trackingState == TrackingState.TRACKING && trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING -> {
                                    // Create an anchor for the image and place the anchor.
                                    Log.d(TAG, "Tracking Image name == ${augmentedImage.name}")
                                    Log.d(TAG, "Tracking Image index == ${augmentedImage.index}")
                                    if (TextUtils.equals(augmentedImage.name, "rabbit")) {
                                        val anchor =
                                            augmentedImage.createAnchor(augmentedImage.centerPose)
                                        AnchorNode(anchor).apply {
                                            parent = scene
                                            // Add your own logic here to render a 3D object on top of the image.
                                            ModelRenderable.builder()
                                                .setSource(
                                                    context,
                                                    Uri.parse("models/girlWithTheBlueRibbon.glb")
                                                )
                                                .setIsFilamentGltf(true)
                                                .build()
                                                .thenAccept { rabbitModel: ModelRenderable? ->
                                                    addChild(TransformableNode((context as PaintingActivitybat).transformationSystem)
                                                        .apply {
                                                            renderable = rabbitModel
                                                        })
                                                }
                                                .exceptionally { throwable: Throwable? ->
                                                    Toast.makeText(
                                                        context,
                                                        "Unable to load rabbit model",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    throwable?.printStackTrace()
                                                    null
                                                }

                                        }

                                    }
                                }
                                trackingState == TrackingState.PAUSED -> {
                                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                                    // but not yet tracked.  This can happen when the image is first found but the
                                    // camera has not moved enough to establish full tracking.  In this case, update the
                                    // state to detect it.
                                    Log.d(TAG, "Detected Image ${augmentedImage.name}")
                                }
                                trackingState == TrackingState.STOPPED -> {
                                    Log.d(TAG, "Stopped Tracking Image ${augmentedImage.name}")
                                    // Remove anchors for AugmentedImages that are no longer tracking.
                                    scene.removeChild(scene.findByName(augmentedImage.name))
                                }
                            }
                        }

                    }
                }
            }
//
//            arSceneView.scene.addOnPeekTouchListener { hitTestResult, motionEvent ->
//                // Add your own logic here to handle tap events on the AR scene.
//
//
//            }

            parentView.also {
                it.addView(arSceneView)
                it.addView(previewView)
            }
        })
}


// Display AR on/off button
@Composable
fun ARButton(onClick: () -> Unit) {
    val availability = ArCoreApk.getInstance().checkAvailability(LocalContext.current)

    Button(
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


