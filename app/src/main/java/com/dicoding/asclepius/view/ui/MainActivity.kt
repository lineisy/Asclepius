package com.dicoding.asclepius.view.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File

class MainActivity : AppCompatActivity(), ImageClassifierHelper.ClassifierListener {
    private lateinit var binding: ActivityMainBinding
    private var currentImageUri: Uri? = null
    private lateinit var imageClassifierHelper: ImageClassifierHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        imageClassifierHelper = ImageClassifierHelper(this, this)

        savedInstanceState?.getString(CURRENT_IMG_URI)?.let {
            currentImageUri = Uri.parse(it)
            showImage()
        }

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.analyzeButton.setOnClickListener { analyzeImage() }
    }


    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            handleImg(it)
        } ?: showToast("No media selected")
    }

    private fun handleImg(uri: Uri) {
        starCropImg(uri)
        currentImageUri = uri
        showImage()
    }

    private fun starCropImg(uri: Uri) {
        val option = getUCropOptions()
        val uCrop = UCrop.of(uri, getImgUriCropped())
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .withOptions(option)

        uCrop.start(this@MainActivity)
    }

    private fun getUCropOptions() = UCrop.Options().apply {
        setCompressionQuality(90)
        setToolbarColor(ContextCompat.getColor(this@MainActivity, R.color.primary_red))
        setActiveControlsWidgetColor(ContextCompat.getColor(this@MainActivity, R.color.black))
        setStatusBarColor(ContextCompat.getColor(this@MainActivity, R.color.primary_red))
        setToolbarWidgetColor(Color.WHITE)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                currentImageUri = it
                showImage()
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.let { onError(it.message.toString()) }
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $currentImageUri")
            binding.previewImageView.setImageURI(currentImageUri)
        }

    }

    private fun getImgUriCropped() = Uri.fromFile(File(cacheDir, "cropped_img"))

    private fun analyzeImage() {
        currentImageUri?.let {
            binding.progressIndicator.visibility = View.VISIBLE
            SHOULD_IMG_RESET = true
            imageClassifierHelper.classifyStaticImage(it)
        } ?: Toast.makeText(this, getString(R.string.empty_img), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (SHOULD_IMG_RESET) {
            currentImageUri = null
            binding.previewImageView.setImageResource(R.drawable.ic_place_holder)
            SHOULD_IMG_RESET = false
        }
    }

    private fun moveToResult(result: String, confidence: Float) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_RESULT, result)
        intent.putExtra(ResultActivity.EXTRA_SCORE, confidence)
        intent.putExtra(ResultActivity.EXTRA_IMG_URI, currentImageUri.toString())
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: String) {
        binding.progressIndicator.visibility = View.GONE
        showToast(error)
    }

    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        runOnUiThread {
            binding.progressIndicator.visibility = View.GONE
            try {
                results?.let {
                    val category = it[0].categories[0].label
                    val confidence = it[0].categories[0].score
                    moveToResult(category, confidence)
                }
            } catch (e: Exception) {
                onError(e.message.toString())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentImageUri?.let {
            outState.putString(CURRENT_IMG_URI, it.toString())
        }
    }

    companion object {
        private const val CURRENT_IMG_URI = "currentImageUri"
        private var SHOULD_IMG_RESET = false
    }
}
