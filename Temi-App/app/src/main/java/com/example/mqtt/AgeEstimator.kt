package com.example.mqtt

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AgeEstimator(private val context: Context) {

    private var interpreter: Interpreter?

    init {
        val model = FileUtil.loadMappedFile(context, "model_age_q.tflite")
        interpreter = Interpreter(model)
    }

    fun estimateAge(bitmap: Bitmap): Int {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(200, 200, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        // Create a TensorImage from the bitmap with the correct data type (FLOAT32 for 4 bytes per channel)
        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Allocate output buffer with the correct size for a single float (4 bytes)
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1), DataType.FLOAT32)

        interpreter?.run(tensorImage.buffer, outputBuffer.buffer)

        val age = outputBuffer.getFloatValue(0) * 116 // Multiply by 116 as per the model's documentation
        return age.toInt()
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

