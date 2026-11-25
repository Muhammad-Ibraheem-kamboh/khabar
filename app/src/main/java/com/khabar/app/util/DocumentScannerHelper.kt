package com.khabar.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.khabar.app.di.AppContainer
import com.khabar.app.domain.model.ExtractedEntities
import com.khabar.app.domain.model.RawMail
import com.khabar.app.rules.EntityExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class ScannedDocumentResult(
    val rawText: String,
    val detectedTitle: String,
    val previewBitmap: Bitmap?,
    val entities: ExtractedEntities,
    val isPdf: Boolean
)

data class ScannedSaveOutcome(
    val mailId: String,
    val reminderCreated: Boolean,
    val headline: String
)

object DocumentScannerHelper {

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun processDocumentUri(
        context: Context,
        uri: Uri,
        mimeType: String? = null
    ): ScannedDocumentResult = withContext(Dispatchers.IO) {
        val isPdf = mimeType?.contains("pdf", ignoreCase = true) == true ||
                uri.toString().contains(".pdf", ignoreCase = true) ||
                (context.contentResolver.getType(uri)?.contains("pdf", ignoreCase = true) == true)

        if (isPdf) {
            processPdfUri(context, uri)
        } else {
            processImageUri(context, uri)
        }
    }

    suspend fun processBitmap(
        bitmap: Bitmap,
        suggestedTitle: String = "Campus Notice"
    ): ScannedDocumentResult = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = Tasks.await(textRecognizer.process(inputImage))
        val text = visionText.text

        val title = extractDocumentTitle(text, suggestedTitle)
        val entities = EntityExtractor.extract(
            subject = title,
            body = text,
            receivedAtEpochMs = System.currentTimeMillis()
        )

        ScannedDocumentResult(
            rawText = text,
            detectedTitle = title,
            previewBitmap = bitmap,
            entities = entities,
            isPdf = false
        )
    }

    private suspend fun processImageUri(context: Context, uri: Uri): ScannedDocumentResult {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: throw IllegalArgumentException("Could not open image file")

        return processBitmap(bitmap, "Scanned Notice")
    }

    private suspend fun processPdfUri(context: Context, uri: Uri): ScannedDocumentResult {
        val bitmaps = renderPdfPages(context, uri, maxPages = 3)
        if (bitmaps.isEmpty()) {
            throw IllegalArgumentException("PDF file is empty or could not be rendered")
        }

        val fullTextBuilder = StringBuilder()
        for ((index, bmp) in bitmaps.withIndex()) {
            val inputImage = InputImage.fromBitmap(bmp, 0)
            val visionText = Tasks.await(textRecognizer.process(inputImage))
            if (visionText.text.isNotBlank()) {
                if (index > 0) fullTextBuilder.append("\n--- Page ${index + 1} ---\n")
                fullTextBuilder.append(visionText.text)
            }
        }

        val combinedText = fullTextBuilder.toString()
        val title = extractDocumentTitle(combinedText, "Campus Circular / Fee Voucher")
        val entities = EntityExtractor.extract(
            subject = title,
            body = combinedText,
            receivedAtEpochMs = System.currentTimeMillis()
        )

        return ScannedDocumentResult(
            rawText = combinedText,
            detectedTitle = title,
            previewBitmap = bitmaps.firstOrNull(),
            entities = entities,
            isPdf = true
        )
    }

    private fun renderPdfPages(context: Context, uri: Uri, maxPages: Int = 3): List<Bitmap> {
        val renderedBitmaps = mutableListOf<Bitmap>()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()

        try {
            val pdfRenderer = PdfRenderer(pfd)
            val count = minOf(pdfRenderer.pageCount, maxPages)

            for (i in 0 until count) {
                val page = pdfRenderer.openPage(i)
                // Render at 2x resolution for clean OCR of small table text
                val scale = 2
                val width = page.width * scale
                val height = page.height * scale

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                renderedBitmaps.add(bitmap)
                page.close()
            }
            pdfRenderer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                pfd.close()
            } catch (_: Exception) {}
        }

        return renderedBitmaps
    }

    private fun extractDocumentTitle(text: String, fallback: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.length in 4..80 }
        // Pick first meaningful line that looks like a notice header
        val headerCandidates = lines.filter { line ->
            line.contains("notice", ignoreCase = true) ||
            line.contains("circular", ignoreCase = true) ||
            line.contains("schedule", ignoreCase = true) ||
            line.contains("timetable", ignoreCase = true) ||
            line.contains("fee", ignoreCase = true) ||
            line.contains("exam", ignoreCase = true) ||
            line.contains("announcement", ignoreCase = true) ||
            line.contains("institute", ignoreCase = true) ||
            line.contains("university", ignoreCase = true) ||
            line.contains("iba", ignoreCase = true)
        }

        return headerCandidates.firstOrNull() ?: lines.firstOrNull() ?: fallback
    }

    suspend fun saveScannedDocumentToKhabar(
        context: Context,
        appContainer: AppContainer,
        result: ScannedDocumentResult
    ): ScannedSaveOutcome = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val docId = "scan_${now}"

        val rawMail = RawMail(
            id = docId,
            sourceId = "scanner_source",
            sender = if (result.isPdf) "Campus PDF Document" else "Campus Notice Board",
            senderEmail = "scanner@campus.local",
            subject = result.detectedTitle,
            bodyText = result.rawText,
            receivedAt = now,
            isPreviewOnly = false,
            hasAttachments = result.isPdf,
            attachmentNames = if (result.isPdf) listOf("document.pdf") else emptyList(),
            link = result.entities.link,
            originalIntentKey = null
        )

        // Process through rules engine & offline summarizer
        val savedEntity = appContainer.mailRepository.processAndSaveIncoming(rawMail)

        // If a deadline was found, auto-create a reminder
        var reminderCreated = false
        val deadlineMs = result.entities.eventStart
        if (deadlineMs != null && deadlineMs > now) {
            try {
                val reminder = com.khabar.app.data.local.entities.ReminderEntity(
                    mailId = docId,
                    title = result.detectedTitle,
                    courseCode = result.entities.courseCode,
                    dueAt = deadlineMs,
                    remindAt = maxOf(now, deadlineMs - (24 * 60 * 60 * 1000L)), // 24h before
                    done = false,
                    pinned = true,
                    createdAt = now
                )
                appContainer.reminderRepository.insertReminder(reminder)
                reminderCreated = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ScannedSaveOutcome(
            mailId = savedEntity.id,
            reminderCreated = reminderCreated,
            headline = savedEntity.headline
        )
    }
}
