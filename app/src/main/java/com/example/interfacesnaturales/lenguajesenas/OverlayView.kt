package com.example.interfacesnaturales.lenguajesenas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.interfacesnaturales.R
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: HandLandmarkerResult? = null
    private var recognizedLetter: String = ""
    private var handRotation: Float = 0f
    private var linePaint = Paint()
    private var pointPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        results = null
        recognizedLetter = ""
        handRotation = 0f
        linePaint.reset()
        pointPaint.reset()
        textPaint.reset()
        invalidate()
        initPaints()
    }
    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

        textPaint.color = Color.RED
        textPaint.textSize = 400f
        textPaint.style = Paint.Style.FILL
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { handLandmarkerResult ->
            for (landmark in handLandmarkerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                HandLandmarker.HAND_CONNECTIONS.forEach { connection ->
                    canvas.drawLine(
                        landmark[connection.start()].x() * imageWidth * scaleFactor,
                        landmark[connection.start()].y() * imageHeight * scaleFactor,
                        landmark[connection.end()].x() * imageWidth * scaleFactor,
                        landmark[connection.end()].y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
                if (recognizedLetter.isNotEmpty()) {
                    val palmCenter = landmark[9]
                    val x = palmCenter.x() * imageWidth * scaleFactor
                    val y = palmCenter.y() * imageHeight * scaleFactor

                    canvas.save()
                    canvas.rotate(handRotation, x, y)
                    canvas.drawText(
                        recognizedLetter,
                        x,
                        y + 140f, // Vertically center the text
                        textPaint
                    )
                    canvas.restore()
                }
            }
        }
    }

    fun setResults(
        handLandmarkerResults: HandLandmarkerResult,
        recognizedLetter: String,
        handRotation: Float,
        imageHeight: Int,
        imageWidth: Int
    ) {
        results = handLandmarkerResults
        this.recognizedLetter = recognizedLetter
        this.handRotation = handRotation

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)

        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}
