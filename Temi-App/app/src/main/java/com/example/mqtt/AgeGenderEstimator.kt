package com.example.mqtt

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.List
import java.util.Map

class AgeGenderEstimator(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var tfLiteModel: MappedByteBuffer? = null
    private var tfLiteOptions: Interpreter.Options? = null

    private val labels = ArrayList<String>()
    private var inputSize: Int = 0
    private var isModelQuantized: Boolean = false

    private lateinit var imgData: ByteBuffer
    private lateinit var intValues: IntArray

    private lateinit var ageMap: Array<FloatArray>
    private lateinit var genderMap: Array<FloatArray>

    private val AGE_SIZE = 4
    private val GENDER_SIZE = 2
    private val WHITE_THRESH = 255f

    init {
        try {
            val modelFilename = "face_model_v5.tflite"
            val labelFilename = "labelmap.txt"
            inputSize = 80 // TF_OD_API_INPUT_SIZE from DetectorActivity
            isModelQuantized = false // TF_OD_API_IS_QUANTIZED from DetectorActivity

            val am = context.assets
            val `is` = am.open(labelFilename)

            tfLiteModel = loadModelFile(context.assets, modelFilename)

            BufferedReader(InputStreamReader(`is`)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    labels.add(line!!)
                }
            }

            tfLiteOptions = Interpreter.Options()
            tfLiteOptions?.setNumThreads(4) // NUM_THREADS from TFLiteObjectDetectionAPIModel
            tfLiteOptions?.setUseXNNPACK(true)
            interpreter = Interpreter(tfLiteModel!!, tfLiteOptions)

            val numBytesPerChannel = if (isModelQuantized) 1 else 4
            imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * numBytesPerChannel)
            imgData.order(ByteOrder.nativeOrder())
            intValues = IntArray(inputSize * inputSize)

            ageMap = Array(1) { FloatArray(AGE_SIZE) }
            genderMap = Array(1) { FloatArray(GENDER_SIZE) }

        } catch (e: IOException) {
            Log.e("AgeGenderEstimator", "Error initializing interpreter", e)
        }
    }

    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun estimateAgeGender(bitmap: Bitmap): Pair<String, String> {
        Log.d("AGE_GENDER_ESTIMATOR", "estimateAgeGender called with bitmap: ${bitmap.width}x${bitmap.height}")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        Log.d("AGE_GENDER_ESTIMATOR", "Resized bitmap to: ${resizedBitmap.width}x${resizedBitmap.height}")

        // Get pixels from the resized bitmap
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        imgData.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue = intValues[i * inputSize + j]

                if (isModelQuantized) {
                    imgData.put(((pixelValue shr 16) and 0xFF).toByte())
                    imgData.put(((pixelValue shr 8) and 0xFF).toByte())
                    imgData.put((pixelValue and 0xFF).toByte())
                } else {
                    // BGR conversion and normalization
                    imgData.putFloat((pixelValue and 0xFF) / WHITE_THRESH)
                    imgData.putFloat(((pixelValue shr 8) and 0xFF) / WHITE_THRESH)
                    imgData.putFloat(((pixelValue shr 16) and 0xFF) / WHITE_THRESH)
                }
            }
        }

        val inputArray = arrayOf<Any>(imgData)
        val outputMap = HashMap<Int, Any>()
        outputMap[0] = ageMap
        outputMap[1] = genderMap

        Log.d("AGE_GENDER_ESTIMATOR", "Running interpreter...")
        try {
            interpreter?.runForMultipleInputsOutputs(inputArray, outputMap)
            Log.d("AGE_GENDER_ESTIMATOR", "Interpreter run successful")
        } catch (e: Exception) {
            Log.e("AGE_GENDER_ESTIMATOR", "Interpreter failed: ", e)
            return Pair("Unknown", "Unknown")
        }

        var ageInd = 0
        var maxAge = -1f
        for (i in ageMap[0].indices) {
            val currValue = ageMap[0][i]
            if (currValue > maxAge) {
                maxAge = currValue
                ageInd = i
            }
        }
        Log.d("AGE_GENDER_ESTIMATOR", "Age values: ${ageMap[0].contentToString()}, selected: $ageInd")

        var genderInd = 0
        var maxGender = -1f
        for (i in genderMap[0].indices) {
            val currValue = genderMap[0][i]
            if (currValue > maxGender) {
                maxGender = currValue
                genderInd = i
            }
        }
        Log.d("AGE_GENDER_ESTIMATOR", "Gender values: ${genderMap[0].contentToString()}, selected: $genderInd")

        val genderList = Arrays.asList("Female", "Male")
        val ageList = Arrays.asList("0-14yo", "15-40yo", "41-60yo", "61-100yo")

        val ageResult = ageList[ageInd]
        val genderResult = genderList[genderInd]

        Log.d("AGE_GENDER_ESTIMATOR", "Final result - Age: $ageResult, Gender: $genderResult")
        return Pair(ageResult, genderResult)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}