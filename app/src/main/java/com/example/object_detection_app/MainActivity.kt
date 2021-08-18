package com.example.object_detection_app

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.size
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.example.object_detection_app.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var detectedObjects: ArrayList<View> = arrayListOf<View>()

    private var detect: Int = 0
    private var amountDetected: Int = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider = cameraProvider)


        }, ContextCompat.getMainExecutor(this))

        val localModel = LocalModel.Builder()
                .setAssetFilePath("mnasnet_1.3_224_1_metadata_1.tflite")
                .build()
        val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f) //Similarity
                .setMaxPerObjectLabelCount(3)
                .build()
        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

        val clearButton = findViewById<Button>(R.id.ClearButton)
        clearButton.setOnClickListener {
            for (k in 4 downTo 0 step 1) {
                binding.parentLayout.removeViewAt(binding.parentLayout.findViewWithTag("child$k"))
                amountDetected.dec()
            }
        }

        val startButton = findViewById<Button>(R.id.startstopButton)
        startButton.setOnClickListener {
            if (startButton.text == "Start") {
                detect = 1
                startButton.text = "Stop"
            } else if (startButton.text == "Stop") {
                detect = 0
                startButton.text = "Stop"
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {


        val preview = Preview.Builder().build()

        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)


        val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->

            val rotationDegreesValue = imageProxy.imageInfo.rotationDegrees

            val image = imageProxy.image

            if (image != null) {

                val processImage = InputImage.fromMediaImage(image, rotationDegreesValue)

                objectDetector
                        .process(processImage)
                        .addOnSuccessListener { objects ->
                            for (i in objects) {

                                if (amountDetected > 3) {
                                    for (k in 1..4) {
                                        binding.parentLayout.removeView(detectedObjects[0])
                                        detectedObjects.removeAt(0)
                                        amountDetected -= 1
                                        Log.d("MainActivity", "Dec")
                                    }

                                }

                                val element = Draw(context = this,
                                        rect = i.boundingBox,
                                        text = i.labels.firstOrNull()?.text ?: "Undefined")
                                amountDetected += 1
                                detectedObjects.add(element)
                                binding.parentLayout.addView(element)
                            }

                            imageProxy.close()
                        }.addOnFailureListener {
                            Log.v("MainActivity", "Error - ${it.message}")
                            imageProxy.close()
                        }
            }
        })

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
    }

}