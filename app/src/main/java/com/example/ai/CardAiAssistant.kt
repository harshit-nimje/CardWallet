package com.example.ai

import android.util.Log
import com.example.data.model.BusinessCard
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

data class AiMatchResult(
    val matchedCard: BusinessCard?,
    val reasoning: String,
    val conversationStarter: String,
    val keyStrengths: List<String>,
    val isLocalModel: Boolean = false
)

class CardAiAssistant {

    private val TAG = "CardAiAssistant"

    private val cloudModel by lazy {
        Firebase.ai.generativeModel("gemini-1.5-flash")
    }

    suspend fun findCardForProblem(
        query: String,
        cards: List<BusinessCard>,
        useLocalModel: Boolean = false
    ): AiMatchResult {
        if (cards.isEmpty()) {
            return AiMatchResult(
                matchedCard = null,
                reasoning = "Your wallet has no cards yet. Scan or import a business card first!",
                conversationStarter = "",
                keyStrengths = emptyList(),
                isLocalModel = useLocalModel
            )
        }

        // If user enabled the local on-device model toggle, run local inference directly
        if (useLocalModel) {
            return findWithLocalModel(query, cards)
        }

        // Cloud Gemini Flash-Lite path
        val cardsContext = cards.take(50).mapIndexed { index, c ->
            """
            Card #$index:
            ID: ${c.id}
            Name: ${c.name}
            Company: ${c.company}
            Title: ${c.title}
            Category: ${c.category}
            Phone: ${c.phone}
            Email: ${c.email}
            Website: ${c.website}
            Notes: ${c.notes}
            Tags: ${c.tags.joinToString(", ")}
            """.trimIndent()
        }.joinToString("\n---\n")

        val prompt = """
            You are an expert executive networking AI assistant for a business card wallet app.
            The user needs to find someone in their business network to solve this specific need or project:
            "$query"
            
            Here are the contacts in their wallet deck:
            $cardsContext
            
            CRITICAL RULES FOR MATCHING:
            1. RELEVANCE FIRST: Only match if the contact's company, job title, tags, or notes explicitly suggest they can help with "$query".
            2. NO HALLUCINATIONS: If NO card matches the need, you MUST return "matchedId": "" (empty string).
            3. REASONING: Explain clearly why they match OR why no match was found.
            4. PROFESSIONALISM: If a match is found, provide a friendly, tailored conversation starter.
            5. AMBIGUITY: If the query is vague, prefer no match unless a card is a very strong generalist.
            
            Return ONLY a raw JSON object with exactly these fields:
            {
              "matchedId": "string (the exact ID of the matching card, or empty string if no relevant card matches)",
              "reasoning": "string (Explain why this contact is a specific match, or explain that no relevant contact was found)",
              "conversationStarter": "string (a tailored opening line, or empty string if no match)",
              "keyStrengths": ["string", "string"] (2-3 concise strengths, or empty list if no match)
            }
        """.trimIndent()

        try {
            val response = withTimeout(35_000L) {
                cloudModel.generateContent(prompt)
            }

            val text = response.text.orEmpty().trim()
            val cleanedJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanedJson)

            val matchedId = json.optString("matchedId", "").trim()
            val reasoning = json.optString("reasoning", "")
            val conversationStarter = json.optString("conversationStarter", "")
            val strengthsArray = json.optJSONArray("keyStrengths")
            val strengths = mutableListOf<String>()
            if (strengthsArray != null) {
                for (i in 0 until strengthsArray.length()) {
                    strengths.add(strengthsArray.getString(i))
                }
            }

            val matchedCard = if (matchedId.isBlank() || matchedId.equals("none", ignoreCase = true)) null else cards.find { it.id == matchedId }

            if (matchedCard == null) {
                return AiMatchResult(
                    matchedCard = null,
                    reasoning = reasoning.ifBlank { "No relevant contact in your wallet deck matches '$query'." },
                    conversationStarter = "",
                    keyStrengths = emptyList(),
                    isLocalModel = false
                )
            }

            return AiMatchResult(
                matchedCard = matchedCard,
                reasoning = reasoning.ifBlank { "Matched based on professional alignment and skills for '$query'." },
                conversationStarter = conversationStarter.ifBlank { "Hi ${matchedCard.name.ifBlank { "there" }}, hope you're doing well!" },
                keyStrengths = strengths.ifEmpty { listOf(matchedCard.title.ifBlank { "Expert" }, matchedCard.category) },
                isLocalModel = false
            )
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Gemini call timed out, falling back to local on-device model", e)
            return findWithLocalModel(query, cards)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini search failed, falling back to local on-device model", e)
            return findWithLocalModel(query, cards)
        }
    }

    /**
     * Fast, privacy-preserving on-device local semantic model.
     * Evaluates industry taxonomy, domain synonyms, and multi-field keyword proximity.
     */
    fun findWithLocalModel(query: String, cards: List<BusinessCard>): AiMatchResult {
        val q = query.lowercase().trim()
        val queryTokens = q.split("\\s+".toRegex()).filter { it.length > 1 }

        if (cards.isEmpty()) {
            return AiMatchResult(null, "No business cards in wallet.", "", emptyList(), isLocalModel = true)
        }

        // Domain Intent Synonyms
        val legalSynonyms = listOf("lawyer", "legal", "patent", "ip", "litigation", "contract", "counsel", "attorney", "nda")
        val financeSynonyms = listOf("seed", "funding", "series a", "investor", "angel", "vc", "venture", "capital", "finance", "banking", "tax", "cfo")
        val techSynonyms = listOf("ai", "ml", "architect", "engineer", "software", "cloud", "payment", "infra", "backend", "developer", "crypto", "security")
        val designSynonyms = listOf("design", "ux", "ui", "spatial", "product design", "branding", "creative", "art", "graphic", "prototype")
        val healthcareSynonyms = listOf("health", "biotech", "doctor", "medical", "pharma", "clinical", "hospital", "therapeutics")

        var bestCard: BusinessCard? = null
        var maxScore = 0

        for (card in cards) {
            var score = 0
            val cardText = "${card.name} ${card.company} ${card.title} ${card.category} ${card.notes} ${card.tags.joinToString(" ")}".lowercase()

            // Exact token matches
            for (token in queryTokens) {
                if (cardText.contains(token)) score += 4
                if (card.title.lowercase().contains(token)) score += 8
                if (card.category.lowercase().contains(token)) score += 6
                if (card.company.lowercase().contains(token)) score += 6
                if (card.tags.any { it.lowercase().contains(token) }) score += 6
            }

            // Semantic Category Boosts
            if (legalSynonyms.any { q.contains(it) } && (card.category.equals("Legal", true) || legalSynonyms.any { cardText.contains(it) })) {
                score += 15
            }
            if (financeSynonyms.any { q.contains(it) } && (card.category.equals("Finance", true) || financeSynonyms.any { cardText.contains(it) })) {
                score += 15
            }
            if (techSynonyms.any { q.contains(it) } && (card.category.equals("Technology", true) || techSynonyms.any { cardText.contains(it) })) {
                score += 15
            }
            if (designSynonyms.any { q.contains(it) } && (card.category.equals("Design", true) || designSynonyms.any { cardText.contains(it) })) {
                score += 15
            }
            if (healthcareSynonyms.any { q.contains(it) } && (card.category.equals("Healthcare", true) || healthcareSynonyms.any { cardText.contains(it) })) {
                score += 15
            }

            if (score > maxScore) {
                maxScore = score
                bestCard = card
            }
        }

        // Require minimum score threshold (>= 4) for relevance
        if (maxScore < 4 || bestCard == null) {
            return AiMatchResult(
                matchedCard = null,
                reasoning = "No relevant contacts in your wallet deck match '$query'. Try searching by name, role, company, or industry.",
                conversationStarter = "",
                keyStrengths = emptyList(),
                isLocalModel = true
            )
        }

        val match = bestCard
        val matchName = match.name.ifBlank { "Contact" }
        val role = match.title.ifBlank { match.category }
        val company = match.company.ifBlank { "their firm" }

        val reasoning = "Matched using On-Device Local Model: ${matchName} specializes in ${role} at ${company}, directly aligning with '$query'."
        val starter = "Hi ${matchName}, hope you're having a great week! I was looking through my wallet and wanted to connect regarding ${query.take(40)}."
        val strengths = listOf(
            match.title.ifBlank { "Executive" },
            match.company.ifBlank { match.category },
            if (match.tags.isNotEmpty()) match.tags.first() else "${match.category} Expert"
        )

        return AiMatchResult(
            matchedCard = match,
            reasoning = reasoning,
            conversationStarter = starter,
            keyStrengths = strengths,
            isLocalModel = true
        )
    }
}
