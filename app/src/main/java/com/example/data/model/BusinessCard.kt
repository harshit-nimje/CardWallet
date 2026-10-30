package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class BusinessCard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val company: String = "",
    val title: String = "",
    val phone: String = "",
    val secondaryPhone: String = "",
    val telephone: String = "",
    val secondaryName: String = "",
    val secondaryPhonePerson: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val notes: String = "",
    val category: String = "Technology",
    val cardColor: String = "obsidian_gold",
    val frontImageUri: String = "",
    val backImageUri: String = "",
    val frontTextRaw: String = "",
    val backTextRaw: String = "",
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val isVirtual: Boolean = false,
    val isVertical: Boolean = false,
    val scaleMode: String = "crop", // "crop" or "fit"
    val websiteUrl: String = "",
    val thumbnailUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
