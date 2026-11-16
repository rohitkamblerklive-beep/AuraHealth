package com.pro.aifitnesstracker

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.concurrent.Executors

@Composable
fun CameraView(
    onRepsUpdated: (Int) -> Unit,
    onFormUpdated: (Boolean) -> Unit,
    onFeedbackUpdated: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var pose by remember { mutableStateOf<Pose?>(null) }
    var isFormGood by remember { mutableStateOf(false) }
    var frameWidth by remember { mutableIntStateOf(480) }
    var frameHeight by remember { mutableIntStateOf(640) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(executor) {
        onDispose {
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            val analyzer = SquatAnalyzer { detectedPose, reps, form, feedback ->
                                pose = detectedPose
                                isFormGood = form
                                onRepsUpdated(reps)
                                onFormUpdated(form)
                                onFeedbackUpdated(feedback)
                            }
                            analyzer.onPoseWithDimensions = { _, _, _, _, w, h ->
                                frameWidth = w
                                frameHeight = h
                            }
                            it.setAnalyzer(executor, analyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraView", "Use case binding failed", exc)
                    }

                }, ContextCompat.getMainExecutor(context))
                previewView
            }
        )
        
        // SKELETON OVERLAY (Dynamic Theme with accurate aspect ratio & front camera mirroring)
        pose?.let { detectedPose ->
            val skeletonColor = if (isFormGood) Color(0xFF00E676) else Color(0xFFFF5252)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / frameWidth.toFloat()
                val scaleY = size.height / frameHeight.toFloat()

                fun getScreenX(rawX: Float): Float {
                    // Front camera is mirrored in PreviewView, so mirror X to match user's perspective
                    val mirroredX = frameWidth - rawX
                    return mirroredX * scaleX
                }

                fun getScreenY(rawY: Float): Float {
                    return rawY * scaleY
                }

                val landmarks = detectedPose.allPoseLandmarks
                if (landmarks.isNotEmpty()) {
                    for (landmark in landmarks) {
                        if (landmark.inFrameLikelihood > 0.5f) {
                            val x = getScreenX(landmark.position.x)
                            val y = getScreenY(landmark.position.y)
                            drawCircle(
                                color = skeletonColor,
                                radius = 8f,
                                center = Offset(x, y)
                            )
                        }
                    }

                    fun drawBone(startType: Int, endType: Int) {
                        val start = detectedPose.getPoseLandmark(startType)
                        val end = detectedPose.getPoseLandmark(endType)
                        if (start != null && end != null && start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f) {
                            drawLine(
                                color = skeletonColor,
                                start = Offset(getScreenX(start.position.x), getScreenY(start.position.y)),
                                end = Offset(getScreenX(end.position.x), getScreenY(end.position.y)),
                                strokeWidth = 8f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
                    drawBone(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
                    drawBone(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
                    drawBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
                    drawBone(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
                    drawBone(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
                    drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
                    drawBone(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
                }
            }
        }
    }
}
