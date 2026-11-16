package com.pro.aifitnesstracker

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.atan2

class SquatAnalyzer(
    private val onPoseDetected: (Pose, Int, Boolean, String) -> Unit
) : ImageAnalysis.Analyzer {

    var onPoseWithDimensions: ((Pose, Int, Boolean, String, Int, Int) -> Unit)? = null

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val poseDetector = PoseDetection.getClient(options)
    
    private var repCount = 0
    private var isBending = false
    private var isPerfectForm = false
    private var currentFeedback = "Stand in frame"

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val frameWidth = if (isRotated) imageProxy.height else imageProxy.width
            val frameHeight = if (isRotated) imageProxy.width else imageProxy.height

            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    processPose(pose)
                    onPoseDetected(pose, repCount, isPerfectForm, currentFeedback)
                    onPoseWithDimensions?.invoke(pose, repCount, isPerfectForm, currentFeedback, frameWidth, frameHeight)
                }
                .addOnFailureListener {}
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processPose(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        var shoulder = leftShoulder
        var hip = leftHip
        var knee = leftKnee
        var ankle = leftAnkle

        if (rightHip != null && rightKnee != null && rightAnkle != null && leftHip != null && leftKnee != null && leftAnkle != null) {
            val rightVisibility = rightHip.inFrameLikelihood + rightKnee.inFrameLikelihood + rightAnkle.inFrameLikelihood
            val leftVisibility = leftHip.inFrameLikelihood + leftKnee.inFrameLikelihood + leftAnkle.inFrameLikelihood
            if (rightVisibility > leftVisibility) {
                shoulder = rightShoulder
                hip = rightHip
                knee = rightKnee
                ankle = rightAnkle
            }
        }

        if (shoulder != null && hip != null && knee != null && ankle != null && 
            shoulder.inFrameLikelihood > 0.4f && hip.inFrameLikelihood > 0.4f && 
            knee.inFrameLikelihood > 0.4f && ankle.inFrameLikelihood > 0.4f) {
            
            // Check orientation: user should be sideways or facing camera
            if (shoulder.position.y < hip.position.y && knee.position.y < ankle.position.y) {
                
                val kneeAngle = calculateAngle(hip, knee, ankle)
                val hipAngle = calculateAngle(shoulder, hip, knee)
                
                // Advanced form tracking
                var formIsGood = true
                var feedback = "Good posture, squat down"

                if (kneeAngle < 115) { 
                    isBending = true
                    
                    if (hipAngle < 60) {
                        formIsGood = false
                        feedback = "Keep your back straight!"
                    } else if (kneeAngle > 90) {
                        formIsGood = true
                        feedback = "Go deeper!"
                    } else {
                        formIsGood = true
                        feedback = "Perfect depth! Stand up."
                    }
                } else if (kneeAngle > 150) { 
                    if (isBending) {
                        if (isPerfectForm) {
                            repCount++
                        }
                        isBending = false
                    }
                    formIsGood = true
                    feedback = "Ready, start squatting"
                } else if (isBending) {
                     // In the middle of squat
                     if (hipAngle < 60) {
                        formIsGood = false
                        feedback = "Keep your back straight!"
                     } else {
                        formIsGood = true
                        feedback = "Keep going lower..."
                     }
                }

                isPerfectForm = formIsGood
                currentFeedback = feedback
            } else {
                isPerfectForm = false
                currentFeedback = "Stand straight"
            }
        } else {
            isPerfectForm = false
            currentFeedback = "Ensure full body is visible"
        }
    }

    private fun calculateAngle(firstPoint: PoseLandmark, midPoint: PoseLandmark, lastPoint: PoseLandmark): Double {
        var result = Math.toDegrees(
            atan2(lastPoint.position.y - midPoint.position.y,
                lastPoint.position.x - midPoint.position.x) -
            atan2(firstPoint.position.y - midPoint.position.y,
                firstPoint.position.x - midPoint.position.x).toDouble()
        )
        result = Math.abs(result)
        if (result > 180) {
            result = 360.0 - result
        }
        return result
    }
}
