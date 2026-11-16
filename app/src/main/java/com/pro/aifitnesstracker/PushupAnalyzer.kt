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

class PushupAnalyzer(
    private val onPoseDetected: (Pose, Int, Boolean, String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val poseDetector = PoseDetection.getClient(options)
    
    private var repCount = 0
    private var isDown = false
    private var isPerfectForm = false
    private var currentFeedback = "Get into pushup position"

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    processPose(pose)
                    onPoseDetected(pose, repCount, isPerfectForm, currentFeedback)
                }
                .addOnFailureListener {}
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun processPose(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        var shoulder = leftShoulder
        var elbow = leftElbow
        var wrist = leftWrist
        var hip = leftHip
        var ankle = leftAnkle

        if (rightShoulder != null && rightElbow != null && rightWrist != null && leftShoulder != null && leftElbow != null && leftWrist != null) {
            val rightVisibility = rightShoulder.inFrameLikelihood + rightElbow.inFrameLikelihood + rightWrist.inFrameLikelihood
            val leftVisibility = leftShoulder.inFrameLikelihood + leftElbow.inFrameLikelihood + leftWrist.inFrameLikelihood
            if (rightVisibility > leftVisibility) {
                shoulder = rightShoulder
                elbow = rightElbow
                wrist = rightWrist
            }
        }

        if (shoulder != null && elbow != null && wrist != null && hip != null && ankle != null &&
            shoulder.inFrameLikelihood > 0.4f && elbow.inFrameLikelihood > 0.4f && wrist.inFrameLikelihood > 0.4f) {
            
            val elbowAngle = calculateAngle(shoulder, elbow, wrist)
            val bodyAngle = calculateAngle(shoulder, hip, ankle)

            var formIsGood = true
            var feedback = "Ready for pushups"

            if (bodyAngle < 150) {
                formIsGood = false
                feedback = "Keep your body straight like a plank"
            } else {
                if (elbowAngle < 90) { // Going down
                    isDown = true
                    formIsGood = true
                    feedback = "Perfect depth! Push up."
                } else if (elbowAngle > 150) { // Going up
                    if (isDown) {
                        if (isPerfectForm) {
                            repCount++
                        }
                        isDown = false
                    }
                    formIsGood = true
                    feedback = "Go down"
                } else if (isDown) {
                    formIsGood = true
                    feedback = "Keep pushing up"
                } else {
                    formIsGood = true
                    feedback = "Go lower"
                }
            }

            isPerfectForm = formIsGood
            currentFeedback = feedback
        } else {
            isPerfectForm = false
            currentFeedback = "Ensure side view is visible"
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
