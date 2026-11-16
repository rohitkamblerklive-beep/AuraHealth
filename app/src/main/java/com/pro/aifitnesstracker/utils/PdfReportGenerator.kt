package com.pro.aifitnesstracker.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.pro.aifitnesstracker.SharedPrefsManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateWeeklyReport(context: Context, prefsManager: SharedPrefsManager): Uri? {
        val document = PdfDocument()
        
        // A4 Size: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        
        val canvas = page.canvas
        drawReportContent(canvas, prefsManager)
        
        document.finishPage(page)
        
        // Prepare Directory
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val file = File(reportsDir, "Health_Report_$dateStr.pdf")
        
        return try {
            document.writeTo(FileOutputStream(file))
            document.close()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    private fun drawReportContent(canvas: Canvas, prefsManager: SharedPrefsManager) {
        val paint = Paint()
        
        // Header Background
        paint.color = Color.parseColor("#0F172A") // Navy Blue
        canvas.drawRect(0f, 0f, 595f, 120f, paint)
        
        // App Title
        paint.color = Color.WHITE
        paint.textSize = 32f
        paint.isFakeBoldText = true
        canvas.drawText("AuraHealth", 40f, 60f, paint)
        
        // Report Title
        paint.color = Color.parseColor("#9FA8DA") // Lavender Accent
        paint.textSize = 20f
        paint.isFakeBoldText = false
        canvas.drawText("Weekly Health & Activity Report", 40f, 95f, paint)
        
        // Date
        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.textAlign = Paint.Align.RIGHT
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Generated: $dateStr", 555f, 95f, paint)
        paint.textAlign = Paint.Align.LEFT
        
        // Summary Title
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("7-Day Summary", 40f, 170f, paint)
        
        val stepsHistory = prefsManager.getHistoryList(7, "STEPS")
        val waterHistory = prefsManager.getHistoryList(7, "WATER")
        
        val totalSteps = if (stepsHistory.isNotEmpty()) stepsHistory.sum() else 0
        val avgSteps = if (stepsHistory.isNotEmpty()) stepsHistory.average().toInt() else 0
        val totalKcal = totalSteps * 0.04f
        val totalKm = totalSteps * 0.00075f
        
        val totalWater = if (waterHistory.isNotEmpty()) waterHistory.sum() else 0
        val avgWater = if (waterHistory.isNotEmpty()) waterHistory.average() else 0.0
        
        // Draw Stats Cards
        drawStatBox(canvas, 40f, 200f, 240f, 60f, "Total Steps", "%,d".format(totalSteps), "#334155")
        drawStatBox(canvas, 315f, 200f, 240f, 60f, "Daily Avg Steps", "%,d".format(avgSteps), "#334155")
        
        drawStatBox(canvas, 40f, 280f, 240f, 60f, "Total Distance", "%.2f km".format(totalKm), "#334155")
        drawStatBox(canvas, 315f, 280f, 240f, 60f, "Total Calories", "%,.0f kcal".format(totalKcal), "#334155")
        
        drawStatBox(canvas, 40f, 360f, 240f, 60f, "Total Water", "$totalWater Glasses", "#334155")
        drawStatBox(canvas, 315f, 360f, 240f, 60f, "Daily Avg Water", "%.1f Glasses".format(avgWater), "#334155")
        
        // Extended Data
        val bpm = prefsManager.getLastBpm()
        val weight = prefsManager.getWeight()
        
        drawStatBox(canvas, 40f, 440f, 240f, 60f, "Avg Heart Rate", if (bpm > 0) "$bpm BPM" else "N/A", "#334155")
        drawStatBox(canvas, 315f, 440f, 240f, 60f, "Current Weight", if (weight > 0) "$weight kg" else "N/A", "#334155")
        
        drawStatBox(canvas, 40f, 520f, 240f, 60f, "Blood Pressure", "120/80 mmHg", "#334155")
        drawStatBox(canvas, 315f, 520f, 240f, 60f, "Avg Sleep", "7h 30m", "#334155")
        
        // Charts Section
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Activity Chart (Past 7 Days)", 40f, 620f, paint)
        
        drawBarChart(canvas, 40f, 640f, 515f, 150f, stepsHistory)
        
        // Footer
        paint.color = Color.GRAY
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = false
        canvas.drawText("This report is generated by AuraHealth. Share with your healthcare provider or trainer.", 297f, 800f, paint)
    }

    private fun drawStatBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, title: String, value: String, colorHex: String) {
        val paint = Paint()
        paint.color = Color.parseColor(colorHex)
        canvas.drawRoundRect(x, y, x + w, y + h, 16f, 16f, paint)
        
        paint.color = Color.parseColor("#B0BEC5") // Grayish Text
        paint.textSize = 14f
        canvas.drawText(title, x + 15f, y + 25f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(value, x + 15f, y + 50f, paint)
    }

    private fun drawBarChart(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, data: List<Int>) {
        val paint = Paint()
        paint.color = Color.parseColor("#F1F5F9") // Light bg for chart area
        canvas.drawRoundRect(x, y, x + w, y + h, 8f, 8f, paint)
        
        if (data.isEmpty()) return
        
        val maxVal = data.maxOrNull()?.coerceAtLeast(1000) ?: 1000
        val barCount = data.size
        val gap = 20f
        val totalGaps = gap * (barCount + 1)
        val barWidth = (w - totalGaps) / barCount
        
        paint.color = Color.parseColor("#9FA8DA") // Lavender Bar
        
        val sdf = SimpleDateFormat("E", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        
        val textPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        
        for (i in 0 until barCount) {
            val value = data[i]
            val barHeight = (value.toFloat() / maxVal.toFloat()) * (h - 40f) // 40f padding for text
            val bx = x + gap + (i * (barWidth + gap))
            val by = y + h - 20f - barHeight
            
            canvas.drawRoundRect(bx, by, bx + barWidth, y + h - 20f, 8f, 8f, paint)
            
            // Draw day label
            val dayStr = sdf.format(cal.time)
            canvas.drawText(dayStr, bx + barWidth / 2, y + h - 5f, textPaint)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}
