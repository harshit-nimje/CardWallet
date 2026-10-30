package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BusinessCard
import java.io.File
import java.io.FileWriter

object VCardExporter {

    /**
     * Converts a BusinessCard into a standard vCard 3.0 string.
     * Compatible with Apple Contacts, Google Contacts, Outlook, and Android address book.
     */
    fun generateVCard(
        card: BusinessCard,
        targetName: String? = null,
        targetPhone: String? = null
    ): String {
        val nameToUse = targetName?.takeIf { it.isNotBlank() } ?: card.name.ifBlank { card.company }
        val phoneToUse = targetPhone?.takeIf { it.isNotBlank() } ?: card.phone

        // Attempt basic name splitting for N: field
        val nameParts = nameToUse.trim().split("\\s+".toRegex())
        val (firstName, lastName) = when {
            nameParts.size >= 2 -> Pair(nameParts.first(), nameParts.drop(1).joinToString(" "))
            nameParts.isNotEmpty() -> Pair(nameParts.first(), "")
            else -> Pair("", "")
        }

        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        sb.appendLine("FN:${escapeVCard(nameToUse)}")
        sb.appendLine("N:${escapeVCard(lastName)};${escapeVCard(firstName)};;;")

        if (card.company.isNotBlank()) {
            sb.appendLine("ORG:${escapeVCard(card.company)}")
        }
        if (card.title.isNotBlank()) {
            sb.appendLine("TITLE:${escapeVCard(card.title)}")
        }
        if (phoneToUse.isNotBlank()) {
            sb.appendLine("TEL;TYPE=CELL,VOICE:${phoneToUse.trim()}")
        }
        if (card.secondaryPhone.isNotBlank() && card.secondaryPhone != phoneToUse) {
            sb.appendLine("TEL;TYPE=WORK,VOICE:${card.secondaryPhone.trim()}")
        }
        if (card.email.isNotBlank()) {
            sb.appendLine("EMAIL;TYPE=INTERNET,PREF:${card.email.trim()}")
        }
        if (card.website.isNotBlank()) {
            val web = if (!card.website.startsWith("http")) "https://${card.website.trim()}" else card.website.trim()
            sb.appendLine("URL:$web")
        }
        if (card.address.isNotBlank()) {
            sb.appendLine("ADR;TYPE=WORK:;;${escapeVCard(card.address.trim())};;;;")
        }
        if (card.notes.isNotBlank()) {
            sb.appendLine("NOTE:${escapeVCard(card.notes)}")
        }
        if (card.category.isNotBlank()) {
            sb.appendLine("CATEGORIES:${escapeVCard(card.category)}")
        }
        sb.appendLine("END:VCARD")
        return sb.toString()
    }

    /**
     * Converts a collection of cards into a single multi-contact vCard (.vcf) string.
     */
    fun generateMultiVCard(cards: List<BusinessCard>): String {
        return buildString {
            cards.forEach { card ->
                append(generateVCard(card))
                appendLine()
                // If there's a secondary individual on the card, also emit their contact record
                if (card.secondaryName.isNotBlank() && card.secondaryPhone.isNotBlank()) {
                    append(generateVCard(card, targetName = card.secondaryName, targetPhone = card.secondaryPhone))
                    appendLine()
                }
            }
        }
    }

    /**
     * Converts a list of cards into standard RFC-4180 CSV format.
     */
    fun generateCsv(cards: List<BusinessCard>): String {
        val sb = StringBuilder()
        // Header
        sb.appendLine("Name,Company,Title,Phone,Secondary Name,Secondary Phone,Email,Website,Address,Category,Tags,Notes,Is Virtual")
        cards.forEach { card ->
            val row = listOf(
                escapeCsv(card.name),
                escapeCsv(card.company),
                escapeCsv(card.title),
                escapeCsv(card.phone),
                escapeCsv(card.secondaryName),
                escapeCsv(card.secondaryPhone),
                escapeCsv(card.email),
                escapeCsv(card.website),
                escapeCsv(card.address),
                escapeCsv(card.category),
                escapeCsv(card.tags.joinToString("; ")),
                escapeCsv(card.notes),
                if (card.isVirtual) "Yes" else "No"
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    /**
     * Writes cards to a temporary .vcf file and presents the Android Share Sheet.
     */
    fun shareCardsAsVcf(context: Context, cards: List<BusinessCard>, fileName: String = "wallet_contacts.vcf") {
        if (cards.isEmpty()) return
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, fileName)
            FileWriter(file).use { writer ->
                writer.write(generateMultiVCard(cards))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Business Cards (${cards.size})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Contacts (.vcf)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Writes cards to a temporary .csv file and presents the Android Share Sheet.
     */
    fun shareCardsAsCsv(context: Context, cards: List<BusinessCard>, fileName: String = "wallet_cards_export.csv") {
        if (cards.isEmpty()) return
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, fileName)
            FileWriter(file).use { writer ->
                writer.write(generateCsv(cards))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Business Cards Export (${cards.size})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Cards (.csv)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun escapeVCard(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
