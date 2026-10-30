package com.example.data.web

import android.util.Log
import com.example.data.model.BusinessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object WebCardHelper {
    private const val TAG = "WebCardHelper"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun parseVCard(vCardText: String): BusinessCard {
        var name = ""
        var company = ""
        var title = ""
        var phone = ""
        var secondaryPhone = ""
        var telephone = ""
        var email = ""
        var website = ""
        var address = ""
        var notes = ""

        val lines = vCardText.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            when {
                line.startsWith("FN:", ignoreCase = true) -> {
                    name = line.substring(3).trim()
                }
                line.startsWith("N:", ignoreCase = true) && name.isBlank() -> {
                    val parts = line.substring(2).split(";")
                    name = parts.filter { it.isNotBlank() }.reversed().joinToString(" ")
                }
                line.startsWith("ORG:", ignoreCase = true) -> {
                    company = line.substring(4).replace(";", " - ").trim()
                }
                line.startsWith("TITLE:", ignoreCase = true) -> {
                    title = line.substring(6).trim()
                }
                line.contains("TEL", ignoreCase = true) -> {
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1) {
                        val number = line.substring(colonIdx + 1).trim()
                        val prefix = line.substring(0, colonIdx).uppercase()
                        if (prefix.contains("WORK") || prefix.contains("VOICE") || prefix.contains("LAND")) {
                            if (telephone.isBlank()) telephone = number else if (phone.isBlank()) phone = number
                        } else if (phone.isBlank()) {
                            phone = number
                        } else if (secondaryPhone.isBlank()) {
                            secondaryPhone = number
                        }
                    }
                }
                line.startsWith("EMAIL", ignoreCase = true) -> {
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1 && email.isBlank()) {
                        email = line.substring(colonIdx + 1).trim()
                    }
                }
                line.startsWith("URL", ignoreCase = true) -> {
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1 && website.isBlank()) {
                        website = line.substring(colonIdx + 1).trim()
                    }
                }
                line.startsWith("ADR", ignoreCase = true) -> {
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1 && address.isBlank()) {
                        val adrParts = line.substring(colonIdx + 1).split(";")
                        address = adrParts.filter { it.isNotBlank() }.joinToString(", ").trim()
                    }
                }
                line.startsWith("NOTE:", ignoreCase = true) -> {
                    notes = line.substring(5).trim()
                }
            }
        }

        val domain = extractDomain(website)
        val defaultColor = if (company.contains("Tech", true) || company.contains("AI", true)) "cyber_neon" else "obsidian_gold"

        val resolvedCompany = company.ifBlank {
            if (domain.isNotBlank()) domain.split(".").first().replaceFirstChar { it.uppercase() } else "Independent"
        }

        return BusinessCard(
            name = name.ifBlank { resolvedCompany },
            company = resolvedCompany,
            title = title.ifBlank { "Professional" },
            phone = phone,
            secondaryPhone = secondaryPhone,
            telephone = telephone,
            email = email,
            website = website,
            address = address,
            notes = notes.ifBlank { "Imported via vCard QR Scan" },
            category = if (company.contains("Design", true)) "Design" else "Technology",
            tags = emptyList(),
            cardColor = defaultColor,
            isVirtual = true,
            websiteUrl = website,
            thumbnailUrl = if (domain.isNotBlank()) "https://www.google.com/s2/favicons?domain=$domain&sz=128" else ""
        )
    }

    suspend fun fetchSiteMetadata(rawUrl: String): BusinessCard = withContext(Dispatchers.IO) {
        val targetUrl = normalizeUrl(rawUrl)
        val domain = extractDomain(targetUrl)

        var siteName = ""
        var pageTitle = ""
        var description = ""
        var ogImage = ""
        var detectedPhone = ""
        var detectedEmail = ""

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/119.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()

                // Extract exact brand/site name from og:site_name
                siteName = extractMetaContent(html, "og:site_name")
                if (siteName.isBlank()) {
                    siteName = extractMetaContent(html, "application-name")
                }

                // Extract page title
                pageTitle = extractMetaContent(html, "og:title")
                if (pageTitle.isBlank()) {
                    pageTitle = extractTagContent(html, "title")
                }

                // Extract description
                description = extractMetaContent(html, "og:description")
                if (description.isBlank()) {
                    description = extractMetaContent(html, "description")
                }

                // Extract representative image
                ogImage = extractMetaContent(html, "og:image")
                if (ogImage.isNotBlank() && !ogImage.startsWith("http")) {
                    ogImage = resolveRelativeUrl(targetUrl, ogImage)
                }

                // Extract structured JSON-LD Organization data if present
                val jsonLdMatch = Pattern.compile("""<script[^>]*?type=["']application/ld\+json["'][^>]*>(.*?)</script>""", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
                while (jsonLdMatch.find()) {
                    try {
                        val jsonStr = jsonLdMatch.group(1).orEmpty().trim()
                        if (jsonStr.startsWith("{")) {
                            val jsonObj = JSONObject(jsonStr)
                            if (siteName.isBlank()) siteName = jsonObj.optString("name", "")
                            if (detectedPhone.isBlank()) detectedPhone = jsonObj.optString("telephone", "")
                            if (detectedEmail.isBlank()) detectedEmail = jsonObj.optString("email", "")
                        }
                    } catch (_: Exception) {}
                }

                // Deep scan for telephone links (tel:+1...)
                if (detectedPhone.isBlank()) {
                    val telMatch = Pattern.compile("""href=["']tel:([^"'\s]+)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
                    if (telMatch.find()) {
                        detectedPhone = telMatch.group(1).orEmpty().replace("%20", " ")
                    }
                }

                // Deep scan for mailto links (mailto:contact@...)
                if (detectedEmail.isBlank()) {
                    val mailMatch = Pattern.compile("""href=["']mailto:([^"'\s\?]+)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
                    if (mailMatch.find()) {
                        detectedEmail = mailMatch.group(1).orEmpty()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download site HTML for $targetUrl: ${e.message}")
        }

        // Clean company name without raw domain extensions
        val finalCompany = cleanHtmlEntities(siteName).ifBlank {
            val parts = domain.split(".")
            val baseName = if (parts.size > 1 && parts.first() != "www") parts.first() else domain
            baseName.replaceFirstChar { it.uppercase() }
        }

        val cleanTitle = cleanHtmlEntities(pageTitle).ifBlank { finalCompany }
        val cleanDesc = cleanHtmlEntities(description).take(220)
        val thumbnail = if (ogImage.isNotBlank()) ogImage else "https://www.google.com/s2/favicons?domain=$domain&sz=128"

        BusinessCard(
            name = cleanTitle,
            company = finalCompany,
            title = "Digital Web Presence",
            phone = detectedPhone,
            email = detectedEmail,
            website = targetUrl,
            notes = cleanDesc.ifBlank { "Virtual card for $finalCompany ($domain)" },
            category = if (cleanTitle.contains("Design", true) || cleanDesc.contains("design", true)) "Design" else "Technology",
            tags = emptyList(),
            cardColor = "cyber_neon",
            isVirtual = true,
            websiteUrl = targetUrl,
            thumbnailUrl = thumbnail
        )
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun extractDomain(urlStr: String): String {
        return try {
            val url = URL(normalizeUrl(urlStr))
            val host = url.host ?: ""
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractMetaContent(html: String, metaName: String): String {
        val pattern = Pattern.compile(
            """<meta\s+[^>]*?(?:property|name)=["']${Pattern.quote(metaName)}["'][^>]*?content=["']([^"']*)["']""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1).orEmpty().trim()
        }
        val reversePattern = Pattern.compile(
            """<meta\s+[^>]*?content=["']([^"']*)["'][^>]*?(?:property|name)=["']${Pattern.quote(metaName)}["']""",
            Pattern.CASE_INSENSITIVE
        )
        val revMatcher = reversePattern.matcher(html)
        if (revMatcher.find()) {
            return revMatcher.group(1).orEmpty().trim()
        }
        return ""
    }

    private fun extractTagContent(html: String, tagName: String): String {
        val pattern = Pattern.compile(
            """<${Pattern.quote(tagName)}[^>]*>(.*?)</${Pattern.quote(tagName)}>""",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        val matcher = pattern.matcher(html)
        return if (matcher.find()) {
            matcher.group(1).orEmpty().trim()
        } else ""
    }

    private fun resolveRelativeUrl(baseUrl: String, relativePath: String): String {
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(relativePath).toString()
        } catch (e: Exception) {
            relativePath
        }
    }

    private fun cleanHtmlEntities(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }
}
