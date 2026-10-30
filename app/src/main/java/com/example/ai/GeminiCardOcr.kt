package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.BusinessCard
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * Intelligent OCR extractor powered by Gemini 3.5 Flash-Lite via Firebase AI Logic.
 * Performs multimodal parsing to extract clean, typo-corrected business card fields
 * directly into structured JSON.
 */
class GeminiCardOcr {

    private val TAG = "GeminiCardOcr"

    private val cardSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Primary full name of the first person/owner"),
            "secondary_name" to Schema.string(description = "Second person/partner name if card lists two people (e.g. Nilesh Kamdar)"),
            "title" to Schema.string(description = "Job title, professional role or designation"),
            "company" to Schema.string(description = "Company, organization, firm, or agency name"),
            "phone" to Schema.string(description = "Primary mobile phone number"),
            "secondary_phone" to Schema.string(description = "Secondary mobile number for second person or alternate mobile"),
            "telephone" to Schema.string(description = "Landline / office telephone number (e.g. 022 2868871)"),
            "email" to Schema.string(description = "Email address"),
            "website" to Schema.string(description = "Website URL or domain"),
            "address" to Schema.string(description = "Physical office address, suite, city, state, or country"),
            "category" to Schema.string(description = "Industry category: Technology, Finance, Design, Healthcare, Legal, or Other"),
            "notes" to Schema.string(description = "Slogan, services, or secondary details on card")
        ),
        optionalProperties = listOf("secondary_name", "title", "company", "phone", "secondary_phone", "telephone", "email", "website", "address", "notes")
    )

    private val modelConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = cardSchema
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = modelConfig
        )
    }

    suspend fun extractCardDetails(
        bitmap: Bitmap,
        isBackSide: Boolean,
        rawOcrHint: String = ""
    ): BusinessCard? {
        val prompt = if (isBackSide) {
            """
            This image is the REVERSE (back) side of a physical business card.
            Extract any company services, certifications, slogans, secondary phone/fax numbers, website links, or office locations.
            Raw detected OCR text:
            $rawOcrHint
            """.trimIndent()
        } else {
            """
            You are an expert OCR vision specialist extracting business card information from this card image.
            IMPORTANT PARSING RULES:
            1. 'name': The first human person's name (e.g., 'Ketan Shah'). NEVER put slogans or company names here.
            2. 'secondary_name': If there is a second person / partner on the card (e.g., 'Nilesh Kamdar'), extract them here.
            3. 'company': The official business / company name (e.g., 'Aditi Laser Tech'). DO NOT put website URLs or email addresses.
            4. 'title': Professional role (e.g., 'Director', 'Founder', 'Partner').
            5. 'phone': The primary mobile phone number (e.g., '98203 31 715').
            6. 'secondary_phone': If a second mobile number is present, put it here (e.g., '93222 559098').
            7. 'telephone': If a landline / office phone with area code is present (e.g., '022 2868871'), put it in 'telephone'.
            8. 'email': The valid email address (e.g., 'amcmark@gmail.com').
            9. 'website': The company website domain or URL (e.g., 'www.aditilasergroup.in'). NEVER set 'gmail.com' as website.
            10. 'address': The complete physical premises / industrial estate / street / city / postal code address.
            11. 'notes': Summarize company offerings or taglines (e.g., 'Laser marking & cutting on all kinds of metals & non metals').
            12. 'category': Industry category (Technology, Finance, Design, Healthcare, Legal, Manufacturing, or Other).

            Raw detected OCR text hints:
            $rawOcrHint
            """.trimIndent()
        }

        return try {
            val response = withTimeout(35_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }

            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                Log.w(TAG, "Gemini Flash-Lite returned empty output")
                return null
            }

            val json = JSONObject(jsonString)
            val name = json.optString("name", "").trim()
            val secondaryName = json.optString("secondary_name", "").trim()
            val title = json.optString("title", "").trim()
            val company = json.optString("company", "").trim()
            val phone = json.optString("phone", "").trim()
            val secondaryPhone = json.optString("secondary_phone", "").trim()
            val telephone = json.optString("telephone", "").trim()
            val email = json.optString("email", "").trim()
            val website = json.optString("website", "").trim()
            val address = json.optString("address", "").trim()
            val notes = json.optString("notes", "").trim()
            val category = json.optString("category", "Misc").trim()
            val resolvedCategory = if (category.isNotBlank() && category.lowercase() != "other") category else "Misc"

            val cardColor = when (resolvedCategory.lowercase()) {
                "finance" -> "obsidian_gold"
                "design" -> "electric_purple"
                "legal" -> "sunset_blaze"
                "healthcare" -> "emerald_matrix"
                else -> "deep_stealth"
            }

            BusinessCard(
                name = name,
                secondaryName = secondaryName,
                company = company,
                title = title,
                phone = phone,
                secondaryPhone = secondaryPhone,
                telephone = telephone,
                secondaryPhonePerson = if (secondaryName.isNotBlank() && secondaryPhone.isNotBlank()) secondaryPhone else "",
                email = email,
                website = website,
                address = address,
                category = resolvedCategory,
                cardColor = cardColor,
                notes = notes,
                tags = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini OCR extraction failed: ${e.message}")
            null
        }
    }
}
