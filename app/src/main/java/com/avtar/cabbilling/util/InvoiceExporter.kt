package com.avtar.cabbilling.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.data.local.entity.TripEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceExporter {

    private const val SHARE_DIR = "shared_invoices"
    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun buildPlainText(bill: BillEntity, currencySymbol: String, companyPhone: String = ""): String {
        val date = Formatters.dateFull.format(Formatters.localDate(bill.dateEpochMillis))
        return buildString {
            appendLine(bill.companyName.uppercase())
            if (companyPhone.isNotBlank()) appendLine("Ph: $companyPhone")
            appendLine("INVOICE / RECEIPT")
            appendLine()
            appendLine("Invoice No : ${bill.invoiceCode}")
            appendLine("Date       : $date")
            appendLine()
            appendLine("From       : ${bill.fromLocation}")
            appendLine("To         : ${bill.toLocation}")
            appendLine("Car        : ${bill.carName}${if (!bill.carPlate.isNullOrBlank()) " (${bill.carPlate})" else ""}")
            if (bill.passengerName.isNotBlank()) appendLine("Passenger  : ${bill.passengerName}")
            appendLine()
            appendLine("Amount     : ${Formatters.amount(bill.amount, currencySymbol)}")
            if (bill.notes.isNotBlank()) {
                appendLine()
                appendLine("Notes: ${bill.notes}")
            }
            appendLine()
            append("— Generated offline by Cab Billing")
        }
    }

    fun sharePlainText(context: Context, bill: BillEntity, currencySymbol: String, companyPhone: String = ""): Result<Unit> =
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${bill.invoiceCode} · ${bill.companyName}")
                putExtra(Intent.EXTRA_TEXT, buildPlainText(bill, currencySymbol, companyPhone))
            }
            launchChooser(context, send, "Share invoice text")
        }

    fun sharePdf(context: Context, bill: BillEntity, currencySymbol: String, companyPhone: String = ""): Result<Unit> =
        runCatching {
            val uri = writePdf(context, bill, currencySymbol, companyPhone)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${bill.invoiceCode} · ${bill.companyName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            launchChooser(context, send, "Share invoice PDF")
        }

    fun shareToWhatsApp(context: Context, bill: BillEntity, currencySymbol: String, companyPhone: String = ""): Result<Unit> =
        runCatching {
            val uri = writePdf(context, bill, currencySymbol, companyPhone)
            fun sendIntent() = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption(bill, currencySymbol))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launched = listOf(WHATSAPP, WHATSAPP_BUSINESS).any { pkg ->
                try {
                    context.startActivity(sendIntent().setPackage(pkg))
                    true
                } catch (e: ActivityNotFoundException) {
                    false
                }
            }
            if (!launched) launchChooser(context, sendIntent(), "Share invoice")
        }

    fun shareAsImage(context: Context, bill: BillEntity, currencySymbol: String, companyPhone: String = ""): Result<Unit> =
        runCatching {
            val uri = writeReceiptImage(context, bill, currencySymbol, companyPhone)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${bill.invoiceCode} · ${bill.companyName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            launchChooser(context, send, "Share invoice image")
        }

    fun shareTripAsImage(context: Context, trip: TripEntity): Result<Unit> =
        runCatching {
            val uri = writeTripImage(context, trip)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Trip Record · ${formatTripDate(trip.startTime)}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            launchChooser(context, send, "Share trip record")
        }

    private fun caption(bill: BillEntity, currencySymbol: String): String =
        "${bill.companyName}\n" +
            "Invoice #${bill.invoiceCode} · ${Formatters.amount(bill.amount, currencySymbol)}\n" +
            "${bill.fromLocation} → ${bill.toLocation}"

    private fun launchChooser(context: Context, intent: Intent, title: String) {
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    // ---- Receipt Image -------------------------------------------------------

    private fun writeReceiptImage(context: Context, bill: BillEntity, currencySymbol: String, companyPhone: String): Uri {
        val w = 840
        val h = 1120
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        drawReceiptImage(canvas, w, bill, currencySymbol, companyPhone)
        return savePng(context, bmp, "Invoice_${bill.invoiceCode}.png")
    }

    private fun drawReceiptImage(canvas: Canvas, width: Int, bill: BillEntity, currencySymbol: String, companyPhone: String) {
        val scale = 2f
        val left = 36f * scale
        val right = width - 36f * scale
        var y = 64f * scale

        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1a73e8") }
        canvas.drawRoundRect(RectF(20f, 20f, width - 20f, 200f), 24f, 24f, headerBg)

        val headerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DDEEFF"); textSize = 16f }

        canvas.drawText(bill.companyName.uppercase(), left, 80f, headerTitle)
        if (companyPhone.isNotBlank()) {
            canvas.drawText("Ph: $companyPhone", left, 110f, headerSub)
        }
        canvas.drawText("INVOICE / RECEIPT", left, if (companyPhone.isNotBlank()) 140f else 115f, headerSub)
        canvas.drawText("#${bill.invoiceCode}", right - 200f, 80f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 32f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT
        })

        y = 250f

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 20f }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827"); textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 2f }

        fun row(l: String, v: String) {
            canvas.drawText(l, left, y, label)
            canvas.drawText(v, left + 200f, y, value)
            y += 46f
        }

        val date = Formatters.dateFull.format(Formatters.localDate(bill.dateEpochMillis))
        row("Invoice No", bill.invoiceCode)
        row("Date", date)
        row("From", bill.fromLocation)
        row("To", bill.toLocation)
        row("Car", "${bill.carName}${if (!bill.carPlate.isNullOrBlank()) " (${bill.carPlate})" else ""}")
        if (bill.passengerName.isNotBlank()) row("Passenger", bill.passengerName)

        y += 10f
        canvas.drawLine(left, y, right, y, rule)
        y += 50f

        canvas.drawText("TOTAL AMOUNT", left, y, label)
        y += 50f
        canvas.drawText(Formatters.amount(bill.amount, currencySymbol), left, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E7C66"); textSize = 44f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        if (bill.notes.isNotBlank()) {
            y += 56f
            canvas.drawText("Notes", left, y, label)
            y += 34f
            canvas.drawText(bill.notes, left, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#374151"); textSize = 18f
            })
        }

        canvas.drawText("Generated by Cab Billing", left, 1080f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF"); textSize = 16f
        })
    }

    // ---- Trip Image ----------------------------------------------------------

    private fun writeTripImage(context: Context, trip: TripEntity): Uri {
        val w = 840
        val h = 1000
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        drawTripImage(canvas, w, trip)
        return savePng(context, bmp, "Trip_${trip.id}.png")
    }

    private fun drawTripImage(canvas: Canvas, width: Int, trip: TripEntity) {
        val left = 60f
        val right = width - 60f

        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1a73e8") }
        canvas.drawRoundRect(RectF(20f, 20f, width - 20f, 160f), 24f, 24f, headerBg)

        val headerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 30f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DDEEFF"); textSize = 16f }

        canvas.drawText("TRIP RECORD", left, 80f, headerTitle)
        canvas.drawText(formatTripDate(trip.startTime), left, 120f, headerSub)
        if (trip.endTime > 0) {
            canvas.drawText("to  ${formatTripTime(trip.endTime)}", left + 240f, 120f, headerSub)
        }

        var y = 210f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 18f }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827"); textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 2f }

        if (trip.startAddress.isNotBlank() || trip.endAddress.isNotBlank()) {
            canvas.drawText("Route", left, y, label)
            y += 34f
            canvas.drawText(
                "${trip.startAddress.ifBlank { "—" }}  →  ${trip.endAddress.ifBlank { "—" }}",
                left, y, value
            )
            y += 44f
        }

        canvas.drawLine(left, y, right, y, rule)
        y += 40f

        val durationSec = if (trip.endTime > 0) (trip.endTime - trip.startTime) / 1000 else 0
        val statLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 15f; textAlign = Paint.Align.CENTER }
        val statValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827"); textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
        }

        val cols = floatArrayOf(left + 80f, left + 260f, left + 440f, left + 620f)
        val labels = arrayOf("Distance", "Duration", "Max Speed", "Avg Speed")
        val values = arrayOf(
            "${"%.2f".format(trip.distanceKm)} km",
            formatDur(durationSec),
            "${"%.0f".format(trip.maxSpeedKmh)} km/h",
            "${"%.0f".format(trip.avgSpeedKmh)} km/h"
        )
        for (i in 0..3) {
            canvas.drawText(values[i], cols[i], y, statValue)
            canvas.drawText(labels[i], cols[i], y + 28f, statLabel)
        }

        y += 80f
        canvas.drawText("Route", left, y, label)
        y += 20f
        val mapW = width - 2 * left
        val mapH = 520f
        drawTripMap(canvas, left, y, mapW, mapH, TripPath.decode(trip.path))

        canvas.drawText("Recorded by Cab Billing", left, y + mapH + 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF"); textSize = 14f
        })
    }

    private fun drawTripMap(canvas: Canvas, left: Float, top: Float, w: Float, h: Float, points: List<GeoPoint>) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EEF2F7") }
        canvas.drawRoundRect(RectF(left, top, left + w, top + h), 20f, 20f, bg)

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D6DEE8"); strokeWidth = 1.5f }
        val step = 64f
        var gx = left + step
        while (gx < left + w) { canvas.drawLine(gx, top, gx, top + h, grid); gx += step }
        var gy = top + step
        while (gy < top + h) { canvas.drawLine(left, gy, left + w, gy, grid); gy += step }

        if (points.isEmpty()) {
            canvas.drawText("Route not recorded", left + w / 2f, top + h / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8"); textSize = 22f; textAlign = Paint.Align.CENTER
            })
            return
        }

        val proj = TripPath.project(points, w, h, 44f)
        if (proj.size >= 2) {
            val route = Path().apply {
                moveTo(left + proj[0].x, top + proj[0].y)
                for (i in 1 until proj.size) lineTo(left + proj[i].x, top + proj[i].y)
            }
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1a73e8"); style = Paint.Style.STROKE
                strokeWidth = 9f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(route, line)
        }
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val startP = proj.first()
        canvas.drawCircle(left + startP.x, top + startP.y, 15f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E7D32") })
        canvas.drawCircle(left + startP.x, top + startP.y, 6f, white)
        if (proj.size > 1) {
            val endP = proj.last()
            canvas.drawCircle(left + endP.x, top + endP.y, 15f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") })
            canvas.drawCircle(left + endP.x, top + endP.y, 6f, white)
        }
    }

    // ---- Helpers --------------------------------------------------------------

    private fun savePng(context: Context, bmp: Bitmap, fileName: String): Uri {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun formatTripDate(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))

    private fun formatTripTime(millis: Long): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))

    private fun formatDur(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    // ---- PDF -----------------------------------------------------------------

    private fun writePdf(context: Context, bill: BillEntity, currencySymbol: String, companyPhone: String = ""): Uri {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val file = File(dir, "Invoice_${bill.invoiceCode}.pdf")

        val document = PdfDocument()
        try {
            val pageWidth = 420
            val pageHeight = 595
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            )
            drawReceipt(page.canvas, pageWidth, bill, currencySymbol, companyPhone)
            document.finishPage(page)

            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawReceipt(canvas: Canvas, width: Int, bill: BillEntity, currencySymbol: String, companyPhone: String = "") {
        val left = 36f
        val right = width - 36f
        var y = 64f

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10151F"); textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 12f }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 12f }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827"); textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }

        canvas.drawText(bill.companyName, left, y, title)
        y += 16f
        if (companyPhone.isNotBlank()) {
            canvas.drawText("Ph: $companyPhone", left, y, subtitle)
            y += 14f
        }
        canvas.drawText("INVOICE / RECEIPT", left, y, subtitle)
        y += 16f
        canvas.drawLine(left, y, right, y, rule)
        y += 28f

        fun row(l: String, v: String) {
            canvas.drawText(l, left, y, label)
            canvas.drawText(v, left + 120f, y, value)
            y += 26f
        }

        val date = Formatters.dateFull.format(Formatters.localDate(bill.dateEpochMillis))
        row("Invoice No", bill.invoiceCode)
        row("Date", date)
        row("From", bill.fromLocation)
        row("To", bill.toLocation)
        row("Car", "${bill.carName}${if (!bill.carPlate.isNullOrBlank()) " (${bill.carPlate})" else ""}")
        if (bill.passengerName.isNotBlank()) row("Passenger", bill.passengerName)

        y += 6f
        canvas.drawLine(left, y, right, y, rule)
        y += 30f

        canvas.drawText("TOTAL AMOUNT", left, y, label)
        y += 30f
        canvas.drawText(Formatters.amount(bill.amount, currencySymbol), left, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E7C66"); textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        if (bill.notes.isNotBlank()) {
            y += 34f
            canvas.drawText("Notes", left, y, label)
            y += 20f
            canvas.drawText(bill.notes, left, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#374151"); textSize = 12f
            })
        }

        canvas.drawText("Generated by Cab Billing", left, 560f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF"); textSize = 10f
        })
    }
}
