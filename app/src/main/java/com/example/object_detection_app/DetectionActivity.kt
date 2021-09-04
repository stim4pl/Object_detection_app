package com.example.object_detection_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.ResolutionInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.example.object_detection_app.databinding.ActivityDetectionBinding
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class DetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetectionBinding
    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var task: Task<List<DetectedObject>>
    private var detectedObjects: ArrayList<View> = arrayListOf<View>()
    private var detect: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detection)

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
                //.enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.1F) //Similarity
                .setMaxPerObjectLabelCount(1)
                .build()
        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)
        val clearButton = findViewById<Button>(R.id.ClearButton)
        clearButton.setOnClickListener {
            clearObjects()
        }

        val startButton = findViewById<Button>(R.id.startstopButton)
        startButton.setOnClickListener {
            when (startButton.text) {
                "Start" -> {
                    detect = 1
                    startButton.text = "Stop"
                }
                "Stop" -> {
                    detect = 0
                    startButton.text = "Start"
                }
            }
        }

    }

    private fun clearObjects() {
        if (detectedObjects.isNotEmpty()) {
            for (item in detectedObjects) {
                binding.parentLayout.removeView(item)
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {


        val size = Size(1280, 720)

        val preview = Preview.Builder()
                .setTargetResolution(size)
                .build()
        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val audio = Audio(this, "en", "GB")


        val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(size)
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
                            clearObjects()
                            for (i in objects) {
                                val element = Draw(context = this,
                                        rect = i.boundingBox,
                                        text = i.labels.firstOrNull()?.text ?: "Undefined")
                                detectedObjects.add(element)

                                if (detect == 1) {
                                    binding.parentLayout.addView(element)
                                    audio.speek(element.text)
                                }
                            }

                            imageProxy.close()

                        }.addOnFailureListener {
                            Log.e("MainActivity", "Error - ${it.message}")
                            imageProxy.close()
                        }

            }
        })

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
    }

}