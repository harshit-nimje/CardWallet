package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import com.example.data.model.BusinessCard
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrScanResult(
    val rawText: String,
    val parsedCard: BusinessCard,
    val imageUri: String = ""
)

object CardOcrHelper {

    fun saveBitmapToStorage(context: Context, bitmap: Bitmap, prefix: String = "card"): String {
        return try {
            val dir = java.io.File(context.filesDir, "scanned_cards")
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            ""
        }
    }

    fun rotateBitmapIfNeeded(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = android.media.ExifInterface(inputStream)
            inputStream.close()
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = android.graphics.Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        Pattern.CASE_INSENSITIVE
    )

    private val phonePattern = Pattern.compile(
        "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}"
    )

    private val websitePattern = Pattern.compile(
        "(https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})|www\\.[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})|[a-zA-Z0-9.-]+\\.(?:com|ai|io|co|org|net|design|tech|app|dev))",
        Pattern.CASE_INSENSITIVE
    )

    private val titleKeywords = listOf(
        "ceo", "cto", "cfo", "coo", "cmo", "founder", "co-founder", "president",
        "vice president", "vp", "director", "head of", "manager", "lead", "engineer",
        "designer", "architect", "partner", "consultant", "developer", "specialist"
    )

    private val companyKeywords = listOf(
        "inc", "llc", "ltd", "corp", "corporation", "technologies", "labs", "capital",
        "partners", "ventures", "studio", "group", "holdings", "design", "agency"
    )

    suspend fun processImage(image: InputImage): OcrScanResult =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { it.lines.map { line -> line.text.trim() } }
                        .filter { it.isNotBlank() }
                    val rawText = visionText.text
                    val parsed = parseLinesToCard(lines, rawText)
                    continuation.resume(OcrScanResult(rawText, parsed))
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    fun parseLinesToCard(lines: List<String>, fullRawText: String): BusinessCard {
        var email = ""
        var telephone = ""
        val mobileNumbers = mutableListOf<String>()
        var website = ""
        val foundNames = mutableListOf<String>()
        var company = ""
        var title = ""
        var address = ""

        // Extract email, telephone, and mobile numbers
        val landlineStdPattern = Pattern.compile("(?:(?:\\+?\\d{1,3}[-.\\s]?)?\\(?0\\d{2,4}\\)?[-.\\s]?\\d{6,8}|\\b0\\d{2,4}[-.\\s]?\\d{6,8}\\b)")
        val generalNumberPattern = Pattern.compile("(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3,5}\\)?[-.\\s]?\\d{3,5}[-.\\s]?\\d{3,5}")

        for (line in lines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()

            // Email
            val emailMatcher = emailPattern.matcher(line)
            if (email.isBlank() && emailMatcher.find()) {
                email = emailMatcher.group().trim()
            }

            // Telephone / Landline (starts with 0 and has 8..12 digits, or has landline keywords)
            val digitsOnly = trimmed.replace("[^0-9]".toRegex(), "")
            val isLandlineKeyword = lower.contains("tel") || lower.contains("phone:") || lower.contains("landline") ||
                    lower.contains("fax") || lower.contains("off.") || lower.contains("office")
            val isLandline = (digitsOnly.startsWith("0") && digitsOnly.length in 8..12) ||
                    (isLandlineKeyword && digitsOnly.length in 6..12)

            if (telephone.isBlank() && isLandline) {
                telephone = trimmed
                continue
            }

            // Mobile numbers (9 to 13 digits, e.g. 98203 31 715 or 93222 559098 or +91...)
            if (digitsOnly.length in 9..13 && !digitsOnly.startsWith("0") && trimmed.length <= 25) {
                if (!mobileNumbers.contains(trimmed)) {
                    mobileNumbers.add(trimmed)
                }
            }
        }

        // Extract website (strictly excluding email domain like gmail.com and email lines)
        for (line in lines) {
            if (line.contains("@")) continue
            val lower = line.lowercase()
            if (lower.contains("www.") || lower.contains("http://") || lower.contains("https://") ||
                (lower.contains(".com") || lower.contains(".in") || lower.contains(".org") || lower.contains(".io") || lower.contains(".net")) &&
                !lower.contains("gmail") && !lower.contains("yahoo") && !lower.contains("hotmail") && !lower.contains("outlook")
            ) {
                val cleanUrl = line.replace("(?i)web\\.?\\s*:?".toRegex(), "")
                    .replace("(?i)website\\.?\\s*:?".toRegex(), "")
                    .replace("(?i)url\\.?\\s*:?".toRegex(), "")
                    .trim()
                if (website.isBlank() && cleanUrl.length > 4) {
                    website = cleanUrl
                }
            }
        }

        val primaryPhone = mobileNumbers.getOrNull(0) ?: ""
        val secondaryPhone = mobileNumbers.getOrNull(1) ?: ""

        // Remaining lines for name, title, company, address
        val contentLines = lines.filter { line ->
            (email.isBlank() || !line.contains(email)) &&
            (primaryPhone.isBlank() || !line.contains(primaryPhone)) &&
            (secondaryPhone.isBlank() || !line.contains(secondaryPhone)) &&
            (telephone.isBlank() || !line.contains(telephone)) &&
            (website.isBlank() || !line.contains(website)) &&
            !line.startsWith("http", ignoreCase = true) &&
            !line.startsWith("www.", ignoreCase = true) &&
            !line.startsWith("web", ignoreCase = true) &&
            !line.contains("@")
        }

        for (line in contentLines) {
            val lower = line.lowercase()
            val trimmed = line.trim()

            // Check address (contains estate, road, street, city, pin/zip code)
            if (address.isBlank() && (
                lower.contains("estate") || lower.contains("charkop") || lower.contains("road") ||
                lower.contains("mumbai") || lower.contains("street") || lower.contains("st.") ||
                lower.contains("ave") || lower.contains("suite") || lower.contains("blvd") ||
                lower.contains("floor") || lower.contains("way") || lower.contains("ind.") ||
                lower.contains("co-op") || lower.contains("lane") || lower.contains("pune") ||
                lower.contains("delhi") || lower.contains("san jose") || lower.contains("ca ")
            )) {
                address = trimmed
                continue
            }

            // Check title
            if (title.isBlank() && titleKeywords.any { lower.contains(it) }) {
                title = trimmed
                continue
            }

            // Check company (must not be a URL, website or slogan)
            if (company.isBlank() && (
                companyKeywords.any { lower.contains(it) } ||
                lower.contains("laser tech") || lower.contains("systems") ||
                lower.contains("solutions") || lower.contains("technologies") ||
                lower.contains("industries")
            ) && !lower.startsWith("web") && !lower.contains("www.")
            ) {
                company = trimmed
                continue
            }

            // Check person name (human name: 2-3 words, Title Case for every word, only letters, no digits/slogans/prepositions)
            val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val hasPrepositions = lower.contains(" in ") || lower.contains(" on ") || lower.contains(" of ") ||
                    lower.contains(" all ") || lower.contains(" kinds ") || lower.contains(" for ") || lower.contains("&")
            val isSlogan = lower.contains("marking") || lower.contains("cutting") || lower.contains("cuting") ||
                    lower.contains("everything") || lower.contains("quality") || lower.contains("service") ||
                    lower.contains("metals") || hasPrepositions

            val isProperPersonName = words.size in 2..3 &&
                    words.all { w -> w.length >= 2 && w[0].isUpperCase() && w.all { c -> c.isLetter() || c == '.' } } &&
                    !isSlogan

            if (isProperPersonName && !foundNames.contains(trimmed)) {
                foundNames.add(trimmed)
            }
        }

        // Assign primary and secondary names
        var name = foundNames.getOrNull(0) ?: ""
        var secondaryName = if (foundNames.size > 1) foundNames[1] else ""

        // Secondary fallback for company & name
        if (company.isBlank()) {
            val potentialCompany = contentLines.firstOrNull { line ->
                val l = line.lowercase()
                !l.contains("marking") && !l.contains("cutting") && !l.contains("everything") &&
                line.length in 5..40
            }
            if (potentialCompany != null) company = potentialCompany
        }

        // Fallback name if empty
        if (name.isBlank() && contentLines.isNotEmpty()) {
            name = contentLines.firstOrNull { it != company && it != address } ?: contentLines.first()
        }

        // Mobile numbers
        val secondaryPhonePerson = if (secondaryName.isNotBlank() && secondaryPhone.isNotBlank()) secondaryPhone else ""

        // Auto-assign fintech card color based on category
        val category = when {
            company.contains("Capital", ignoreCase = true) || company.contains("Finance", ignoreCase = true) -> "Finance"
            company.contains("Design", ignoreCase = true) || title.contains("Design", ignoreCase = true) -> "Design"
            company.contains("Legal", ignoreCase = true) || title.contains("Partner", ignoreCase = true) -> "Legal"
            company.contains("Health", ignoreCase = true) || company.contains("Bio", ignoreCase = true) -> "Healthcare"
            company.contains("Tech", ignoreCase = true) || title.contains("Engineer", ignoreCase = true) -> "Technology"
            else -> "Misc"
        }

        val cardColor = when (category) {
            "Finance" -> "obsidian_gold"
            "Design" -> "electric_purple"
            "Legal" -> "sunset_blaze"
            "Healthcare" -> "emerald_matrix"
            else -> "deep_stealth"
        }

        return BusinessCard(
            name = name,
            secondaryName = secondaryName,
            company = company,
            title = title,
            phone = primaryPhone,
            secondaryPhone = secondaryPhone,
            telephone = telephone,
            secondaryPhonePerson = secondaryPhonePerson,
            email = email,
            website = website,
            address = address,
            category = category,
            cardColor = cardColor,
            frontTextRaw = fullRawText
        )
    }

    /**
     * Creates a test sample business card bitmap for users who don't have a physical
     * card on hand so they can test front & back scanning and OCR extraction right away.
     */
    fun createSampleCardBitmap(isBackSide: Boolean): Bitmap {
        val width = 1050
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = if (isBackSide) Color.rgb(15, 23, 42) else Color.rgb(24, 30, 48)
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 32f, 32f, bgPaint)

        // Accent strip
        val accentPaint = Paint().apply {
            color = if (isBackSide) Color.rgb(14, 165, 233) else Color.rgb(0, 245, 212)
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(40f, 40f, 80f, height.toFloat() - 40f), 12f, 12f, accentPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }

        if (!isBackSide) {
            // Front side
            textPaint.textSize = 52f
            textPaint.isFakeBoldText = true
            canvas.drawText("JORDAN STERLING", 120f, 130f, textPaint)

            textPaint.textSize = 34f
            textPaint.isFakeBoldText = false
            textPaint.color = Color.rgb(0, 245, 212)
            canvas.drawText("Founder & Chief Architect", 120f, 185f, textPaint)

            textPaint.textSize = 40f
            textPaint.isFakeBoldText = true
            textPaint.color = Color.rgb(226, 232, 240)
            canvas.drawText("SYNAPSE QUANTUM SYSTEMS", 120f, 290f, textPaint)

            textPaint.textSize = 30f
            textPaint.isFakeBoldText = false
            textPaint.color = Color.rgb(203, 213, 225)
            canvas.drawText("Phone: +1 (555) 789-0123", 120f, 370f, textPaint)
            canvas.drawText("Email: jordan@synapsequantum.io", 120f, 420f, textPaint)
            canvas.drawText("Web: https://synapsequantum.io", 120f, 470f, textPaint)
            canvas.drawText("Office: 100 Innovation Blvd, Suite 400, Austin, TX", 120f, 520f, textPaint)
        } else {
            // Back side
            textPaint.textSize = 48f
            textPaint.isFakeBoldText = true
            textPaint.color = Color.rgb(14, 165, 233)
            canvas.drawText("NEXT-GEN QUANTUM CLOUD", 120f, 160f, textPaint)

            textPaint.textSize = 32f
            textPaint.isFakeBoldText = false
            textPaint.color = Color.rgb(226, 232, 240)
            canvas.drawText("Accelerating Discovery with Edge QPU Architectures", 120f, 220f, textPaint)

            textPaint.textSize = 28f
            textPaint.color = Color.rgb(148, 163, 184)
            canvas.drawText("• Series B Funded • 25+ Global Patents • SOC2 Certified", 120f, 320f, textPaint)
            canvas.drawText("Scan QR or connect on LinkedIn: /in/jordan-sterling", 120f, 380f, textPaint)
            canvas.drawText("Partner Inquiries: partners@synapsequantum.io", 120f, 440f, textPaint)
        }

        return bitmap
    }
}
