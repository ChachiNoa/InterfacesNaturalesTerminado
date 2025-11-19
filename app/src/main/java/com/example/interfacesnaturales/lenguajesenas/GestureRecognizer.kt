package com.example.interfacesnaturales.lenguajesenas

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class GestureRecognizer {

    fun recognize(handLandmarkerResult: HandLandmarkerResult): String {
        if (handLandmarkerResult.landmarks().isEmpty()) {
            return ""
        }

        // Check for special gestures first
        if (isRockOn(handLandmarkerResult)) {
            return "ROCK_ON"
        }
        if (isDeleteLast(handLandmarkerResult)) {
            return "DELETE_LAST"
        }
        if (isSpace(handLandmarkerResult)) {
            return "SPACE"
        }

        // Check for letters in a specific order to avoid conflicts
        if (isLetterA(handLandmarkerResult)) {
            return "A"
        }

        if (isLetterB(handLandmarkerResult)) {
            return "B"
        }

        if (isLetterE(handLandmarkerResult)) {
            return "E"
        }

        if (isLetterI(handLandmarkerResult)) {
            return "I"
        }

        if (isLetterL(handLandmarkerResult)) {
            return "L"
        }

        if (isLetterH(handLandmarkerResult)) {
            return "H"
        }

        if (isLetterO(handLandmarkerResult)) {
            return "O"
        }

        if (isLetterR(handLandmarkerResult)) {
            return "R"
        }

        if (isLetterN(handLandmarkerResult)) {
            return "N"
        }

        if (isLetterD(handLandmarkerResult)) {
            return "D"
        }

        if (isLetterS(handLandmarkerResult)) {
            return "S"
        }

        if (isLetterT(handLandmarkerResult)) {
            return "T"
        }

        return ""
    }

    fun getHandRotation(landmarks: List<NormalizedLandmark>): Float {
        if (landmarks.size < 10) return 0f

        val wrist = landmarks[0]
        val middleFingerMcp = landmarks[9]

        val dX = middleFingerMcp.x() - wrist.x()
        val dY = middleFingerMcp.y() - wrist.y()

        val angleInRadians = atan2(dY, dX)
        var angleInDegrees = Math.toDegrees(angleInRadians.toDouble()).toFloat()

        angleInDegrees += 90f

        return angleInDegrees
    }
    private fun isRockOn(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Index and Pinky fingers are straight.
        val indexStraight = landmarks[8].y() < landmarks[6].y()
        val pinkyStraight = landmarks[20].y() < landmarks[18].y()
        if (!indexStraight || !pinkyStraight) return false

        // 2. Middle and Ring fingers are curled.
        val middleCurled = landmarks[12].y() > landmarks[10].y()
        val ringCurled = landmarks[16].y() > landmarks[14].y()

        return middleCurled && ringCurled
    }

    private fun isDeleteLast(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Index, Middle and Ring fingers are straight.
        val indexStraight = landmarks[8].y() < landmarks[6].y()
        val middleStraight = landmarks[12].y() < landmarks[10].y()
        val ringStraight = landmarks[16].y() < landmarks[14].y()
        if (!indexStraight || !middleStraight || !ringStraight) return false

        // 2. Pinky finger is curled.
        val pinkyCurled = landmarks[20].y() > landmarks[18].y()

        return pinkyCurled
    }

    private fun isSpace(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Thumb and Middle finger tips are close
        val thumbTip = landmarks[4]
        val middleTip = landmarks[12]
        val distance = sqrt(Math.pow((thumbTip.x() - middleTip.x()).toDouble(), 2.0) + Math.pow((thumbTip.y() - middleTip.y()).toDouble(), 2.0))
        val tipsAreClose = distance < 0.08

        // 2. Index, Ring, and Pinky fingers are extended
        val indexStraight = landmarks[8].y() < landmarks[7].y()
        val ringStraight = landmarks[16].y() < landmarks[15].y()
        val pinkyStraight = landmarks[20].y() < landmarks[19].y()

        return tipsAreClose && indexStraight && ringStraight && pinkyStraight
    }

    private fun isLetterA(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. All four fingers are curled.
        val indexFingerCurled = landmarks[8].y() > landmarks[6].y()
        val middleFingerCurled = landmarks[12].y() > landmarks[10].y()
        val ringFingerCurled = landmarks[16].y() > landmarks[14].y()
        val pinkyFingerCurled = landmarks[20].y() > landmarks[18].y()
        if (! (indexFingerCurled && middleFingerCurled && ringFingerCurled && pinkyFingerCurled)) return false

        // 2. Thumb is on the side of the index finger.
        val thumbTip = landmarks[4]
        val indexMcp = landmarks[5]
        val thumbIsOnSide = abs(thumbTip.y() - indexMcp.y()) < 0.05

        // 3. Thumb is not tucked under for E
        val thumbIsNotTucked = landmarks[4].y() < landmarks[6].y() && landmarks[4].y() < landmarks[10].y()

        return thumbIsOnSide && thumbIsNotTucked
    }

    private fun isLetterB(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()
        val handedness = handLandmarkerResult.handednesses().first().first().displayName()

        val indexStraight = landmarks[8].y() < landmarks[7].y()
        val middleStraight = landmarks[12].y() < landmarks[11].y()
        val ringStraight = landmarks[16].y() < landmarks[15].y()
        val pinkyStraight = landmarks[20].y() < landmarks[19].y()
        val allFingersStraight = indexStraight && middleStraight && ringStraight && pinkyStraight

        val thumbBent = if (handedness == "Right") {
            landmarks[4].x() < landmarks[3].x() && landmarks[4].x() < landmarks[2].x()
        } else { // Left hand
            landmarks[4].x() > landmarks[3].x() && landmarks[4].x() > landmarks[2].x()
        }

        return allFingersStraight && thumbBent
    }

    private fun isLetterE(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()
        val handedness = handLandmarkerResult.handednesses().first().first().displayName()

        // 1. All four main fingers are curled, with tips pointing down.
        val indexCurled = landmarks[8].y() > landmarks[7].y()
        val middleCurled = landmarks[12].y() > landmarks[11].y()
        val ringCurled = landmarks[16].y() > landmarks[15].y()
        val pinkyCurled = landmarks[20].y() > landmarks[19].y()
        if (!(indexCurled && middleCurled && ringCurled && pinkyCurled)) return false

        // 2. The thumb is tucked in and under the index and middle fingers.
        val thumbTuckedIn = if (handedness == "Right") {
            landmarks[4].x() < landmarks[5].x()
        } else { // Left
            landmarks[4].x() > landmarks[5].x()
        }
        val thumbUnderFingers = landmarks[4].y() > landmarks[6].y() && landmarks[4].y() > landmarks[10].y()

        // 3. Ensure it is not an 'A' fist.
        if (isLetterA(handLandmarkerResult)) return false

        return thumbTuckedIn && thumbUnderFingers
    }

    private fun isLetterI(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Pinky is straight
        val pinkyStraight = landmarks[20].y() < landmarks[18].y()

        // 2. Other three fingers are curled
        val indexCurled = landmarks[8].y() > landmarks[6].y()
        val middleCurled = landmarks[12].y() > landmarks[10].y()
        val ringCurled = landmarks[16].y() > landmarks[14].y()
        if (!pinkyStraight || !indexCurled || !middleCurled || !ringCurled) return false

        // 3. Thumb is tucked over and close to the other fingers
        val thumbTip = landmarks[4]
        val indexPip = landmarks[6]
        val thumbTucked = thumbTip.y() > indexPip.y() // Thumb tip below index PIP
        val thumbIsClose = abs(thumbTip.x() - indexPip.x()) < 0.08 // Thumb horizontally close

        return thumbTucked && thumbIsClose
    }

    private fun isLetterL(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()
        val handedness = handLandmarkerResult.handednesses().first().first().displayName()

        // 1. Index finger must be mostly vertical and pointing up.
        val indexTip = landmarks[8]
        val indexMcp = landmarks[5]
        val indexIsUp = indexTip.y() < indexMcp.y()
        val indexVerticality = abs(indexTip.y() - indexMcp.y())
        val indexHorizontality = abs(indexTip.x() - indexMcp.x())
        val indexIsVertical = indexHorizontality < indexVerticality * 0.5 // Finger is more vertical than horizontal
        if (!indexIsUp || !indexIsVertical) return false

        // 2. Thumb must be mostly horizontal and pointing outwards.
        val thumbTip = landmarks[4]
        val thumbIp = landmarks[3]
        val thumbVerticality = abs(thumbTip.y() - thumbIp.y())
        val thumbHorizontality = abs(thumbTip.x() - thumbIp.x())
        val thumbIsHorizontal = thumbVerticality < thumbHorizontality * 0.5 // Thumb is more horizontal than vertical
        val thumbPointsOut = if (handedness == "Right") {
            thumbTip.x() > thumbIp.x()
        } else { // Left hand (camera mirrors this)
            thumbTip.x() < thumbIp.x()
        }
        if (!thumbIsHorizontal || !thumbPointsOut) return false

        // 3. Middle, Ring, and Pinky fingers must be curled.
        val middleCurled = landmarks[12].y() > landmarks[10].y()
        val ringCurled = landmarks[16].y() > landmarks[14].y()
        val pinkyCurled = landmarks[20].y() > landmarks[18].y()

        return middleCurled && ringCurled && pinkyCurled
    }

    private fun isLetterH(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Index and Middle fingers are straight.
        val indexStraight = landmarks[8].y() < landmarks[6].y()
        val middleStraight = landmarks[12].y() < landmarks[10].y()

        // 2. Ring and Pinky fingers are curled.
        val ringCurled = landmarks[16].y() > landmarks[14].y()
        val pinkyCurled = landmarks[20].y() > landmarks[18].y()
        if (!(indexStraight && middleStraight && ringCurled && pinkyCurled)) return false

        // 3. Hand must have some horizontal orientation.
        val indexTip = landmarks[8]
        val indexMcp = landmarks[5]
        val isSomewhatHorizontal = abs(indexTip.x() - indexMcp.x()) > abs(indexTip.y() - indexMcp.y())
        if(!isSomewhatHorizontal) return false

        // 4. Index and Middle fingers are close together.
        val middleTip = landmarks[12]
        val distance = sqrt(Math.pow((indexTip.x() - middleTip.x()).toDouble(), 2.0) + Math.pow((indexTip.y() - middleTip.y()).toDouble(), 2.0))
        val fingersTogether = distance < 0.1

        return fingersTogether
    }

    private fun isLetterO(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Thumb and index finger tips are close, forming a circle.
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val tipsDistance = sqrt(Math.pow((thumbTip.x() - indexTip.x()).toDouble(), 2.0) + Math.pow((thumbTip.y() - indexTip.y()).toDouble(), 2.0))
        if (tipsDistance > 0.08) return false

        // 2. To ensure a 'hole' is visible, check that the index finger is widely curved.
        val indexMcp = landmarks[5]
        val indexPip = landmarks[6]
        val curveWidth = abs(indexMcp.x() - indexPip.x())
        val hasHole = curveWidth > 0.04 // This value enforces a wide curve.

        // 3. All other fingers must also be curved.
        val middleCurved = landmarks[12].y() > landmarks[11].y()
        val ringCurved = landmarks[16].y() > landmarks[15].y()
        val pinkyCurved = landmarks[20].y() > landmarks[19].y()
        if (!(middleCurved && ringCurved && pinkyCurved)) return false

        // 4. Exclude 'A' gesture explicitly.
        if (isLetterA(handLandmarkerResult)) return false

        return hasHole
    }

    private fun isLetterR(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Index and Middle fingers are straight.
        val indexStraight = landmarks[8].y() < landmarks[6].y()
        val middleStraight = landmarks[12].y() < landmarks[10].y()

        // 2. Ring and Pinky fingers are curled.
        val ringCurled = landmarks[16].y() > landmarks[14].y()
        val pinkyCurled = landmarks[20].y() > landmarks[18].y()
        if (!(indexStraight && middleStraight && ringCurled && pinkyCurled)) return false

        // 3. Index and Middle fingers must be separated.
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val distance = sqrt(Math.pow((indexTip.x() - middleTip.x()).toDouble(), 2.0) + Math.pow((indexTip.y() - middleTip.y()).toDouble(), 2.0))
        val fingersSeparated = distance > 0.1

        return fingersSeparated
    }

    private fun isLetterN(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Four main fingers are curled into a fist.
        val indexCurled = landmarks[8].y() > landmarks[6].y()
        val middleCurled = landmarks[12].y() > landmarks[10].y()
        val ringCurled = landmarks[16].y() > landmarks[14].y()
        val pinkyCurled = landmarks[20].y() > landmarks[18].y()
        if (!(indexCurled && middleCurled && ringCurled && pinkyCurled)) return false

        // 2. Thumb is straight and very horizontal.
        val thumbTip = landmarks[4]
        val thumbBase = landmarks[1]
        val isHorizontal = abs(thumbTip.x() - thumbBase.x()) > abs(thumbTip.y() - thumbBase.y()) * 2.0 // Stricter horizontal check
        val isStraight = abs(thumbTip.x() - landmarks[2].x()) > abs(thumbTip.x() - landmarks[3].x())

        // 3. Exclude other fist gestures.
        if (isLetterA(handLandmarkerResult) || isLetterE(handLandmarkerResult)) return false

        return isHorizontal && isStraight
    }

    private fun isLetterD(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Index finger is straight and vertical.
        val indexTip = landmarks[8]
        val indexMcp = landmarks[5]
        val indexIsVertical = abs(indexTip.y() - indexMcp.y()) > abs(indexTip.x() - indexMcp.x()) * 2.0
        val indexIsUp = indexTip.y() < indexMcp.y()
        if (!indexIsVertical || !indexIsUp) return false

        // 2. A circle is formed by the other fingers and the thumb (similar to 'O').
        val thumbTip = landmarks[4]
        val middleTip = landmarks[12]
        val tipsDistance = sqrt(Math.pow((thumbTip.x() - middleTip.x()).toDouble(), 2.0) + Math.pow((thumbTip.y() - middleTip.y()).toDouble(), 2.0))
        if (tipsDistance > 0.1) return false

        // 3. Middle, Ring, and Pinky fingers are curled.
        val middleCurled = landmarks[12].y() > landmarks[11].y()
        val ringCurled = landmarks[16].y() > landmarks[15].y()
        val pinkyCurled = landmarks[20].y() > landmarks[19].y()
        if (!middleCurled || !ringCurled || !pinkyCurled) return false

        // 4. Ensure it's not an 'O'.
        if(isLetterO(handLandmarkerResult)) return false

        return true
    }

    private fun isLetterS(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()

        // 1. Middle, Ring, and Pinky fingers are straight.
        val middleStraight = landmarks[12].y() < landmarks[11].y()
        val ringStraight = landmarks[16].y() < landmarks[15].y()
        val pinkyStraight = landmarks[20].y() < landmarks[19].y()
        val threeFingersStraight = middleStraight && ringStraight && pinkyStraight

        // 2. Index finger and thumb tips form a circle (are very close).
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val distance = sqrt(Math.pow((thumbTip.x() - indexTip.x()).toDouble(), 2.0) + Math.pow((thumbTip.y() - indexTip.y()).toDouble(), 2.0))
        val circleFormed = distance < 0.08

        // 3. Index finger should be curled, not straight.
        val indexCurled = landmarks[8].y() > landmarks[7].y()

        return threeFingersStraight && circleFormed && indexCurled
    }

    private fun isLetterT(handLandmarkerResult: HandLandmarkerResult): Boolean {
        val landmarks = handLandmarkerResult.landmarks().first()
        val handedness = handLandmarkerResult.handednesses().first().first().displayName()

        // 1. Index, Middle, and Ring fingers must be curled.
        val indexCurled = landmarks[8].y() > landmarks[6].y()
        val middleCurled = landmarks[12].y() > landmarks[10].y()
        val ringCurled = landmarks[16].y() > landmarks[14].y()
        if (!(indexCurled && middleCurled && ringCurled)) return false

        // 2. Thumb is extended horizontally.
        val thumbTip = landmarks[4]
        val thumbBase = landmarks[1]
        val thumbIsHorizontal = abs(thumbTip.x() - thumbBase.x()) > abs(thumbTip.y() - thumbBase.y()) * 0.7
        val thumbIsStraight = if (handedness == "Right") {
            thumbTip.x() < thumbBase.x()
        } else { // Left hand
            thumbTip.x() > thumbBase.x()
        }

        // 3. Pinky is extended horizontally.
        val pinkyTip = landmarks[20]
        val pinkyBase = landmarks[17]
        val pinkyIsHorizontal = abs(pinkyTip.x() - pinkyBase.x()) > abs(pinkyTip.y() - pinkyBase.y()) * 0.7
        val pinkyIsStraight = if (handedness == "Right") {
            pinkyTip.x() > pinkyBase.x()
        } else { // Left hand
            pinkyTip.x() < pinkyBase.x()
        }

        return thumbIsHorizontal && thumbIsStraight && pinkyIsHorizontal && pinkyIsStraight
    }
}