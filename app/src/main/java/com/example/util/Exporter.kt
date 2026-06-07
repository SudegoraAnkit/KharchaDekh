package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.TransactionWithCategory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object Exporter {

    fun exportToCsv(context: Context, transactions: List<TransactionWithCategory>): Uri? {
        val csvHeader = "ID,Date,Merchant,Amount,Type,Category,Payment Method,Notes\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        val csvContent = StringBuilder(csvHeader)
        for (item in transactions) {
            val t = item.transaction
            val dateStr = dateFormat.format(Date(t.timestamp))
            val categoryName = item.category?.name ?: "Uncategorized"
            val notesEscaped = (t.notes ?: "").replace("\"", "\"\"")
            val merchantEscaped = t.merchant.replace("\"", "\"\"")
            
            csvContent.append("${t.id},")
                .append("\"$dateStr\",")
                .append("\"$merchantEscaped\",")
                .append("${t.amount},")
                .append("${t.type},")
                .append("\"$categoryName\",")
                .append("${t.paymentMethod},")
                .append("\"$notesEscaped\"\n")
        }

        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "KharchaDekh_Report_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use { out ->
                out.write(csvContent.toString().toByteArray())
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(context: Context, transactions: List<TransactionWithCategory>): Uri? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subHeaderPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Draw page 1
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPosition = 50f
        
        // Draw Header
        canvas.drawText("KharchaDekh Expense Report", 50f, yPosition, headerPaint)
        yPosition += 20f
        
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr", 50f, yPosition, subHeaderPaint)
        yPosition += 30f

        // Draw summary metrics
        val totalDebit = transactions.filter { it.transaction.type == "DEBIT" }.sumOf { it.transaction.amount }
        val totalCredit = transactions.filter { it.transaction.type == "CREDIT" }.sumOf { it.transaction.amount }
        
        paint.color = Color.LTGRAY
        canvas.drawRect(50f, yPosition, 545f, yPosition + 40f, paint)
        
        textPaint.isFakeBoldText = true
        canvas.drawText("Total Outflow: ₹%,.2f".format(totalDebit), 60f, yPosition + 25f, textPaint)
        canvas.drawText("Total Inflow: ₹%,.2f".format(totalCredit), 300f, yPosition + 25f, textPaint)
        yPosition += 60f

        // Draw Table Header
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
        yPosition += 15f
        
        textPaint.isFakeBoldText = true
        canvas.drawText("Date", 50f, yPosition, textPaint)
        canvas.drawText("Merchant / Source", 150f, yPosition, textPaint)
        canvas.drawText("Category", 320f, yPosition, textPaint)
        canvas.drawText("Type", 420f, yPosition, textPaint)
        canvas.drawText("Amount", 480f, yPosition, textPaint)
        yPosition += 10f
        canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
        yPosition += 20f

        textPaint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())

        for (item in transactions) {
            // Check if page height exceeded
            if (yPosition > 780f) {
                canvas.drawText("Made with 💝 by Ankit Sudegora • 100% Offline & Private", 595f / 2f, 815f, footerPaint)
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yPosition = 50f
                
                // Draw continuation table header
                canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
                yPosition += 15f
                textPaint.isFakeBoldText = true
                canvas.drawText("Date", 50f, yPosition, textPaint)
                canvas.drawText("Merchant / Source", 150f, yPosition, textPaint)
                canvas.drawText("Category", 320f, yPosition, textPaint)
                canvas.drawText("Type", 420f, yPosition, textPaint)
                canvas.drawText("Amount", 480f, yPosition, textPaint)
                yPosition += 10f
                canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
                yPosition += 20f
                textPaint.isFakeBoldText = false
            }

            val t = item.transaction
            val dateFormatted = dateFormat.format(Date(t.timestamp))
            val merchantName = if (t.merchant.length > 20) t.merchant.substring(0, 18) + ".." else t.merchant
            val catName = item.category?.name ?: "Uncategorized"
            val categoryName = if (catName.length > 12) catName.substring(0, 10) + ".." else catName
            
            canvas.drawText(dateFormatted, 50f, yPosition, textPaint)
            canvas.drawText(merchantName, 150f, yPosition, textPaint)
            canvas.drawText(categoryName, 320f, yPosition, textPaint)
            canvas.drawText(t.type, 420f, yPosition, textPaint)
            
            val amtStr = "₹%,.0f".format(t.amount)
            canvas.drawText(amtStr, 480f, yPosition, textPaint)

            yPosition += 22f
        }

        canvas.drawText("Made with 💝 by Ankit Sudegora • 100% Offline & Private", 595f / 2f, 815f, footerPaint)
        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "KharchaDekh_Statement_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
