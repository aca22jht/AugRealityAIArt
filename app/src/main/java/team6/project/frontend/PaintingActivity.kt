package team6.project.frontend

import android.content.DialogInterface
import android.content.Intent
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.InstructionsController
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_painting.arButton
import team6.project.R
import java.io.IOException
import java.util.concurrent.CompletionException


class PaintingActivity : AppCompatActivity(), FragmentOnAttachListener,
    OnSessionConfigurationListener {

    private val TAG = "PaintingActivity"

    private lateinit var mArFragment: ArFragment
    private lateinit var mImageDatabase: AugmentedImageDatabase
    var mSession : Session ? = null
    var mUserRequestInstall = true

    private lateinit var mArButton: Button
    private lateinit var mChatButton: Button
    private lateinit var augmentedImage : AugmentedImage

//    private var mGirlDetected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painting)

        mArButton = findViewById(R.id.arButton)
        mChatButton = findViewById(R.id.chatButton)

        mArButton.setOnClickListener {
            val anchor = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))
            mArFragment.arSceneView.scene.addChild(anchor)
            //Toast.makeText(
                //this@PaintingActivity,
                //"Add the 3d effect of ar",
                //  Toast.LENGTH_SHORT
                //).show()
                // TODO("Add the 3d effect of ar")
                //renderObject()

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

    override fun onResume() {
        super.onResume()
        try {
            if (mSession == null) {

                when (ArCoreApk.getInstance()
                    .requestInstall( this , mUserRequestInstall)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        mSession = Session( this )
                        Toast.makeText( this , "Arcore Session Started", Toast.LENGTH_LONG).show()
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> mUserRequestInstall = false
                    else -> mUserRequestInstall = false
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Toast.makeText( this , "Please Allow ARCore installation to use AR Content", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableArcoreNotInstalledException) {
            Toast.makeText( this , "You need to Install ARCore to contniue", Toast.LENGTH_LONG).show()
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
            try {
                assets.open("myimages.imgdb").use {
                    mImageDatabase = AugmentedImageDatabase.deserialize(session, it)
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO exception loading augmented image database.", e)
            }
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
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

                        renderObject(augmentedImage)
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
    fun renderObject( image: AugmentedImage) {
        ModelRenderable.builder()
            .setSource(this, Uri.parse("models/girlWithTheBlueRibbon.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { model: ModelRenderable ->
                    addNodeToScene(model , image)
                }
            .exceptionally { throwable: Throwable? ->
                var message: String?
                message = if (throwable is CompletionException) {
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
                            renderObject(image)
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .show()
                }
                mainHandler.post(myRunnable)
                null
            }
    }
    private fun addNodeToScene(renderable: Renderable , image : AugmentedImage) {
        val anchorNode =
            AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))
        val modelWidth: Float = 0.5f// the real width of the model
        val modelHeight: Float = 0.6f // the real height of the model
        var arWidth = image.extentX // extentX is estimated width
        var arHeight = image.extentZ // extentZ is estimated height
        var scaledW = modelWidth / arWidth
        var scaledH = modelHeight / arHeight
        val modelNode = Node()
        modelNode.localScale = Vector3(scaledW, scaledH, 0.7f)

        modelNode.renderable = renderable
        modelNode.parent = anchorNode
        mArFragment.arSceneView.scene.addChild(anchorNode)
    }


}




