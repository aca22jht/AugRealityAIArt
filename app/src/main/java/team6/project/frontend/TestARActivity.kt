package team6.project.frontend

import android.content.DialogInterface
import android.content.Intent
import team6.project.R
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.InstructionsController
import com.gorisse.thomas.sceneform.light.LightEstimationConfig
import com.gorisse.thomas.sceneform.lightEstimationConfig
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.function.Consumer

class TestARActivity : AppCompatActivity(), FragmentOnAttachListener, OnSessionConfigurationListener {
    private val futures: MutableList<CompletableFuture<Void>> = ArrayList()
    private var arFragment: ArFragment? = null
    private var paintingDetected = false
    private var database: AugmentedImageDatabase? = null
    private var mediaPlayer: MediaPlayer? = null
    private var anchorNode: AnchorNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testaractivity)

        val arButton: Button = findViewById(R.id.testArButton)
        val chatButton: Button = findViewById(R.id.testChatButton)

        arButton.setOnClickListener {
            //TODO: Make button enable/disable anchorNode
            if (anchorNode!!.isEnabled) {
                arButton.text = "AR off"
            } else {
                arButton.text = "AR on"
            }
        }

        chatButton.setOnClickListener {
            // Switch from the Painting Screen to the Chatbot Screen
            startActivity(Intent(this, ChatbotActivity::class.java))
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
            finish()
        }

        supportFragmentManager.addFragmentOnAttachListener(this)
        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.testFragmentContainerView, ArFragment::class.java, null)
                    .commit()
            }
        }
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment.id == R.id.testFragmentContainerView) {
            arFragment = fragment as ArFragment
            arFragment!!.setOnSessionConfigurationListener(this)
        }
    }

    override fun onSessionConfiguration(session: Session, config: Config) {
        // Disable plane detection
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        // Set camera to auto focus
        config.focusMode = Config.FocusMode.AUTO

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)
        database = AugmentedImageDatabase(session)
        val paintingImage = BitmapFactory.decodeResource(resources, R.drawable.painting)
        // Every image has to have its own unique String identifier
        database!!.addImage("painting", paintingImage)
        config.setAugmentedImageDatabase(database)

        // Check for image detection
        arFragment!!.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(
                augmentedImage
            )
        }
        arFragment!!.arSceneView.lightEstimationConfig = LightEstimationConfig.DISABLED
    }

    override fun onDestroy() {
        super.onDestroy()
        futures.forEach(Consumer { future: CompletableFuture<Void> ->
            if (!future.isDone) future.cancel(
                true
            )
        })
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
        }
    }

    private fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (paintingDetected) {
            return
        }
        if ((augmentedImage.trackingState == TrackingState.TRACKING
                    && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING)
        ) {

            // If painting model haven't been placed yet and detected image has String identifier of "painting"
            // This is also example of model loading and placing at runtime
            if (!paintingDetected && (augmentedImage.name == "painting")) {
                // Setting anchor to the center of Augmented Image
                anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

                paintingDetected = true
                Toast.makeText(this, "Painting tag detected", Toast.LENGTH_LONG).show()
                renderObject(anchorNode!!, augmentedImage)
            }
        }
        if (paintingDetected) {
            arFragment!!.instructionsController.setEnabled(
                InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false
            )
        }
    }

    private fun renderObject(anchorNode : AnchorNode, image: AugmentedImage) {
        futures.add(
            ModelRenderable.builder()
                .setSource(this, Uri.parse("models/girlWithTheBlueRibbon.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept { model: ModelRenderable ->
                    addNodeToScene(anchorNode ,model , image)
                }
                .exceptionally { throwable: Throwable? ->
                    var message = if (throwable is CompletionException) {
                        "Internet is not working"
                    } else {
                        "Can't load Model"
                    }
                    val mainHandler = Handler(Looper.getMainLooper())
                    val finalMessage: String = message
                    val myRunnable = Runnable {
                        AlertDialog.Builder(this)
                            .setTitle("Error")
                            .setMessage(finalMessage + "")
                            .setPositiveButton("Retry") { dialogInterface: DialogInterface, _: Int ->
                                renderObject(anchorNode, image)
                                dialogInterface.dismiss()
                            }
                            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                            .show()
                    }
                    mainHandler.post(myRunnable)
                    null
                }
        )
    }

    private fun addNodeToScene(anchorNode : AnchorNode , renderable: Renderable , image : AugmentedImage) {

        val modelWidth = 0.6f // the real width of the model
        val modelHeight = 0.6f // the real height of the model
        val modelDepth = 0.8f // the real depth of the model
        var arWidth = image.extentX // extentX is estimated width
        var scale = arWidth / modelWidth * 1.7f
        val modelNode = Node()
        modelNode.localScale = Vector3(modelWidth*scale, modelHeight*scale, modelDepth*scale)

        modelNode.renderable = renderable
        modelNode.renderableInstance.animate(true).start()
        modelNode.parent = anchorNode
        arFragment!!.arSceneView.scene.addChild(anchorNode)
    }
}