package team6.project.frontend

import team6.project.R
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.filamat.MaterialPackage
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.InstructionsController
import com.google.ar.sceneform.ux.TransformableNode
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class TestARActivity : AppCompatActivity(), FragmentOnAttachListener,
    OnSessionConfigurationListener {
    private val futures: MutableList<CompletableFuture<Void>> = ArrayList()
    private var arFragment: ArFragment? = null
    private var paintingDetected = false
    private var database: AugmentedImageDatabase? = null
    private var plainVideoModel: Renderable? = null
    private var plainVideoMaterial: Material? = null
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testaractivity)
        supportFragmentManager.addFragmentOnAttachListener(this)
        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.arFragment, ArFragment::class.java, null)
                    .commit()
            }
        }
        if (Sceneform.isSupported(this)) {
            // .glb models can be loaded at runtime when needed or when app starts
            // This method loads ModelRenderable when app starts
            loadPaintingModel()
            loadPaintingModelMaterial()
            println("model loaded")
        }
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment.id == R.id.arFragment) {
            arFragment = fragment as ArFragment
            arFragment!!.setOnSessionConfigurationListener(this)
        }
    }

    override fun onSessionConfiguration(session: Session, config: Config) {
        // Disable plane detection
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)
        database = AugmentedImageDatabase(session)
        val paintingImage = BitmapFactory.decodeResource(resources, R.drawable.blue)
        // Every image has to have its own unique String identifier
        database!!.addImage("painting", paintingImage)
        config.setAugmentedImageDatabase(database)

        // Check for image detection
        arFragment!!.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(
                augmentedImage
            )
        }
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

    private fun loadPaintingModel() {
        futures.add(
            ModelRenderable.builder()
                .setSource(this, Uri.parse("models/girlWithTheBlueRibbon.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept { model: ModelRenderable ->
                    //removing shadows for this Renderable
                    model.isShadowCaster = false
                    model.isShadowReceiver = true
                    plainVideoModel = model
                }
                .exceptionally {
                    Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show()
                    null
                }
        )
    }

    private fun loadPaintingModelMaterial() {
        val filamentEngine = EngineInstance.getEngine().filamentEngine
        MaterialBuilder.init()
        val materialBuilder: MaterialBuilder = MaterialBuilder()
            .platform(MaterialBuilder.Platform.MOBILE)
            .name("External Video Material")
            .require(MaterialBuilder.VertexAttribute.UV0)
            .shading(MaterialBuilder.Shading.UNLIT)
            .doubleSided(true)
            .samplerParameter(
                MaterialBuilder.SamplerType.SAMPLER_EXTERNAL,
                MaterialBuilder.SamplerFormat.FLOAT,
                MaterialBuilder.ParameterPrecision.DEFAULT,
                "videoTexture"
            )
            .optimization(MaterialBuilder.Optimization.NONE)
        val plainVideoMaterialPackage: MaterialPackage = materialBuilder
            .blending(MaterialBuilder.BlendingMode.OPAQUE)
            .material(
                "void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;\n" +
                        "}\n"
            )
            .build(filamentEngine)
        if (plainVideoMaterialPackage.isValid) {
            val buffer: ByteBuffer = plainVideoMaterialPackage.buffer
            futures.add(
                Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept { material: Material? ->
                        plainVideoMaterial = material
                    }
                    .exceptionally {
                        Toast.makeText(this, "Unable to load material", Toast.LENGTH_LONG)
                            .show()
                        null
                    }
            )
        }
        MaterialBuilder.shutdown()
    }

    private fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (paintingDetected) {
            return
        }
        if ((augmentedImage.trackingState == TrackingState.TRACKING
                    && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING)
        ) {

            // Setting anchor to the center of Augmented Image
            val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

            // If rabbit model haven't been placed yet and detected image has String identifier of "rabbit"
            // This is also example of model loading and placing at runtime
            if (!paintingDetected && (augmentedImage.name == "painting")) {
                paintingDetected = true
                Toast.makeText(this, "Painting tag detected", Toast.LENGTH_LONG).show()
                anchorNode.worldScale = Vector3(3.5f, 3.5f, 3.5f)
                arFragment!!.arSceneView.scene.addChild(anchorNode)
                futures.add(
                    ModelRenderable.builder()
                        .setSource(this, Uri.parse("models/girlWithTheBlueRibbon.glb"))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept { paintingModel: ModelRenderable? ->
                            val modelNode = TransformableNode(
                                arFragment!!.transformationSystem
                            )
                            modelNode.renderable = paintingModel
                            anchorNode.addChild(modelNode)
                        }
                        .exceptionally {
                            Toast.makeText(
                                this,
                                "Unable to load rabbit model",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            null
                        }
                )
            }
        }
        if (paintingDetected) {
            arFragment!!.instructionsController.setEnabled(
                InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false
            )
        }
    }
}