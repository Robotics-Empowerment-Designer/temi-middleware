/**
 * The MainActivity is the main activity of the application and implements the MQTT callback interface / Die MainActivity ist die Hauptaktivität der Anwendung und implementiert das MQTT-Callback-Interface
 * It serves as a user interface for MQTT communication and displays the connection with the MQTT broker / Sie dient als Benutzeroberfläche für die MQTT-Kommunikation und zeigt die Verbindung mit dem MQTT-Broker an
 */
package com.example.mqtt

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import coil.load
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.constants.Platform
import com.robotemi.sdk.constants.SdkConstants
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener
import com.robotemi.sdk.listeners.OnConstraintBeWithStatusChangedListener

import com.robotemi.sdk.listeners.OnDetectionStateChangedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnMovementStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotLiftedListener



import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.permission.Permission
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.OkHttpClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONException
import org.json.JSONObject

import com.robotemi.sdk.listeners.OnDetectionDataChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.listeners.OnUserInteractionChangedListener
import com.robotemi.sdk.model.DetectionData

import androidx.core.app.ActivityCompat


import androidx.core.content.ContextCompat
import org.eclipse.paho.client.mqttv3.MqttClient
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView

import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions


import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


// typealias LumaListener = (luma: Double, data: ByteArray) -> Unit


class MainActivity : AppCompatActivity(), MqttCallback, OnInitListener,
    OnGoToLocationStatusChangedListener, OnBeWithMeStatusChangedListener,
    OnConstraintBeWithStatusChangedListener, OnDetectionStateChangedListener,
    OnMovementStatusChangedListener, Robot.AsrListener, OnRobotReadyListener,
    OnDetectionDataChangedListener,
    OnUserInteractionChangedListener, OnCurrentPositionChangedListener {


    private var GET_AND_SENDOUT_CAMERAPICTURE_VIA_MQTT = true
    private var DISPLAY_CAMERA_FEED_ON_SCREEN = false //GET_AND_SENDOUT_CAMERAPICTURE_VIA_MQTT need to be activated for this working
    private var ageGenderEstimationActive = false


    //load the nodels
    private val modelFilenames = arrayOf(
        arrayOf("model_age_q.tflite", "model_gender_q.tflite"),
        arrayOf("model_age_nonq.tflite", "model_gender_nonq.tflite"),
        arrayOf("model_lite_age_q.tflite", "model_lite_gender_q.tflite"),
        arrayOf("model_lite_age_nonq.tflite", "model_lite_gender_nonq.tflite"),
    )

    // Instanz des MqttHandlers für MQTT-Kommunikation
    protected var mqttHandler: MqttHandler? = null
    private val CAMERA_REQUEST = 1888

    private var currentPayload: String = ""

    // Initialisierung des TextToSpeech-Objekts
    private var textToSpeech: TextToSpeech? = null
    private lateinit var robot: Robot
    private val TAG = MainActivity::class.java.getSimpleName()

    public lateinit var textView: TextView
    private lateinit var emojiLottie: LottieAnimationView
    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var btnGerman: ImageButton
    private lateinit var btnEnglish: ImageButton
    private lateinit var homeButton: ImageButton
    private lateinit var languageBox: LinearLayout
    /*------*/
    private var isUserInteracting : Boolean = false
    private var isHumanDetected : Boolean = false
    /*------*/
    /* */

    private lateinit var imageCapture: ImageCapture
    private val executor = Executors.newSingleThreadExecutor()

    // CameraX variables
    private lateinit var cameraExecutor: ExecutorService





    private var client: OkHttpClient? = null

    // CameraX variables
    private lateinit var previewView: PreviewView
    private val CAMERA_PERMISSION_CODE = 101

    //private lateinit var outputDirectory: File


    data class SkidJoyDir(
        var x: Float = 0F,
        var y: Float = 0F
    )


    private lateinit var uiManager: UIManager
    private lateinit var robotController: RobotController
    private lateinit var ageEstimator: AgeEstimator
    private lateinit var ageGenderEstimator: AgeGenderEstimator
    private lateinit var faceDetector: FaceDetector

    /**
     * Called when the activity is created / Wird aufgerufen, wenn die Aktivität erstellt wird
     */

    //    Helper function for mqtt connection:
    fun MqttHandler.smartConnect(
        brokerUrl: String,
        clientId: String,
        username: String? = null,
        password: String? = null
    ): Boolean {
        // Returns the result of the appropriate connect call
        return if (username.isNullOrBlank() || password.isNullOrBlank()) {
            // If no credentials, call the 2-argument version
            this.connect(brokerUrl, clientId)
        } else {
            // If credentials exist, call the 4-argument version
            this.connect(brokerUrl, clientId, username, password)
        }
    }


    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("RestrictedApi")
    private fun bindCamera() {
        Log.d(TAG, "bindCamera() called!")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            Log.d(TAG, "CameraProvider initialized.")
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Remove the preview setup = not display camera feed on screen
            var preview: Preview? = null
            if(DISPLAY_CAMERA_FEED_ON_SCREEN){
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
                }
            }


            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()

                val resolution = intArrayOf(320, 240)
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(resolution[0], resolution[1]))
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { imageProxy ->
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                                faceDetector.process(inputImage)
                                    .addOnSuccessListener { faces ->
                                        if (faces.isNotEmpty()) {
                                            val face = faces[0]
                                            val faceBitmap = mediaImageToBitmap(mediaImage, face.boundingBox)
                                            if (faceBitmap != null) {
                                                if (ageGenderEstimationActive) {
                                                    val (age, gender) = ageGenderEstimator.estimateAgeGender(faceBitmap)
                                                    Log.d("AGE_GENDER", "Age: $age, Gender: $gender")
                                                    publishMessage("temi/age_estimation", age)
                                                    publishMessage("temi/gender_estimation", gender)
                                                    ageGenderEstimationActive = false
                                                    unbindCamera()
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Face detection failed: ", e)
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        })
                    }

                // Bind only imageCapture and imageAnalyzer, without preview

                if(DISPLAY_CAMERA_FEED_ON_SCREEN){
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
                }else{
                    cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, imageAnalyzer)
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed.")
        } else {
            Log.d(TAG, "OpenCV initialization succeeded.")
        }*/

        // Logging for debugging / Protokollierung für Debugging
        //Log.e("Test", "onCreate: ")

        // Mapping XML objects / XML Ojekte mappen
        emojiLottie = findViewById<LottieAnimationView>(R.id.emojiLottie)
        emojiLottie.playAnimation()
        textView = findViewById(R.id.textHallo)
        imageView = findViewById(R.id.imageView)
        videoView = findViewById(R.id.videoView)
        btnGerman = findViewById((R.id.btnGerman))
        btnEnglish = findViewById((R.id.btnEnglish))
        homeButton = findViewById((R.id.homeButton))
        languageBox = findViewById((R.id.language_box))


        btnGerman.setOnClickListener{
            Log.i("Button","German button pressed")
            uiManager.clearTablet("nothing")
            publishMessage("temi/cancel_flow", "false")
            publishMessage("temi/reset_flow","true")
            languageBox.visibility = View.GONE
            emojiLottie.visibility = View.GONE
            homeButton.visibility = View.VISIBLE
            ageGenderEstimationActive = true
            bindCamera()
            publishMessage("temi/user_language", "german")
        }
        btnEnglish.setOnClickListener{
            Log.i("Button","English button pressed")
            uiManager.clearTablet("nothing")
            publishMessage("temi/cancel_flow", "false")
            publishMessage("temi/reset_flow","true")
            languageBox.visibility = View.GONE
            emojiLottie.visibility = View.GONE
            homeButton.visibility = View.VISIBLE
            ageGenderEstimationActive = true
            bindCamera()
            publishMessage("temi/user_language", "english")

        }

        homeButton.setOnClickListener{

            languageBox.visibility = View.VISIBLE
            emojiLottie.visibility = View.VISIBLE
            homeButton.visibility = View.GONE

            // stop flow if running
            publishMessage("temi/cancel_flow", "true")
            //robotController.interruptSpeech()
            uiManager.clearTablet("nothing")
        }

        // Create MqttHandler instance and connect to the broker / MqttHandler-Instanz erstellen und mit dem Broker verbinden
        mqttHandler = MqttHandler()



        // Initialize MQTT handler
        try {
            mqttHandler = MqttHandler().apply {

                if (USERNAME.isNullOrBlank() || PASSWORD.isNullOrBlank()) {
                    // Credentials are not provided, use the local version
                    println("Attempting local connection...")
                    connect(BROKER_URL, CLIENT_ID)
                }
                else {
                    // Credentials are provided, use the online version
                    println("Attempting online connection...")
                    connect(BROKER_URL, CLIENT_ID, USERNAME, PASSWORD)
                }

                setCallback(this@MainActivity)
                Log.d("MQTT", "Stelle Verbindung her")
            }
            Log.w("line 191 mqtt connection", "Sucess")
        }
        catch (e: Exception){
            Log.w("line 191 mqtt connection", e.toString())

        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission NOT granted! Requesting permission...")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            Log.d(TAG, "Camera permission already granted.")
            if(GET_AND_SENDOUT_CAMERAPICTURE_VIA_MQTT) {
                Log.d(TAG, "Starting camera and MQTT setup...")
                bindCamera()
                setupHttpConnection()
            }

        }

        previewView = findViewById(R.id.previewView)
        //Call bindCameraPreview to set up camera preview

        // Create output directory

        cameraExecutor = Executors.newSingleThreadExecutor()
        client = OkHttpClient.Builder().build()





        //++++++++++++++




        // Initialisierung des TextToSpeech-Objekts in der onCreate-Methode
        textToSpeech = TextToSpeech(this, this)

        // Initialize the Robot object / Initialisieren des Roboterobjekts
        robot = Robot.getInstance()

        // Hide top menu bar of the robot / Obere Menüleiste des Roboters verstecken
        robot!!.hideTopBar()

        // Deactivate robot buttons / Roboter-Knöpfe deaktivieren
        robot!!.isHardButtonsDisabled = true

        // Set this activity as a callback for MQTT messages / Diese Aktivität als Callback für MQTT-Nachrichten setzen
        mqttHandler!!.setCallback(this)


        uiManager = UIManager(imageView, videoView, textView) { topic, message ->
            if (mqttHandler!!.isConnected) {
                Log.d("MQTT", "Verbindung steht, sendet Daten")
                mqttHandler!!.publish(topic, message)
            } else {
                try {
                    if (mqttHandler!!.smartConnect(BROKER_URL, CLIENT_ID, USERNAME, PASSWORD)) {
                        Log.d("MQTT", "Verbindung zum Broker wiederhergestellt")
                        mqttHandler!!.publish(topic, message)
                    } else {
                        Log.d("MQTT", "Fehler beim Herstellen der Verbindung zum Broker")
                    }
                    Log.w("Line 252 if", "Success")
                }catch (e: Exception){
                    Log.w("Line 252 if", e.toString())
                }
            }
        }

        // Initialisierung des TextToSpeech-Objekts in der onCreate-Methode
        textToSpeech = TextToSpeech(this, this)


        robotController = RobotController(this,robot, textToSpeech, ::publishMessage)
        ageEstimator = AgeEstimator(this)
        ageGenderEstimator = AgeGenderEstimator(this)

        // Initialize FaceDetector
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        faceDetector = FaceDetection.getClient(options)

        // Beispiel: Ein MQTT-Thema abonnieren
        subtoTopic()

        // Set up the OnRobotLiftedListener / Einrichten des OnRobotLiftedListener
        robot!!.addOnRobotLiftedListener(object : OnRobotLiftedListener {
            override fun onRobotLifted(isLifted: Boolean, s: String) {
                // This method will be called when the robot is lifted or set down / Diese Methode wird aufgerufen, wenn der Roboter angehoben oder abgesetzt wird
                if (!isLifted) {
                    // The robot has been set down (action like goTo completed) / Der Roboter wurde abgesetzt (Aktion wie goTo abgeschlossen)
                    Log.d("move", "Robot has been set down.")
                }
            }
        })
        if (requestPermissionIfNeeded(robot, Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
    }



    private fun mediaImageToBitmap(mediaImage: android.media.Image, boundingBox: Rect): Bitmap? {
        val yBuffer = mediaImage.planes[0].buffer
        val uBuffer = mediaImage.planes[1].buffer
        val vBuffer = mediaImage.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, mediaImage.width, mediaImage.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        val fullBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val x = boundingBox.left
        val y = boundingBox.top
        val width = boundingBox.width()
        val height = boundingBox.height()

        if (x < 0 || y < 0 || x + width > fullBitmap.width || y + height > fullBitmap.height) {
            Log.e(TAG, "Invalid bounding box for cropping.")
            return null
        }
        return Bitmap.createBitmap(fullBitmap, x, y, width, height)
    }


    private fun handleMessage(topic: String, payload: String) {
        when (topic) {
            "temi/tts" -> {//working
                Log.d("MQTT", "Es wird gesprochen: $payload")
//                CoroutineScope(Dispatchers.Main).launch {
//                    robotController.say(payload)
//                }
                robotController.say(payload)
            }
            "temi/get_locations" -> {// working
                robotController.sendLocationList()
            }
            "temi/stop" -> {//working
                Log.w("Cancel","interrupt Function 1")
                robotController.interruptSpeech()
            }
            "temi/goto" -> {//working
                robotController.goTo(payload)
            }
            "temi/txt" -> {//working
                uiManager.showText(payload)
            }
            "temi/img" -> {//working
                uiManager.showImage(payload)
            }
            "temi/vid" -> {//working
                uiManager.showVideo(payload)
            }
            "temi/clear" -> {//working
                uiManager.clearTablet(payload)
            }
            "temi/wait" -> {//working
                uiManager.wait(payload)
            }
            "temi/gotoposition" -> {//working
                robotController.goToPosition(payload)
            }
            "temi/gotopositionwithbypass" -> {//working
                robotController.goToPositionWithByPass(payload)
            }
            "temi/stopmovement" -> {//working
                robotController.stopMovement(payload)
            }
            "temi/setgotospeed" -> {//working
                robotController.setGoToSpeed(payload)
            }
            "temi/tiltAngle" -> {//working
                robotController.tiltAngle(payload)
            }
            "temi/skidJoyForward" -> {//working
                robotController.skidJoyForward(payload)
            }
            "temi/skidJoy" -> {//working
                robotController.skidJoy(payload)
            }
            "temi/turnBy" -> {//TODO
                robotController.turnBy(payload)
            }
            "temi/waitforkeyword" -> {//working
                try {
                    val jsonObject = JSONObject(payload)
                    val question = jsonObject.getString("question")
                    val keywordsPayload = jsonObject.getString("payload")
                    val id = jsonObject.getString("id")
                    val language = jsonObject.getString("language")
                    robotController.askQuestion(question, keywordsPayload, id, language)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.e("MQTT", "Failed to parse JSON payload: ${e.message}")
                }
            }
            "temi/tele" -> {//not tested
                try {
                    val json = JSONObject(payload)
                    val displayName = json.getString("value")
                    val peerId = json.getString("id")
                    val platform = Platform.MOBILE // Beispiel, wie du die Platform setzen könntest
                    robotController.startTelepresence(displayName, peerId, platform)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.e("MQTT", "Failed to parse JSON payload: ${e.message}")
                }
            }
            /*"temi/camera" -> {
                when (payload) {
                    "true" -> runOnUiThread { bindCamera() }
                    "false" -> runOnUiThread { unbindCamera() }
                }
            }*/
            else -> {
                Log.d("MQTT", "Unbekanntes Thema: $topic")
            }
        }
    }


    private fun setupHttpConnection() {
        client = OkHttpClient.Builder().build()
    }










    /**
     *+++++++++++++++++++++++++++++++++++++++++++++++
     */
    public override fun onStart() {
        super.onStart()

        // Add robot event listeners
        robot!!.addOnRobotReadyListener(this)
        robot!!.addOnDetectionStateChangedListener(this)
        robot!!.addOnDetectionDataChangedListener(this)
        robot!!.addOnUserInteractionChangedListener(this)
        robot.addOnCurrentPositionChangedListener(this)
        robot!!.addAsrListener(this)
    }

    public override fun onStop() {
        super.onStop()

        // Remove robot event listeners
        robot!!.removeOnRobotReadyListener(this)
        robot!!.removeOnDetectionStateChangedListener(this)
        robot!!.removeOnDetectionDataChangedListener(this)
        robot!!.removeOnUserInteractionChangedListener(this)
        robot.removeOnCurrentPositionChangedListener(this)
        robot!!.removeAsrListener(this)
    }

    /**
     * Called when the activity is destroyed / Wird aufgerufen, wenn die Aktivität zerstört wird
     */
    // Ensures that the TextToSpeech object is released when the activity is destroyed / Stellt sicher, dass das TextToSpeech-Objekt freigegeben wird, wenn die Aktivität zerstört wird
    override fun onDestroy() {
        // Sharing TextToSpeech resources / Freigeben der TextToSpeech-Ressourcen
        if (textToSpeech != null) {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }
        // Disconnecting the MQTT connection / Trennen der MQTT-Verbindung
        mqttHandler!!.disconnect()
        super.onDestroy()
    }

    // TextToSpeech.OnInitListener-Methode
    override fun onInit(status: Int) {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.GERMAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Deutsch wird nicht unterstützt")
                } else {
                    Log.d("TTS", "TextToSpeech auf Deutsch gesetzt")
                }
            } else {
                Log.e("TTS", "TextToSpeech Initialisierung fehlgeschlagen")
            }
        }
    }

    /********************** Detection and Interaction state **********************/
    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            Log.i(TAG, "Robot is ready")
            robot!!.hideTopBar() // hide temi's top action bar when skill is active
            Log.i(TAG, "Set detection mode: ON")
            robot!!.setDetectionModeOn(
                true,
                2.0f
            ) // Set detection mode on; set detection distance to be 2.0 m
            Log.i(TAG, "Set track user on : ON")
            robot!!.trackUserOn = true // Set tracking mode on
            // Note: When exiting the application, track user will still be enabled unless manually disabled
        }
    }

    override fun onDetectionStateChanged(state: Int) {
        //val textView = findViewById<TextView>(R.id.detectionState)

        when (state) {
            OnDetectionStateChangedListener.IDLE -> {
                // No active detection and/or 10 seconds have passed since the last detection was lost
                Log.i(TAG, "OnDetectionStateChanged: IDLE")
                //textView.text = "OnDetectionStateChanged: IDLE"
            }

            OnDetectionStateChangedListener.LOST -> {
                // When human-target is lost
                Log.i(TAG, "OnDetectionStateChanged: LOST")
                //textView.text = "OnDetectionStateChanged: LOST"
            }

            OnDetectionStateChangedListener.DETECTED -> {
                // Human is detected
                Log.i(TAG, "OnDetectionStateChanged: DETECTED")
                //textView.text = "OnDetectionStateChanged: DETECTED"
                isHumanDetected=true
            }

            else -> {
                // This should not happen
                Log.i(TAG, "OnDetectionStateChanged: UNKNOWN")
                //textView.text = "OnDetectionStateChanged: UNKNOWN"
            }
        }
        checkConditionAndTriggerNextNode()
    }



    override fun onDetectionDataChanged(detectionData: DetectionData) {
        if (detectionData.isDetected) {
            //val textView = findViewById<TextView>(R.id.detectionData)
            //textView.text = "OnDetectionDataChanged: " + detectionData.distance + " m"
            Log.i(TAG, "OnDetectionDataChanged: " + detectionData.distance + " m")
        }
    }

    override fun onUserInteraction(isInteracting: Boolean) {
        //val textView = findViewById<TextView>(R.id.userInteraction)
        isUserInteracting = isInteracting
        if (isInteracting) {
            // User is interacting with the robot:
            // - User is detected
            // - User is interacting by touch, voice, or in telepresence-mode
            // - Robot is moving
            Log.i(TAG, "OnUserInteraction: TRUE")
            //textView.text = "OnUserInteraction: TRUE"
        } else {
            // User is not interacting with the robot
            Log.i(TAG, "OnUserInteraction: FALSE")
            //textView.text = "OnUserInteraction: FALSE"
        }
        checkConditionAndTriggerNextNode()
    }

    private fun checkConditionAndTriggerNextNode() {
        if(isHumanDetected && isUserInteracting){
            bindCamera()
            Log.i(TAG, "Condition Met User Detected and Interacting")
            publishMessage("temi/face_detected","true")
            //takePhoto()
            //reset the flags
            isHumanDetected=false
            isUserInteracting=false
        }
    }

    /*************************/


    /**
     * publish the asr result on mqtt
     */
    override fun onAsrResult(asrResult: String) {
        Log.d("ASR_Result", "ASR Result: $asrResult")
        mqttHandler!!.publish("temi/asrResult", asrResult)
        /**old stuff
        // Handle ASR result here
        // asrResult contains the user's answer
        // sttLanguage contains the language information
        // Perform any necessary processing with the user's answer
        Log.d("ASR_Result", "ASR Result: $asrResult")

        // Unregister AsrListener after receiving the answer
        robot!!.removeAsrListener(this)
        finishConversation(this.currentFocus)

        // Konvertiere currentPayload in eine Liste von Strings
        val keywords = currentPayload
        .removeSurrounding("[", "]")
        .split(",")
        .map { it.trim().removeSurrounding("\"") }

        Log.d("Keyword", "Keywords: $keywords")

        // Überprüfe, ob asrResult eines der Schlüsselwörter enthält
        val matchedKeyword = KeywordChecker.containsKeyword(asrResult, keywords)

        if (matchedKeyword != null) {
        Log.d("Keyword", "Keyword Result positiv: $matchedKeyword")
        publishMessage("temi/wfk/finished", matchedKeyword)
        retries = 0 // Reset retries
        } else {
        // Wenn keine Übereinstimmung gefunden wurde
        Log.d("Keyword", "Keyword nicht vorhanden: $asrResult")
        if (retries < maxRetries) {
        retries++
        Log.d("Keyword", "Retrying... ($retries/$maxRetries)")

        askQuestion(lastAskedQuestion, currentPayload) // Wiederhole die Frage
        } else {
        publishMessage("temi/wfk/finished", "not found")
        retries = 0 // Reset retries
        }
        }
         **/
    }






    private fun finishConversation(view: View?) {
        robot!!.finishConversation()
    }


    /**
     * Publishes an MQTT message on a specific topic / Veröffentlicht eine MQTT-Nachricht zu einem bestimmten Thema
     *
     * @param topic The MQTT topic for which the message is to be published / Das MQTT-Thema, zu dem die Nachricht veröffentlicht werden soll
     * @param msg   The content of the message to be published / Der Inhalt der zu veröffentlichenden Nachricht
     */
    private fun publishMessage(topic: String, msg: String) {
        if (mqttHandler!!.isConnected) {
            // Connection exists, publish message / Verbindung besteht, Nachricht veröffentlichen
            Log.d("MQTT", "Verbindung steht, sendet Daten")
            mqttHandler!!.publish(topic, msg)
        } else {
            try {// Try to re-establish the connection / Versuchen die Verbindung wiederherzustellen
                if (mqttHandler!!.smartConnect(BROKER_URL, CLIENT_ID, USERNAME, PASSWORD)) {


                    Log.d("MQTT", "Verbindung zum Broker wiederhergestellt")
                    // Publish message after the connection has been re-established / Nachricht veröffentlichen, nachdem die Verbindung wiederhergestellt wurde
                    mqttHandler!!.publish("test_sensor_data", "Guten Tag")
                } else {
                    Log.d("MQTT", "Fehler beim Herstellen der Verbindung zum Broker")
                }
                Log.w("Line 848", "Success")
            } catch (e: Exception){
                Log.w("Line 848", e.toString())
            }
        }
    }

    // Method for subscribing to an MQTT topic / Methode zum Abonnieren eines MQTT-Themas
    private fun subtoTopic() {
        Log.d("MQTT", "Abonniere Thema: " + "temi/#")
        mqttHandler!!.subscribe("temi/#")
    }

    /**
     * Wird aufgerufen, wenn eine MQTT-Nachricht empfangen wird.
     *
     * @param topic   Das Thema, auf dem die Nachricht empfangen wurde.
     * @param message Die empfangene MQTT-Nachricht.
     */
    override fun messageArrived(topic: String, message: MqttMessage) {
        runOnUiThread {
            try {
                val payload = String(message.payload)
                Log.d("MQTT", "Nachricht empfangen auf Thema: $topic, Nachricht: $payload")
                handleMessage(topic, payload)
                // Weitere Verarbeitung hier
                /*if (topic == "temi/camera") {
                    if (payload == "true") {
                        runOnUiThread { bindCamera() }
                    } else if (payload == "false") {
                        runOnUiThread { unbindCamera() }
                    }
                }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Called if the connection to the MQTT broker is lost / Wird aufgerufen, wenn die Verbindung zum MQTT-Broker verloren geht
     *
     * @param cause The cause of the loss of connection / Die Ursache des Verbindungsverlusts
     */
    override fun connectionLost(cause: Throwable) {
        Log.d("MQTT", "Verbindung verloren: $cause")
    }

    /**
     * Called when an MQTT message has been successfully delivered / Wird aufgerufen, wenn eine MQTT-Nachricht erfolgreich zugestellt wurde
     *
     * @param token The token for the delivered message / Der Token für die zugestellte Nachricht
     */
    override fun deliveryComplete(token: IMqttDeliveryToken) {
        Log.d("MQTT", "Nachricht erfolgreich zugestellt: $token")
    }


    private fun unbindCamera() {
        // Stop capturing frames and unbind the camera
        imageCapture?.let {
            executor.shutdown() // Stop the frame capture task
        }
    }

    // Implementation of the empty methods of the listener interfaces / Implementierung der leeren Methoden der Listener-Interfaces
    //override fun onAsrResult(s: String) {}
    //override fun onTtsStatusChanged(ttsRequest: TtsRequest) {}
    override fun onBeWithMeStatusChanged(s: String) {}
    override fun onConstraintBeWithStatusChanged(b: Boolean) {}
    override fun onGoToLocationStatusChanged(s: String, s1: String, i: Int, s2: String) {}
    override fun onMovementStatusChanged(s: String, s1: String) {}



    companion object {
        private const val TAG = "CameraXApp"
        private const val SERVER_URL = "http://172.30.32.34:5000/video_feed"

        private const val CAMERA_PERMISSION_REQUEST = 100
        //private const val TAG = "MainActivity"
        // Konstanten für die Verbindung mit dem MQTT-Broker


//      For Localhost
        protected const val BROKER_URL = "tcp://0.0.0.0:1885" //
        protected const val CLIENT_ID = "hi"
        protected val USERNAME = null
        protected val PASSWORD = null



        //
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10

        const val REQUEST_CODE_NORMAL = 0
        //Method from temi sdk (check permission and return true if request was done in case no permission was granted
        @CheckResult
        fun requestPermissionIfNeeded(robot: Robot, permission: Permission, requestCode: Int): Boolean {
            if (robot.checkSelfPermission(permission) == Permission.GRANTED) {
                return false
            }
            robot.requestPermissions(listOf(permission), requestCode)
            return true
        }
    }

    override fun onCurrentPositionChanged(position: Position) {

        val gson = Gson()
        val jsonString = gson.toJson(position)
        mqttHandler!!.publish("temi/position", jsonString)
    }


}