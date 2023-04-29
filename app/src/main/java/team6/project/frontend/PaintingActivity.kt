package team6.project.frontend

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.InstructionsController
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_painting.*
import team6.project.R
import java.io.IOException
import java.util.*


class PaintingActivity : AppCompatActivity(), FragmentOnAttachListener,
    OnSessionConfigurationListener {

    private val TAG = "PaintingActivity"

    private lateinit var mArFragment: ArFragment
    private lateinit var mImageDatabase: AugmentedImageDatabase

    private lateinit var mArButton: Button
    private lateinit var mChatButton: Button

//    private var mGirlDetected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painting)

        mArButton = findViewById(R.id.arButton)
        mChatButton = findViewById(R.id.chatButton)

        mArButton.setOnClickListener {
            Toast.makeText(
                this@PaintingActivity,
                "Add the 3d effect of ar",
                Toast.LENGTH_SHORT
            ).show()
            TODO("Add the 3d effect of ar")
        }

        mChatButton.setOnClickListener {
            // Switch from the Painting Screen to the Chatbot Screen
            startActivity(Intent(this, ChatbotActivity::class.java))
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
            finish()
        }

        supportFragmentManager.addFragmentOnAttachListener(this)

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainerView, ArFragment::class.java, null, "ar")
                    .commit()
            }
        }
    }


    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (TextUtils.equals(fragment.tag, "ar") && fragment.id == R.id.fragmentContainerView) {
            mArFragment = fragment as ArFragment
            mArFragment.setOnSessionConfigurationListener(this)
        }
    }

    private var mLastToastTime: Long = 0

    override fun onSessionConfiguration(session: Session, config: Config) {
        with(config) {
            // Disable plane detection
            planeFindingMode = Config.PlaneFindingMode.DISABLED

            focusMode = Config.FocusMode.AUTO

            // Images to be detected by our AR need to be added in AugmentedImageDatabase
            // This is how database is created at runtime
            // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)
            try {
                assets.open("myimages.imgdb").use {
                    mImageDatabase = AugmentedImageDatabase.deserialize(session, it)
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO exception loading augmented image database.", e)
            }
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            // This is an alternative way to initialize an AugmentedImageDatabase instance,
            // load a pre-existing augmented image database.
            setAugmentedImageDatabase(mImageDatabase)
        }

        session.configure(config)

        // Check for image detection
        mArFragment.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            Log.i(TAG, "onSessionConfiguration: ")
//            if (mGirlDetected) {
//                return@setOnAugmentedImageUpdateListener
//            }
            arButton.isEnabled = with(augmentedImage) {
                when {
                    trackingState == TrackingState.TRACKING && trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING -> {
                        // Create an anchor for the image and place the anchor.
                        val augmentedImageName = augmentedImage.name
                        Log.d(TAG, "Tracking Image name == $augmentedImageName")

                        !TextUtils.isEmpty(augmentedImageName) && augmentedImageName.contains("girl_with_a_blue_ribbon")

//                        if (!TextUtils.isEmpty(augmentedImageName) && augmentedImageName.contains("girl_with_a_blue_ribbon")) {
//                            TODO("Add the 3d effect of ar")
//                            AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose)).apply {
//                                worldScale = Vector3(0.1f, 0.1f, 0.1f)
//                                parent = mArFragment.arSceneView.scene
//                                // Add your own logic here to render a 3D object on top of the image.
//                                ModelRenderable.builder()
//                                    .setSource(
//                                        this@PaintingActivity,
//                                        Uri.parse("models/girlWithTheBlueRibbon.glb")
//                                    )
//                                    .setIsFilamentGltf(true)
//                                    .build()
//                                    .thenAccept { model: ModelRenderable? ->
//                                        addChild(
//                                            TransformableNode(mArFragment.transformationSystem)
//                                                .apply {
//                                                    renderable = model
//                                                })
//                                    }
//                                    .exceptionally { throwable: Throwable? ->
//                                        Toast.makeText(
//                                            this@PaintingActivity,
//                                            "Unable to load rabbit model",
//                                            Toast.LENGTH_LONG
//                                        ).show()
//                                        throwable?.printStackTrace()
//                                        null
//                                    }
//                            }

//                        } else {
//                        }
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
                        // Remove anchors for AugmentedImages that are no longer tracking.
//                        scene.removeChild(scene.findByName(augmentedImage.name))
                        false
                    }
                    else -> {
                        Log.i(TAG, "onSessionConfiguration: else")
                        false
                    }
                }
            }.also {
                if (it) {
                    if (System.currentTimeMillis() - mLastToastTime >= 5 * 1000) {
                        mLastToastTime = System.currentTimeMillis()
                        Toast.makeText(
                            this@PaintingActivity,
                            "ar effects can be loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }

            mArFragment.instructionsController.setEnabled(
                InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false
            )
        }
    }

}




