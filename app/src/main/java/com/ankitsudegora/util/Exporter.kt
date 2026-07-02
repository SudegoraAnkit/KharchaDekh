package com.ankitsudegora.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt
import androidx.core.content.FileProvider
import com.ankitsudegora.data.TransactionWithCategory
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

    fun exportCcStatement(context: Context, cardName: String, transactions: List<TransactionWithCategory>): Uri? {
        val csvHeader = "Purchase Date,Merchant,Amount,Category,Settlement Reference ID\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        val csvContent = StringBuilder(csvHeader)
        for (item in transactions) {
            val t = item.transaction
            val dateStr = dateFormat.format(Date(t.timestamp))
            val categoryName = item.category?.name ?: "Uncategorized"
            val merchantEscaped = t.merchant.replace("\"", "\"\"")
            val repaymentIdStr = t.ccRepaymentId?.toString() ?: "Unbilled"
            
            csvContent.append("\"$dateStr\",")
                .append("\"$merchantEscaped\",")
                .append("${t.amount},")
                .append("\"$categoryName\",")
                .append("\"$repaymentIdStr\"\n")
        }

        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "KharchaDekh_${cardName.replace(" ", "_")}_Statement_${System.currentTimeMillis()}.csv")
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
        val textPaint = TextPaint().apply {
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
        val pageNumPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        var pageNumber = 1
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 50f
        var isPageFinished = false

        fun drawPageHeader(canvas: Canvas, pageNum: Int) {
            var y = 50f
            if (pageNum == 1) {
                canvas.drawText("KharchaDekh Expense Report", 50f, y, headerPaint)
                y += 20f
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                canvas.drawText("Generated on: $dateStr", 50f, y, subHeaderPaint)
                y += 30f

                val totalDebit = transactions.filter { it.transaction.type == "DEBIT" }.sumOf { it.transaction.amount }
                val totalCredit = transactions.filter { it.transaction.type == "CREDIT" }.sumOf { it.transaction.amount }
                
                paint.color = Color.LTGRAY
                canvas.drawRect(50f, y, 545f, y + 40f, paint)
                
                textPaint.isFakeBoldText = true
                canvas.drawText("Total Outflow: ₹%,.2f".format(totalDebit), 60f, y + 25f, textPaint)
                canvas.drawText("Total Inflow: ₹%,.2f".format(totalCredit), 300f, y + 25f, textPaint)
                y += 60f
            } else {
                canvas.drawText("KharchaDekh Expense Report (Continued)", 50f, y, headerPaint)
                y += 30f
            }

            paint.color = Color.BLACK
            paint.strokeWidth = 1f
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 15f
            
            textPaint.isFakeBoldText = true
            canvas.drawText("Date", 50f, y, textPaint)
            canvas.drawText("Merchant / Source", 150f, y, textPaint)
            canvas.drawText("Category", 320f, y, textPaint)
            canvas.drawText("Type", 420f, y, textPaint)
            canvas.drawText("Amount", 480f, y, textPaint)
            y += 10f
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 20f
            textPaint.isFakeBoldText = false
            
            currentY = y
        }

        fun drawPageFooter(canvas: Canvas, pageNum: Int) {
            canvas.drawText("Made with 💝 by Ankit Sudegora • 100% Offline & Private", 595f / 2f, 815f, footerPaint)
            canvas.drawText("Page $pageNum", 545f, 815f, pageNumPaint)
        }

        drawPageHeader(canvas, pageNumber)

        val dateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())

        for (item in transactions) {
            val rowHeight = 22f
            if (currentY + rowHeight > 842f - 50f) {
                drawPageFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                isPageFinished = true

                pageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                isPageFinished = false

                drawPageHeader(canvas, pageNumber)
            }

            val t = item.transaction
            val dateFormatted = dateFormat.format(Date(t.timestamp))
            val merchantName = TextUtils.ellipsize(t.merchant, textPaint, 155f, TruncateAt.END).toString()
            val catName = item.category?.name ?: "Uncategorized"
            val categoryName = TextUtils.ellipsize(catName, textPaint, 90f, TruncateAt.END).toString()
            
            canvas.drawText(dateFormatted, 50f, currentY, textPaint)
            canvas.drawText(merchantName, 150f, currentY, textPaint)
            canvas.drawText(categoryName, 320f, currentY, textPaint)
            canvas.drawText(t.type, 420f, currentY, textPaint)
            
            val amtStr = "₹%,.0f".format(t.amount)
            canvas.drawText(amtStr, 480f, currentY, textPaint)

            currentY += rowHeight
        }

        if (!isPageFinished) {
            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)
            isPageFinished = true
        }

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
