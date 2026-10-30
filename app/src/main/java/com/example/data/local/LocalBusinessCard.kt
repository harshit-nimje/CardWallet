package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BusinessCard

@Entity(tableName = "business_cards")
data class LocalBusinessCard(
    @PrimaryKey val id: String,
    val name: String,
    val company: String,
    val title: String,
    val phone: String,
    val email: String,
    val website: String,
    val address: String,
    val notes: String,
    val category: String,
    val cardColor: String,
    val frontImageUri: String,
    val isFavorite: Boolean,
    val isVirtual: Boolean,
    val isVertical: Boolean
) {
    fun toBusinessCard(): BusinessCard {
        return BusinessCard(
            id = id,
            name = name,
            company = company,
            title = title,
            phone = phone,
            email = email,
            website = website,
            address = address,
            notes = notes,
            category = category,
            cardColor = cardColor,
            frontImageUri = frontImageUri,
            isFavorite = isFavorite,
            isVirtual = isVirtual,
            isVertical = isVertical
        )
    }

    companion object {
        fun fromBusinessCard(card: BusinessCard): LocalBusinessCard {
            return LocalBusinessCard(
                id = card.id,
                name = card.name,
                company = card.company,
                title = card.title,
                phone = card.phone,
                email = card.email,
                website = card.website,
                address = card.address,
                notes = card.notes,
                category = card.category,
                cardColor = card.cardColor,
                frontImageUri = card.frontImageUri,
                isFavorite = card.isFavorite,
                isVirtual = card.isVirtual,
                isVertical = card.isVertical
            )
        }
    }
}
