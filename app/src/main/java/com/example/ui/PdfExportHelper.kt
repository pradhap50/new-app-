package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.SlideWithVariables
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfExportHelper {

    fun exportSlideToPdf(context: Context, slideWithVariables: SlideWithVariables, computedResult: Double, decimalPlaces: Int = 2): File? {
        val pdfDocument = PdfDocument()
        
        // Define standard A4 page size (72 points per inch) -> 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        
        val canvas: Canvas = page.canvas
        val paint = Paint()
        
        // Background color
        canvas.drawColor(Color.WHITE)
        
        // Title
        paint.color = Color.DKGRAY
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ChemDose Formula Report", 40f, 60f, paint)
        
        // Subtitle/Source
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.GRAY
        canvas.drawText("Generated from ChemDose Formula Calculator App", 40f, 80f, paint)
        
        // Draw decorative thin divider line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, 100f, 555f, 100f, paint)
        
        // Section: Slide Title and description
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("Slide #${slideWithVariables.slide.id}: ${slideWithVariables.slide.title}", 40f, 140f, paint)
        
        // Category
        paint.textSize = 10f
        paint.color = Color.parseColor("#1565C0") // elegant blue
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Category: ${slideWithVariables.slide.category.uppercase(Locale.US)}", 40f, 165f, paint)
        
        // Details Box background
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(40f, 180f, 555f, 260f, paint)
        
        // Details text (Formula and description)
        paint.color = Color.DKGRAY
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Formula Expression:  ${slideWithVariables.slide.formula}", 60f, 210f, paint)
        
        val desc = if (slideWithVariables.slide.description.isNotEmpty()) slideWithVariables.slide.description else "Custom Formula Slide calculation report."
        // Split description if extra long
        if (desc.length > 80) {
            canvas.drawText("Description: ${desc.substring(0, 80)}...", 60f, 235f, paint)
        } else {
            canvas.drawText("Description: $desc", 60f, 235f, paint)
        }
        
        // Section Header: Inputs
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("Input Variable Parameters", 40f, 300f, paint)
        
        // Simple Grid List for Variables
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        var startY = 330f
        
        // Headers of Grid Table
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.GRAY
        canvas.drawText("Symbol", 50f, startY, paint)
        canvas.drawText("Parameter Name", 140f, startY, paint)
        canvas.drawText("User Value", 380f, startY, paint)
        canvas.drawText("Unit", 480f, startY, paint)
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, startY + 5, 555f, startY + 5, paint)
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        val valueFormatStr = "%,.${decimalPlaces}f"
        for (v in slideWithVariables.variables) {
            startY += 25f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(v.symbol, 50f, startY, paint)
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val trimmedName = if (v.name.length > 30) v.name.substring(0, 28) + ".." else v.name
            canvas.drawText(trimmedName, 140f, startY, paint)
            canvas.drawText(String.format(Locale.US, valueFormatStr, v.value), 380f, startY, paint)
            canvas.drawText(v.unit, 480f, startY, paint)
        }
        
        // Add Result Block at bottom half
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E3F2FD") // Light blue block
        canvas.drawRect(40f, 520f, 555f, 620f, paint)
        
        // Label
        paint.color = Color.parseColor("#0D47A1") // Dark blue Text
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("CALCULATED REAL-TIME OUTCOME", 60f, 555f, paint)
        
        // Numeric Result
        paint.textSize = 28f
        val resultStr = if (computedResult.isNaN()) "MATH ERROR" else String.format(Locale.US, valueFormatStr, computedResult)
        canvas.drawText("$resultStr  ${slideWithVariables.slide.resultUnit}", 60f, 595f, paint)
        
        // Timestamp Footer
        paint.color = Color.GRAY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Report Timestamp: $timestamp", 40f, 790f, paint)
        canvas.drawText("ChemDose Formula Offline Calculator Engine", 400f, 790f, paint)
        
        // Finish page
        pdfDocument.finishPage(page)
        
        // Save PDF file in cacheDir
        val outputDir = context.cacheDir
        val fileName = "ChemDose_Formula_Report_Slide_${slideWithVariables.slide.id}.pdf"
        val pdfFile = File(outputDir, fileName)
        
        return try {
            val fileOutputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Shares the exported PDF report using FileProvider.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val pdfUri = FileProvider.getUriForFile(context, authority, pdfFile)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "ChemDose Formula Calculation Report")
            putExtra(Intent.EXTRA_TEXT, "Here is the PDF report for the ChemDose formula calculation.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Share Calculation Report")
        context.startActivity(chooser)
    }

}
