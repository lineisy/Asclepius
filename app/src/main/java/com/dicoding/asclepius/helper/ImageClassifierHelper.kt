package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import com.dicoding.asclepius.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier


class ImageClassifierHelper(
    val context: Context,
    private val classifierListener: ClassifierListener?,
    private val modelName: String = "cancer_classification.tflite",
    private var threshold: Float = 0.1f,
    private var maxResults: Int = 3
) {
    private var imageClassifier : ImageClassifier ?= null


    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(4)

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        }catch (e:IllegalStateException){
            classifierListener?.onError(
                context.getString(
                    R.string.image_classifier_failed
                )
            )
            Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    fun classifyStaticImage(imageUri: Uri) {
        if(imageClassifier == null){
            setupImageClassifier()
        }

        val imgProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224,224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(DataType.FLOAT32))
            .build()

        val bitmap = uriToBitMap(imageUri, context)
        val tensorImg = imgProcessor.process(TensorImage.fromBitmap(bitmap))
        val imgProcessOption = ImageProcessingOptions.builder().build()

        val inferenceTime = SystemClock.uptimeMillis()
        val results = imageClassifier?.classify(tensorImg, imgProcessOption)
        classifierListener?.onResults(
            results, inferenceTime
        )
    }

    @Suppress("DEPRECATION")
    private fun uriToBitMap(uri: Uri, context: Context) : Bitmap{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }else{
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }.copy(Bitmap.Config.ARGB_8888, true)
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }
}