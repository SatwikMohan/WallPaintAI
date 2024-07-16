package com.example.wallpaintai

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.java.GenerativeModelFutures
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Config.LightEstimationMode
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.HandMotionView
import com.google.ar.sceneform.ux.TransformableNode
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale


class MainActivity : AppCompatActivity(),ClickInterface, TextToSpeech.OnInitListener {

    lateinit var arFragment: ArFragment
    lateinit var frame:Frame
    lateinit var color: com.google.ar.sceneform.rendering.Color
    val nodeList:ArrayList<AnchorNode> = ArrayList()
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tts = TextToSpeech(this, this)
        val arSession = Session(this)
        val config = Config(arSession)
        config.planeFindingMode = Config.PlaneFindingMode.VERTICAL
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.lightEstimationMode = LightEstimationMode.AMBIENT_INTENSITY
        config.setFocusMode(Config.FocusMode.AUTO)
        arSession.configure(config)
        arFragment=(supportFragmentManager.findFragmentById(R.id.ArFragment) as ArFragment)
        arFragment.arSceneView.setupSession(arSession)
        arFragment.arSceneView.scene.setUseHdrLightEstimate(true)
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
        arFragment.arSceneView.planeRenderer.isEnabled = false

        val progressBar = findViewById<ProgressBar>(R.id.ProgressBar)
        val captureImage = findViewById<FloatingActionButton>(R.id.CaptureImage)
        val text1 = findViewById<TextView>(R.id.Text1)
        val text2 = findViewById<TextView>(R.id.text2)
        val reCaptureImage = findViewById<FloatingActionButton>(R.id.ReCaptureImage)
        val deleteNode = findViewById<FloatingActionButton>(R.id.DeleteNode)
        val recyclerView: RecyclerView = findViewById(R.id.RecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        arFragment.arSceneView.scene.addOnUpdateListener {
            frame = arFragment.arSceneView.session!!.update()
            val planes = frame.getUpdatedTrackables(Plane::class.java)
            if(text2.visibility!=View.GONE){
                for (plane in planes) {
                    if (plane.trackingState == TrackingState.TRACKING) {
                        // Check if plane is newly detected
                        text2.text = "Choose color and click on screen to place paint"
                    }else{
                        text2.text = "Move the camera slowly facing the wall"
                    }
                }
            }
        }

        captureImage.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val image = frame.acquireCameraImage()
            captureImage.visibility = View.GONE
            reCaptureImage.visibility = View.VISIBLE
            deleteNode.visibility = View.VISIBLE
            text1.visibility = View.GONE
            val generativeModel = GenerativeModel(
                // For text-only input, use the gemini-pro model
                modelName = "gemini-1.5-flash",
                // Access your API key as a Build Configuration variable (see "Set up your API key" above)
                apiKey = BuildConfig.API_KEY
            )
            val generativeModelFutures = GenerativeModelFutures.from(generativeModel)
            val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val yuvToRgbConverter = YuvToRgbConverter(this)
            yuvToRgbConverter.yuvToRgb(image, bmp)
            val prompt = Content.Builder()
                .image(bmp)
                .text("Check the old color of the wall and Suggest me color for this wall paint in the image and give reason along with it. You can also add the old color with some improvements. In your reason suggest your opinion on every kind of furniture, room purpose, mood, room style, choices etc. Your response should be in Json String format and should contain a Json array with its Json objects and each JSON object contains the color, HexCode and reason. Try to add the old color in the list too. Response should not contains anything else other than JSON so that it is in valid format. If the wall is not in a good condition suggest your color opinion on it and also add some repairing suggestion in the reason")
                .build()
            val executor  = Runnable::run
            val future:ListenableFuture<GenerateContentResponse> = generativeModelFutures.generateContent(prompt)
            Futures.addCallback(
                future,
                object : FutureCallback<GenerateContentResponse> {
                    override fun onSuccess(result: GenerateContentResponse) {
                        // handle success
                        progressBar.visibility = View.GONE
                        var jsonString = result.text.toString().trimIndent()
                        Log.d("Gemini",jsonString)
//                        val ind = jsonString.indexOf('[')
//                        jsonString = jsonString.substring(ind)
                        jsonString = jsonString.replace("`","").replace("json","").replace(Regex("\\s{2,}"), "")
                        Log.d("Gemini",jsonString)
                        try{
                            val aiSuggestions = Klaxon().parseArray<AISuggestionObject>(jsonString)!!
                            Log.d("Gemini",aiSuggestions?.size.toString())
                            recyclerView.visibility = View.VISIBLE
                            text2.visibility = View.VISIBLE
                            val adapter = Adapter(aiSuggestions,this@MainActivity)
                            recyclerView.adapter = adapter
                        }catch(e:Exception){
                            Log.d("Gemini",e.toString())
                            Toast.makeText(applicationContext,"Something went wrong",Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onFailure(t: Throwable) {
                        // handle failure
                        progressBar.visibility = View.GONE
                        Log.d("Gemini",t.toString())
                        Toast.makeText(applicationContext,"Something went wrong",Toast.LENGTH_LONG).show()
                    }
                },
                // causes the callbacks to be executed on the main (UI) thread
                executor
            )
        }

        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            val anchor = hitResult.createAnchor()
            placeObject(arFragment, anchor, plane)
        }

        reCaptureImage.setOnClickListener {
            arFragment.planeDiscoveryController.hide()
            arFragment.planeDiscoveryController.setInstructionView(null)
            arFragment.arSceneView.planeRenderer.isEnabled = false
            captureImage.visibility = View.VISIBLE
            text1.visibility = View.VISIBLE
            reCaptureImage.visibility  = View.GONE
            progressBar.visibility = View.GONE
            recyclerView.visibility = View.GONE
            deleteNode.visibility = View.GONE
            for(anchorNode in nodeList){
                arFragment.arSceneView.scene.removeChild(anchorNode)
            }
            text2.visibility = View.GONE
        }

        deleteNode.setOnClickListener {
            if(nodeList.size>0){
                arFragment.arSceneView.scene.removeChild(nodeList.last())
                nodeList.removeLast()
            }
        }

    }

    private fun placeObject(arFragment: ArFragment, anchor: Anchor, plane:Plane) {
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)
//        val color = com.google.ar.sceneform.rendering.Color(Color.RED)
        val materialFuture = MaterialFactory.makeOpaqueWithColor(this, color)
        val pose = plane.centerPose
        materialFuture.thenAccept { material ->
            val model = ShapeFactory.makeCube(
                Vector3(0.1f, 0.1f, 0.01f),  // Adjust size as needed
                Vector3(0f, 0f, 0f),
                material
            )
            model.isShadowCaster = false
            model.isShadowReceiver = false
//            val modelNode = Node()
//            modelNode.setParent(anchorNode)
//            modelNode.renderable = model
//            // Ensure the tile is vertical
//            val rotationQuaternion = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 90f)
//            modelNode.localRotation = rotationQuaternion
            anchorNode.addChild(TransformableNode(arFragment.transformationSystem).apply {
                renderable = model
                localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 90f)
                //localPosition = Vector3(0.0f, 0.0f, 0.0f)
            })
            nodeList.add(anchorNode)
        }

    }

    override fun onClick(colour:Int) {
        arFragment.planeDiscoveryController.show()
        arFragment.arSceneView.planeRenderer.isEnabled = true
        color = com.google.ar.sceneform.rendering.Color(colour)
        for(anchorNode in nodeList){
            arFragment.arSceneView.scene.removeChild(anchorNode)
        }
    }

    override fun onLongClick(reason: String) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Gemini's Study")
        alertDialog.setMessage(reason)
        alertDialog.setNegativeButton("Dismiss",DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
        })
        tts.speak(reason, TextToSpeech.QUEUE_FLUSH, null, "")
        alertDialog.setOnDismissListener {
            tts.stop()
        }
        alertDialog.show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            //tts.speak("Hello", TextToSpeech.QUEUE_FLUSH, null, "")
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported.
                // Handle error appropriately.
                Toast.makeText(applicationContext,"English US not available",Toast.LENGTH_LONG).show()
            }
        } else {
            // Initialization failed.
            // Handle error appropriately.
            Toast.makeText(applicationContext,"TTS Failed",Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        // Shutdown TextToSpeech when activity is destroyed to release resources.
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}