package com.example.object_detection_app

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.size
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.example.object_detection_app.databinding.ActivityMainBinding
import com.google.a.b.a.a.a.e
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class MainActivity : AppCompatActivity() {


    val CAMERA_RQ = 102


    private lateinit var binding: ActivityMainBinding

    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var task: Task<List<DetectedObject>>

    private var detectedObjects: ArrayList<View> = arrayListOf<View>()

    private var detect: Int = 0
    private var amountDetected: Int = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkForPermission(android.Manifest.permission.CAMERA, "camera", CAMERA_RQ)

        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider = cameraProvider)


        }, ContextCompat.getMainExecutor(this))

        val localModel = LocalModel.Builder()
                .setAssetFilePath("mnasnet_0.50_224_1_metadata_1.tflite")
                .build()
        val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5F) //Similarity
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

    private fun checkForPermission(permission: String, name: String, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(applicationContext, "$name permission granted", Toast.LENGTH_SHORT).show()
                }
                shouldShowRequestPermissionRationale(permission) -> showDialog(permission, name, requestCode)
                else -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        fun innerCheck(name: String) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "$name permission refused", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "$name permission granted", Toast.LENGTH_SHORT).show()
            }
        }
        when (requestCode) {
            CAMERA_RQ -> innerCheck("camera")
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showDialog(permission: String, name: String, requestCode: Int) {
        val builder = AlertDialog.Builder(this)

        builder.apply {
            setMessage("Permission to access your $name is required to use this app")
            setTitle("Permission required")
            setPositiveButton("OK") { dialog, which ->
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun clearObjects() {
        if (detectedObjects.isNotEmpty()) {
            for (item in detectedObjects) {
                binding.parentLayout.removeView(item)
                amountDetected -= 1
                Log.d("MainActivity", "Dec")
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
                            clearObjects()
                            for (i in objects) {
                                val element = Draw(context = this,
                                        rect = i.boundingBox,
                                        text = i.labels.firstOrNull()?.text ?: "Undefined")
                                amountDetected += 1
                                detectedObjects.add(element)

                                if (detect == 1) binding.parentLayout.addView(element)
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