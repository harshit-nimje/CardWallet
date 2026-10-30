package com.example.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.export.VCardExporter
import com.example.data.model.BusinessCard
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object CardQrGenerator {

    /**
     * Generates a high-resolution QR Bitmap from raw string content.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 640,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 2)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a QR Code for a card's vCard 3.0 data.
     * When any phone camera scans it, it immediately prompts to save to contacts.
     */
    fun generateVCardQrBitmap(
        card: BusinessCard,
        sizePx: Int = 640,
        personName: String? = null,
        personPhone: String? = null
    ): Bitmap? {
        val vcardString = VCardExporter.generateVCard(card, targetName = personName, targetPhone = personPhone)
        return generateQrBitmap(vcardString, sizePx)
    }

    /**
     * Generates a QR Code for a card's website or web portfolio.
     */
    fun generateWebsiteQrBitmap(card: BusinessCard, sizePx: Int = 640): Bitmap? {
        val url = if (card.website.isNotBlank()) {
            if (!card.website.startsWith("http")) "https://${card.website}" else card.website
        } else ""
        if (url.isBlank()) return null
        return generateQrBitmap(url, sizePx)
    }
}
