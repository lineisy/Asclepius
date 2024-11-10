package com.dicoding.asclepius.view.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showImage()
        showResult()
    }

    private fun showImage() {
        (Uri.parse(intent.getStringExtra(EXTRA_IMG_URI))?.let {
            Log.d("Image URI", "showImage: $it")
            binding.resultImage.setImageURI(it)
        }) ?: run {
            "Image is missing!!".showError()
        }
    }

    private fun showResult() {
        val category = intent.getStringExtra(EXTRA_RESULT)
        val score = intent.getFloatExtra(EXTRA_SCORE, 0f)

        val persentase = (score * 100).toInt()

        if (score != null) {
            binding.resultText.text =
                "Hasilnya adalah $category dengan confidence score $persentase%"
        } else {
            binding.resultText.text = getString(R.string.result_not_available)
        }
    }

    private fun String.showError() {
        Toast.makeText(this@ResultActivity, this, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_IMG_URI = "extra_img_uri"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_RESULT = "extra_result"
    }
}