package team6.project.frontend

import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.InstructionsController
import com.gorisse.thomas.sceneform.light.LightEstimationConfig
import com.gorisse.thomas.sceneform.lightEstimationConfig
import team6.project.R
import java.io.IOException
import java.util.concurrent.CompletionException


class PaintingWithArActivity : AppCompatActivity(), FragmentOnAttachListener,
    OnSessionConfigurationListener {

    private val TAG = "PaintingActivity"

    private lateinit var arFragment: ArFragment
    private lateinit var imageDatabase: AugmentedImageDatabase

    private lateinit var arSwitch: SwitchCompat
    private lateinit var chatButton: Button

    private var savedAnchorNode: AnchorNode? = null

    private var lastToastTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painting_with_ar)

        arSwitch = findViewById(R.id.arSwitch)
        chatButton = findViewById(R.id.chatButton)

        chatButton.setOnClickListener {
            // Switch from the Painting Screen to the Chatbot Screen
            startActivity(Intent(this, ChatbotActivity::class.java))
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
            finish()
        }

        supportFragmentManager.addFragmentOnAttachListener(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, ArFragment::class.java, null, "ar")
                .commit()
        }
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (TextUtils.equals(fragment.tag, "ar") && fragment.id == R.id.fragmentContainerView) {
            arFragment = fragment as ArFragment
            arFragment.setOnSessionConfigurationListener(this)
        }
    }

    override fun onSessionConfiguration(session: Session, config: Config) {
        with(config) {
            // Disable plane detection
            planeFindingMode = Config.PlaneFindingMode.DISABLED

            // Configure camera
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            focusMode = Config.FocusMode.AUTO

            // Add the image database to the session so the painting can be detected and tracked
            try {
                assets.open("myimages.imgdb").use {
                    imageDatabase = AugmentedImageDatabase.deserialize(session, it)
                }
            } catch (e: IOException) {
                // Create database at runtime if failed to load
                Log.e(TAG, "IO exception loading augmented image database.", e)
                imageDatabase = AugmentedImageDatabase(session)
                val paintingImage = BitmapFactory.decodeResource(resources, R.drawable.painting)
                imageDatabase.addImage("painting", paintingImage)
            }
            augmentedImageDatabase = imageDatabase

            // Configure lighting
            arFragment.arSceneView.lightEstimationConfig = LightEstimationConfig.DISABLED

            // Check for image detection
            arFragment.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
                onAugmentedImageTrackingUpdate(augmentedImage)
            }
        }
        session.configure(config)
    }

    private fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage) {
        Log.i(TAG, "onSessionConfiguration: ")

        arSwitch.isEnabled = with(augmentedImage) {
            when { trackingState == TrackingState.TRACKING
                    && trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING -> {
                // Create an anchor for the image and place the anchor.
                val augmentedImageName = augmentedImage.name
                Log.d(TAG, "Tracking Image name == $augmentedImageName")
                val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

                arSwitch.setOnCheckedChangeListener { _, _ ->
                    if (arSwitch.isChecked) {
                        addAnchorToScene(anchorNode, augmentedImage)
                    } else {
                        arFragment.arSceneView.scene.removeChild(savedAnchorNode)
                    }
                }
                !TextUtils.isEmpty(augmentedImageName) && augmentedImageName.contains("girl_with_a_blue_ribbon")
            }
                trackingState == TrackingState.PAUSED -> {
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.  This can happen when the image is first found but the
                    // camera has not moved enough to establish full tracking.  In this case, update the
                    // state to detect it.
                    Log.d(TAG, "Detected Image ${augmentedImage.name}")
                    false
                }
                trackingState == TrackingState.STOPPED -> {
                    Log.d(TAG, "Stopped Tracking Image ${augmentedImage.name}")
                    false
                }
                else -> {
                    Log.i(TAG, "onSessionConfiguration: else")
                    false
                }
            }
        }.also {
            if (it) {
                if (!arSwitch.isChecked) {
                    if (System.currentTimeMillis() - lastToastTime >= 10000) {
                        lastToastTime = System.currentTimeMillis()
                        Toast.makeText(
                            this@PaintingWithArActivity,
                            "AR effects can be loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        arFragment.instructionsController.setEnabled(
            InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false
        )
    }

    private fun addAnchorToScene(anchorNode : AnchorNode, image: AugmentedImage) {
        ModelRenderable.builder()
            .setSource(this, Uri.parse("models/girlWithTheBlueRibbon.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { model: ModelRenderable ->
                    addModelToAnchor(anchorNode ,model , image)
                    arFragment.arSceneView.scene.addChild(anchorNode)
                    savedAnchorNode = anchorNode
                }
            .exceptionally { throwable: Throwable? ->
                var message: String? = if (throwable is CompletionException) {
                    "Internet is not working"
                } else {
                    "Can't load Model"
                }
                val mainHandler = Handler(Looper.getMainLooper())
                val myRunnable = Runnable {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(message + "")
                        .setPositiveButton("Retry") { dialogInterface: DialogInterface, _: Int ->
                            addAnchorToScene(anchorNode, image)
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .show()
                }
                mainHandler.post(myRunnable)
                null
            }
    }

    private fun addModelToAnchor(anchorNode : AnchorNode , model: Renderable , image : AugmentedImage) {
        val modelWidth = 0.6f // real width of the model
        val modelHeight = 0.4f // real height of the model
        val modelDepth = 0.7f // real depth of the model
        var arWidth = image.extentX // estimated width of painting augmented image
        var scale = arWidth / modelWidth * 1.8f

        var modelNode = Node()
        modelNode.localScale = Vector3(modelWidth*scale, modelHeight*scale, modelDepth*scale)
        modelNode.renderable = model
        modelNode.renderableInstance.animate(true).start()
        modelNode.parent = anchorNode
    }

    override fun onDestroy() {
        super.onDestroy()
        arFragment.arSceneView.scene.removeChild(savedAnchorNode)
        savedAnchorNode = null
        supportFragmentManager.beginTransaction().remove(arFragment)
    }
}
