package com.example.interfacesnaturales.lenguajesenas.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.interfacesnaturales.R
import com.example.interfacesnaturales.lenguajesenas.GestureRecognizer
import com.example.interfacesnaturales.lenguajesenas.HandLandmarkerHelper
import com.example.interfacesnaturales.lenguajesenas.MainViewModel
import com.example.interfacesnaturales.databinding.FragmentCameraBinding
import com.google.android.material.slider.Slider
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment(), HandLandmarkerHelper.LandmarkerListener, TextToSpeech.OnInitListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var recognizedLetterTextView: TextView
    private lateinit var timeLabelTextView: TextView
    private lateinit var timeSlider: Slider
    private lateinit var progressBar: ProgressBar
    private lateinit var settingsButton: Button
    private lateinit var settingsPanel: LinearLayout
    private var recognizedText = ""
    private var lastRecognizedGesture: String? = null
    private lateinit var gestureRecognizer: GestureRecognizer
    private var gestureTimer: CountDownTimer? = null
    private var holdTimeInMillis: Long = 2000
    private lateinit var tts: TextToSpeech
    private var isRockOnPendingDelete = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recognizedLetterTextView = view.findViewById(R.id.recognized_letter_text_view)
        timeLabelTextView = view.findViewById(R.id.time_label_text_view)
        timeSlider = view.findViewById(R.id.time_slider)
        progressBar = view.findViewById(R.id.progress_bar)
        settingsButton = view.findViewById(R.id.settings_button)
        settingsPanel = view.findViewById(R.id.settings_panel)
        gestureRecognizer = GestureRecognizer()
        tts = TextToSpeech(requireContext(), this)

        settingsButton.setOnClickListener {
            if (settingsPanel.visibility == View.VISIBLE) {
                settingsPanel.visibility = View.GONE
                settingsButton.text = "^"
            } else {
                settingsPanel.visibility = View.VISIBLE
                settingsButton.text = "v"
            }
        }

        timeSlider.setLabelFormatter { value: Float ->
            "${String.format("%.1f", value / 1000.0)}s"
        }

        timeSlider.addOnChangeListener { _, value, _ ->
            holdTimeInMillis = value.toLong()
            timeLabelTextView.text = "Hold Time: ${String.format("%.1f", value / 1000.0)}s"
            progressBar.max = holdTimeInMillis.toInt()
        }

        backgroundExecutor = Executors.newSingleThreadExecutor()
        // Wait for the views to be properly laid out
        binding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the HandLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            handLandmarkerHelper = HandLandmarkerHelper(
                context = requireContext(),
                handLandmarkerHelperListener = this
            )
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Using a 4:3 aspect ratio
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match MediaPipe inputs
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    handLandmarkerHelper.detectLiveStream(
                        imageProxy = image,
                        isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e("CameraFragment", "Use case binding failed", exc)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            val handLandmarkerResult = resultBundle.results.first()
            val recognizedGesture = gestureRecognizer.recognize(handLandmarkerResult)

            if (recognizedGesture != lastRecognizedGesture) {
                lastRecognizedGesture = recognizedGesture
                gestureTimer?.cancel()
                progressBar.progress = 0

                if (recognizedGesture.isNotEmpty()) {
                    gestureTimer = object : CountDownTimer(holdTimeInMillis, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            progressBar.progress = (holdTimeInMillis - millisUntilFinished).toInt()
                        }

                        override fun onFinish() {
                            when (recognizedGesture) {
                                "ROCK_ON" -> {
                                    if (isRockOnPendingDelete) {
                                        recognizedText = ""
                                        recognizedLetterTextView.text = ""
                                        isRockOnPendingDelete = false
                                    } else {
                                        if (recognizedText.isNotEmpty()) {
                                            tts.speak(recognizedText, TextToSpeech.QUEUE_FLUSH, null, "")
                                        }
                                        isRockOnPendingDelete = true
                                    }
                                }
                                "SPACE" -> {
                                    isRockOnPendingDelete = false
                                    recognizedText += " "
                                }
                                "DELETE_LAST" -> {
                                    isRockOnPendingDelete = false
                                    if (recognizedText.isNotEmpty()) {
                                        recognizedText = recognizedText.dropLast(1)
                                    }
                                }
                                else -> {
                                    isRockOnPendingDelete = false
                                    recognizedText += recognizedGesture
                                    tts.speak(recognizedGesture, TextToSpeech.QUEUE_FLUSH, null, "")
                                }
                            }
                            recognizedLetterTextView.text = recognizedText
                            progressBar.progress = 0
                        }
                    }.start()
                }
            }

            val letterToDraw = when (recognizedGesture) {
                "SPACE" -> "‿"
                "DELETE_LAST" -> "←"
                "ROCK_ON" -> "⌦"
                else -> recognizedGesture
            }
            val handRotation = if (handLandmarkerResult.landmarks().isNotEmpty()) gestureRecognizer.getHandRotation(handLandmarkerResult.landmarks().first()) else 0f

            binding.overlay.setResults(
                handLandmarkerResult,
                letterToDraw,
                handRotation,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                //RunningMode.LIVE_STREAM
            )
            binding.overlay.invalidate()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        _binding = null
        backgroundExecutor.shutdown()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("CameraFragment", "The Language specified is not supported!")
            }
        } else {
            Log.e("CameraFragment", "TTS Initialization Failed!")
        }
    }
}