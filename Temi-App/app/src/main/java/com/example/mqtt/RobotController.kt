package com.example.mqtt

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.example.mqtt.MainActivity.Companion.REQUEST_CODE_NORMAL
import com.example.mqtt.MainActivity.SkidJoyDir
import com.google.gson.Gson

import com.robotemi.sdk.Robot
import com.robotemi.sdk.Robot.AsrListener
import com.robotemi.sdk.constants.Platform
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.permission.Permission
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

import com.robotemi.sdk.TtsRequest.Companion.create
import com.robotemi.sdk.TtsRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class RobotController(
        private val context: Context,
        private val robot: Robot?,
        private val textToSpeech: TextToSpeech?,
        private val publishMessage: (String, String) -> Unit
) :  Robot.AsrListener {

    private var doneMessageSent = false
    private var startedGoing = false
    private var lastAskedQuestion: String = ""
    private var retries = 0
    private val maxRetries = 3
    private var originalVolume: Int = -1
    private val volumeIncrement = 2
    private var audioManager: AudioManager
    private var currentPayload: String = ""
    private var currentId: String = ""
    private var isSpeaking = false
    private var ttsJob : Job? = null
    private val ttsScope = CoroutineScope(Dispatchers.Main)
    private var goToListener : OnGoToLocationStatusChangedListener? = null
    private var askquestion_language = "German"
    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }





    fun sendLocationList() {
        val locations = robot?.locations ?: return
        val json = Gson().toJson(locations)
        publishMessage("temi/locations", json)
    }


    /**
     * Make the robot go to a saved location
     * format: just use the locations name e.g. "home base"
     */
    fun goTo(jsonString: String) {
        val jsonObject = JSONObject(jsonString)
        val text = jsonObject.getString("text")
        val id = jsonObject.getString("id")
        Log.i("goto__1", id)
        doneMessageSent = false
        startedGoing = false

        robot?.goTo(text) // Roboter fährt zum Checkpoint
        Log.i("goto__2", id)

        // Remove any previous listener (if exists)
        goToListener?.let { robot?.removeOnGoToLocationStatusChangedListener(it) }

        goToListener = object : OnGoToLocationStatusChangedListener {
            override fun onGoToLocationStatusChanged(
                location: String,
                status: String,
                descriptionId: Int,
                description: String
            ) {
                Log.i("goto__2", id)
                when (status) {
                    OnGoToLocationStatusChangedListener.START -> { /* Initial Start */
                        Log.i("goto__3", id)
                    }

                    OnGoToLocationStatusChangedListener.CALCULATING -> { /* Route wird berechnet */
                        Log.i("goto__4", id)
                    }

                    OnGoToLocationStatusChangedListener.GOING -> {
                        Log.i("goto__5", id)
                        Log.d("goto", "Auf dem Weg")
                        if (!startedGoing) {
                            Log.i("goto__6", id)
                            publishMessage("temi/goto/going", "going")
                            startedGoing = true
                            Log.i("goto__7", id)
                        }
                    }


                    OnGoToLocationStatusChangedListener.COMPLETE -> {
                        Log.i("goto__8", id)
                        if (!doneMessageSent) {
                            Log.i("goto__9", id)
                            publishMessage("temi/goto/finished/$id", "done")
                            doneMessageSent = true
                            Log.i("goto__10", id)
                        }
                    }

                    OnGoToLocationStatusChangedListener.ABORT -> { /* Abbruchstatus */
                        Log.i("goto__11", id)
                    }

                    OnGoToLocationStatusChangedListener.REPOSING -> { /* Repos-Status */
                        Log.i("goto__12", id)
                    }

                    else -> { /* Andere Statusfälle */
                    }
                }
                Log.i("goto__13", id)
            }
        }
        robot?.addOnGoToLocationStatusChangedListener(goToListener!!)
        robot?.goTo(text)

        Log.i("goto__14", id)

    }

    /**
     * making temi say text in the specified language
     * format: {"text": "what is you name", "language":"en"}
     */

//    OLD TEMI SAY
//    fun say(jsonString: String) {
//        textToSpeech?.let { tts ->
//            try {
//                val jsonObject = JSONObject(jsonString)
//                val text = jsonObject.getString("text")
//                val language = jsonObject.getString("language")
//
//                val locale = when (language) {
//                    "en" -> Locale.ENGLISH
//                    else -> Locale.GERMAN
//                }
//
//                val result = tts.setLanguage(locale)
//                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                    Log.e("TTS", "Sprache nicht unterstützt oder fehlende Daten.")
//                    return
//                }
//
//                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "Id1")
//
//                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//                    override fun onStart(utteranceId: String) {
//                        Log.d("TTS", "Sprachausgabe gestartet: $utteranceId")
//                        publishMessage("temi/tts/finished", "warte")
//                    }
//
//                    override fun onDone(utteranceId: String) {
//                        Log.d("TTS", "Sprachausgabe abgeschlossen: $utteranceId")
//                        publishMessage("temi/tts/finished", "done")
//                    }
//
//                    override fun onError(utteranceId: String) {
//                        Log.e("TTS", "Fehler bei der Sprachausgabe: $utteranceId")
//                    }
//                })
//            } catch (e: JSONException) {
//                Log.e("TTS", "Fehler beim Parsen des JSON-Strings: $e")
//            }
//        }
//    }


    private fun estimateSpeechDuration(text: String, speechRate: Float = 1.0f): Long {
        // Average speech rate (words per second) at normal speed
        val averageWordsPerSecond = 2.6f * speechRate // Adjust based on your testing
        val wordCount = text.split("\\s+".toRegex()).size
        return (wordCount / averageWordsPerSecond * 1000).toLong() // Return in milliseconds

    }
    
    fun say(jsonString: String) {
        // Cancel any ongoing Tts
        ttsJob?.cancel()

        val jsonObject = JSONObject(jsonString)
        val text = jsonObject.getString("text")
        val language = jsonObject.getString("language")
        val animation = jsonObject.optString("animation","false") == "true"
        val id = jsonObject.getString("id")

        val locale = when (language) {
            "english" -> TtsRequest.Language.EN_US
            else -> TtsRequest.Language.DE_DE
        }

        val speechduration = estimateSpeechDuration(text) + 2000
        Log.w("speechrate", (speechduration).toString())

        val robot = Robot.getInstance()

        val ttsRequest = create(text,  false, locale, showAnimationOnly = animation)
        isSpeaking = true
        Log.w("TTS","Speaking: $text | Animation: $animation")

        ttsJob = ttsScope.launch{
            robot.speak(ttsRequest)
            delay(speechduration)
            if (isSpeaking){
                isSpeaking = false
                Log.w("Cancel","Ende")
                publishMessage("temi/tts/finished/$id", "done")

            }
        }
    }

    fun interruptSpeech(){

        if(isSpeaking){
            ttsJob?.cancel()
            isSpeaking = false
            Robot.getInstance().speak(create("",false,TtsRequest.Language.DE_DE))
            publishMessage("temi/tts/finished", "done")

        }
    }

    /**
     * Make temi ask a question and the start the ASR (automatic speech recognition) => what is captured with this in called back in onAsrResult method
     */

    fun askQuestion(originalQuestion: String, payload: String, id: String, language: String) {
        lastAskedQuestion = originalQuestion
        currentPayload = payload
        currentId = id
        askquestion_language = language
        robot?.askQuestion(originalQuestion)
        robot?.addAsrListener(this) // Hier wird die Klasse als ASR-Listener hinzugefügt
    }

    /**
     * Callback method of all which is captured in the asr of the temi and publushing on a topic if a keyword was said/captured
     */
    override fun onAsrResult(asrResult: String) {
        Log.d("ASR_Result", "ASR Result: $asrResult")
        robot?.removeAsrListener(this)
        robot?.finishConversation()

        val keywords = currentPayload
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }

        Log.d("Keyword", "Keywords: $keywords")

        val matchedKeyword = KeywordChecker.containsKeyword(asrResult, keywords)

        if (matchedKeyword != null) {
            Log.d("Keyword", "Keyword Result positiv: $matchedKeyword")
            publishMessage("temi/wfk/finished/$currentId", matchedKeyword)
            if(retries > 0){
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume,0)
                Log.w("Volume", "Reset volume to original level: $originalVolume")
            }
            retries = 0 // Reset retries
        } else {
            Log.d("Keyword", "Keyword nicht vorhanden: $asrResult")
            if (retries < maxRetries) {
                retries++
                Log.d("Keyword", "Retrying... ($retries/$maxRetries)")

                increasesVolumeLevel()

                if(askquestion_language == "English"){
                    if (!lastAskedQuestion.startsWith("I misunderstood you, I'll repeat the question again")) {
                        lastAskedQuestion = "I misunderstood you, I'll repeat the question again: $lastAskedQuestion"
                    }
                }
                else{
                    if (!lastAskedQuestion.startsWith("Ich habe Sie falsch verstanden, ich wiederhole die Frage nochmal")) {
                        lastAskedQuestion = "Ich habe Sie falsch verstanden, ich wiederhole die Frage nochmal: $lastAskedQuestion"
                    }
                }

                askQuestion(lastAskedQuestion, currentPayload,currentId, askquestion_language) // Wiederhole die Frage
            } else {
                publishMessage("temi/wfk/finished", "not found")
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,originalVolume,0)
                Log.w("Volume","Reset volume to original level after mac retries: $originalVolume")
                retries = 0 // Reset retries
            }
        }
    }

    /**
     * Increases the volume for asking questions
     */

    fun increasesVolumeLevel(){

        try{
            if(retries == 1 && originalVolume == -1){
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                Log.w("Volume", "stored original volume: $originalVolume")
            }
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val newVolume = (currentVolume + volumeIncrement)

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,newVolume,0)
            Log.w("Volume", "Increased volume from $currentVolume to $newVolume (retry $retries)")
        } catch (e:Exception){
            Log.e("Volume", "Failed to adjust volume: ${e.message}")
        }
    }



    /**
     * TODO
     */
    fun startTelepresence(displayName: String?, peerId: String?, platform: Platform?) {
        // Validate input parameters
        require(!displayName.isNullOrEmpty()) { "Display name cannot be null or empty" }
        require(!peerId.isNullOrEmpty()) { "Peer ID cannot be null or empty" }
        requireNotNull(platform) { "Platform cannot be null" }

        // Start the telepresence session
        try {
            val success = robot?.startTelepresence(displayName, peerId, platform)
            Thread.sleep(4000)
            robot?.stopTelepresence()
            Thread.sleep(4000)
            publishMessage("temi/tele/finished", "done")
            if (success.isNullOrEmpty()) {
                Log.i("Telepresence", "Telepresence session started successfully")
                Thread.sleep(3000)
                robot?.stopTelepresence()
                publishMessage("temi/tele/finished", "done")
            } else {
                Log.e("Telepresence", "Failed to start telepresence session")
            }
        } catch (e: Exception) {
            // Handle exceptions appropriately
            e.printStackTrace()
            Log.e("Telepresence", "Error occurred while starting telepresence session: ${e.message}")
        }
    }

    /**
     * Make the robot to go to a position. It will bypass obstracles which are in his way
     * Format: {
     *   "x": 4,
     *   "y": 7,
     *   "yaw": 999, # 999 make the robot keep its rotation when arriving
     *   "tiltAngle": 30
     * }
     */
    fun goToPositionWithByPass(payload: String){
        val gson = Gson()
        val objectFromJson = gson.fromJson(payload, Position::class.java)
        Log.d("JSON", objectFromJson.toString())
        robot?.goToPosition(objectFromJson, backwards = false, noBypass = false)//yaw = 999 cancle rotation at arival
    }

    /**
     * Make the robot to go to a position. It will not bypass obstracles which are in his direct way/path and stop moving until the obstacle is gone
     * Format: {
     *   "x": 4,
     *   "y": 7,
     *   "yaw": 999, # 999 make the robot keep its rotation when arriving
     *   "tiltAngle": 30
     * }
     */
    fun goToPosition(payload: String){
        val gson = Gson()
        val objectFromJson = gson.fromJson(payload, Position::class.java)
        Log.d("JSON", objectFromJson.toString())
        robot?.goToPosition(objectFromJson, backwards = false, noBypass = true)//yaw = 999 cancle rotation at arival
    }

    /**
     * Robot will stop its movement
     */
    fun stopMovement(payload: String){
        robot?.stopMovement()
    }

    /**
     * Will make the robot move forward (this must be triggered all over again to make to robot keep moving)
     */
    fun skidJoyForward(payload: String){
        robot?.skidJoy(1f,0f, false)
        Log.d("skidjoy", "forward skidjoy")
    }

    /**
     * Will make the robot move depending on the payload (this must be triggered all over again to make to robot keep moving)
     * format:{
     *   "x": 1,
     *   "y": 1
     * }
     * Also see documentation of temi sdk
     */
    fun skidJoy(payload: String){
        val gson = Gson()
        val objectFromJson = gson.fromJson(payload, SkidJoyDir::class.java)
        Log.d("JSON skidjoy", objectFromJson.toString())
        robot?.skidJoy(objectFromJson.x,objectFromJson.y, false)
    }

    /**
     * Method changing the speed used when robot is going to a saved location or position and publishing the speed in robot setting after changing
     * format: type "slow", "medium" or "high"
     * NOTE: looks like this application must be set to kiosk mode and as displayed application in home screen for make this work
     */
    fun setGoToSpeed(payload: String){
        if (robot?.let { MainActivity.requestPermissionIfNeeded(it, Permission.SETTINGS, REQUEST_CODE_NORMAL) } == true) {
            return
        }
        Log.d("gotospeed", payload)
        if (payload.uppercase() == "SLOW"){
            Log.d("gotospeed", "set to slow")
            robot?.goToSpeed = SpeedLevel.SLOW
        } else if(payload.uppercase() == "MEDIUM"){
            Log.d("gotospeed", "set to medium")
            robot?.goToSpeed = SpeedLevel.MEDIUM
        }else if (payload.uppercase() == "HIGH"){
            Log.d("gotospeed", "set to high")
            robot?.goToSpeed = SpeedLevel.HIGH
        }
        publishMessage("temi/currentGoToSpeed", robot?.goToSpeed.toString())
    }

    /**
     * Changing the tilt angle of the robot tablet
     * format: number e.g. 22
     */
    fun tiltAngle(payload: String){
        try {
            val angle = payload.toInt()
            robot?.tiltAngle(angle, 0.5f)

        }catch (e:Exception){
            Log.e("MQTT", e.toString())
        }
    }

    /**
     * Turning the robot by a certain degree
     * format:
     * {
     *   "degrees": 1, # as int
     *   "speed": 1 # as float (range 0 - 1)
     * }
     */
    fun turnBy(payload: String) {
        val jsonObject = JSONObject(payload)
        val degrees = jsonObject.getInt("degrees")
        val speed = jsonObject.getDouble("speed")
        robot?.turnBy(degrees, speed.toFloat())
    }


}
