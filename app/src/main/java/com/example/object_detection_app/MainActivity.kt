package com.example.object_detection_app

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
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
            .setAbsoluteFilePath("object_detection.tflite")
            .build()
        val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f) //Similarity
            .setMaxPerObjectLabelCount(3)
            .build()
        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)
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

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),{imageProxy ->

            val rotationDegreesValue = imageProxy.imageInfo.rotationDegrees

            val image = imageProxy.image

            if(image != null){

                val processImage = InputImage.fromMediaImage(image, rotationDegreesValue)

                objectDetector
                    .process(processImage)
                    .addOnSuccessListener { objects ->
                        for (i in objects) {

                            if(binding.parentLayout.childCount > 1) binding.parentLayout.removeViewAt(1)

                            val element = Draw(context = this,
                                rect = i.boundingBox,
                                text = i.labels.firstOrNull()?.text ?: "Undefined")

                            binding.parentLayout.addView(element)
                        }

                        Log.d("MainActivity", "Cos robi")
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