package com.example.data.storage

import android.content.Context
import android.util.Log
import com.example.data.model.BusinessCard
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object LocalCardStorage {
    private const val FILE_NAME = "local_wallet_cards.json"
    private const val TAG = "LocalCardStorage"

    fun loadCards(context: Context): List<BusinessCard> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val jsonString = file.readText()
            importCardsFromJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local cards", e)
            emptyList()
        }
    }

    fun saveCards(context: Context, cards: List<BusinessCard>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonString = exportCardsToJson(cards)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save local cards", e)
        }
    }

    fun exportCardsToJson(cards: List<BusinessCard>): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportTime", System.currentTimeMillis())
        val array = JSONArray()

        for (card in cards) {
            val obj = JSONObject()
            obj.put("id", card.id)
            obj.put("userId", card.userId)
            obj.put("name", card.name)
            obj.put("company", card.company)
            obj.put("title", card.title)
            obj.put("phone", card.phone)
            obj.put("secondaryPhone", card.secondaryPhone)
            obj.put("telephone", card.telephone)
            obj.put("secondaryName", card.secondaryName)
            obj.put("secondaryPhonePerson", card.secondaryPhonePerson)
            obj.put("email", card.email)
            obj.put("website", card.website)
            obj.put("address", card.address)
            obj.put("notes", card.notes)
            obj.put("category", card.category)
            obj.put("cardColor", card.cardColor)
            obj.put("frontImageUri", card.frontImageUri)
            obj.put("backImageUri", card.backImageUri)
            obj.put("frontTextRaw", card.frontTextRaw)
            obj.put("backTextRaw", card.backTextRaw)
            obj.put("isFavorite", card.isFavorite)
            obj.put("isVirtual", card.isVirtual)
            obj.put("isVertical", card.isVertical)
            obj.put("scaleMode", card.scaleMode)
            obj.put("websiteUrl", card.websiteUrl)
            obj.put("thumbnailUrl", card.thumbnailUrl)

            val tagsArray = JSONArray()
            card.tags.forEach { tagsArray.put(it) }
            obj.put("tags", tagsArray)

            array.put(obj)
        }

        root.put("cards", array)
        return root.toString(2)
    }

    fun importCardsFromJson(jsonString: String): List<BusinessCard> {
        val results = mutableListOf<BusinessCard>()
        try {
            val trimmed = jsonString.trim()
            val array = if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                root.optJSONArray("cards") ?: JSONArray()
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                return emptyList()
            }

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        tagsList.add(tagsArr.getString(t))
                    }
                }

                val card = BusinessCard(
                    id = obj.optString("id", "").ifBlank { "card_${System.currentTimeMillis()}_$i" },
                    userId = obj.optString("userId", "guest"),
                    name = obj.optString("name", ""),
                    company = obj.optString("company", ""),
                    title = obj.optString("title", ""),
                    phone = obj.optString("phone", ""),
                    secondaryPhone = obj.optString("secondaryPhone", ""),
                    telephone = obj.optString("telephone", ""),
                    secondaryName = obj.optString("secondaryName", ""),
                    secondaryPhonePerson = obj.optString("secondaryPhonePerson", ""),
                    email = obj.optString("email", ""),
                    website = obj.optString("website", ""),
                    address = obj.optString("address", ""),
                    notes = obj.optString("notes", ""),
                    category = obj.optString("category", "Technology"),
                    cardColor = obj.optString("cardColor", "obsidian_gold"),
                    frontImageUri = obj.optString("frontImageUri", ""),
                    backImageUri = obj.optString("backImageUri", ""),
                    frontTextRaw = obj.optString("frontTextRaw", ""),
                    backTextRaw = obj.optString("backTextRaw", ""),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    tags = tagsList,
                    isVirtual = obj.optBoolean("isVirtual", false),
                    isVertical = obj.optBoolean("isVertical", false),
                    scaleMode = obj.optString("scaleMode", "crop"),
                    websiteUrl = obj.optString("websiteUrl", ""),
                    thumbnailUrl = obj.optString("thumbnailUrl", "")
                )
                results.add(card)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON cards", e)
        }
        return results
    }
}
