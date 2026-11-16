package com.pro.aifitnesstracker

import android.graphics.PointF
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class YogaAnalyzer {

    enum class PoseType { NONE, PLANK, TREE, WARRIOR }

    data class YogaResult(
        val poseType: PoseType,
        val isCorrect: Boolean,
        val feedback: String,
        val progress: Float = 0f,
        val pose: Pose? = null
    )

    fun analyzePose(pose: Pose, targetPose: PoseType): YogaResult {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) return YogaResult(PoseType.NONE, false, "No person detected", 0f, pose)

        return when (targetPose) {
            PoseType.PLANK -> analyzePlank(pose)
            PoseType.TREE -> analyzeTree(pose)
            PoseType.WARRIOR -> analyzeWarrior(pose)
            else -> YogaResult(PoseType.NONE, false, "Select a pose", 0f, pose)
        }
    }

    private fun analyzePlank(pose: Pose): YogaResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        if (shoulder == null || hip == null || knee == null || ankle == null) {
            return YogaResult(PoseType.PLANK, false, "Ensure full body is visible", 0f, pose)
        }

        val bodyAngle = getAngle(shoulder, hip, knee)
        val isStraight = bodyAngle in 165.0..195.0
        val isHorizontal = abs(shoulder.position.y - ankle.position.y) < abs(shoulder.position.x - ankle.position.x)

        return if (isStraight && isHorizontal) {
            YogaResult(PoseType.PLANK, true, "Perfect Plank! Hold it.", 1f, pose)
        } else if (!isStraight) {
            YogaResult(PoseType.PLANK, false, "Keep your hips level", 0.5f, pose)
        } else {
            YogaResult(PoseType.PLANK, false, "Get into horizontal position", 0.2f, pose)
        }
    }

    private fun analyzeTree(pose: Pose): YogaResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        if (leftHip == null || leftKnee == null || leftAnkle == null || rightAnkle == null || rightKnee == null) {
            return YogaResult(PoseType.TREE, false, "Ensure legs are visible", 0f, pose)
        }

        val isRightFootLifted = rightAnkle.position.y < leftKnee.position.y && rightAnkle.position.y > leftHip.position.y
        val isLeftLegStraight = abs(leftHip.position.x - leftAnkle.position.x) < 100 

        return if (isRightFootLifted && isLeftLegStraight) {
            YogaResult(PoseType.TREE, true, "Great balance! Focus on one point.", 1f, pose)
        } else {
            YogaResult(PoseType.TREE, false, "Place your foot on your inner thigh", 0.3f, pose)
        }
    }

    private fun analyzeWarrior(pose: Pose): YogaResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        if (leftShoulder == null || rightShoulder == null || leftWrist == null || rightWrist == null || leftKnee == null || leftAnkle == null || rightAnkle == null) {
            return YogaResult(PoseType.WARRIOR, false, "Ensure full body is visible", 0f, pose)
        }

        // Arms should be horizontal
        val leftArmAngle = abs(leftShoulder.position.y - leftWrist.position.y)
        val rightArmAngle = abs(rightShoulder.position.y - rightWrist.position.y)
        val armsHorizontal = leftArmAngle < 100 && rightArmAngle < 100

        // Legs should be wide
        val legDistance = abs(leftAnkle.position.x - rightAnkle.position.x)
        val legsWide = legDistance > 250

        return if (armsHorizontal && legsWide) {
            YogaResult(PoseType.WARRIOR, true, "Strong Warrior! Gaze forward.", 1f, pose)
        } else if (!armsHorizontal) {
            YogaResult(PoseType.WARRIOR, false, "Keep arms parallel to the floor", 0.5f, pose)
        } else {
            YogaResult(PoseType.WARRIOR, false, "Step your feet wider apart", 0.3f, pose)
        }
    }

    private fun getAngle(firstPoint: PoseLandmark, midPoint: PoseLandmark, lastPoint: PoseLandmark): Double {
        var result = Math.toDegrees(
            (atan2(lastPoint.position.y - midPoint.position.y, lastPoint.position.x - midPoint.position.x) -
                    atan2(firstPoint.position.y - midPoint.position.y, firstPoint.position.x - midPoint.position.x)).toDouble()
        )
        result = abs(result)
        if (result > 180) result = 360.0 - result
        return result
    }
}
