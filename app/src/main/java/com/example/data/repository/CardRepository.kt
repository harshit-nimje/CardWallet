package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.BusinessCard
import com.example.data.storage.LocalCardStorage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class CardRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val roomDb: com.example.data.local.AppDatabase
) {
    constructor(context: Context) : this(
        context,
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        androidx.room.Room.databaseBuilder(
            context.applicationContext,
            com.example.data.local.AppDatabase::class.java,
            "card-db"
        ).build()
    )

    private val auth = Firebase.auth
    private val TAG = "CardRepository"

    private fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    private val localUpdateTrigger = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }

    private val isGuest: Boolean
        get() {
            val prefs = context.getSharedPreferences("cardwallet_prefs", Context.MODE_PRIVATE)
            return auth.currentUser == null || prefs.getBoolean("is_guest_mode", false)
        }

    fun observeCards(): Flow<List<BusinessCard>> = callbackFlow {
        if (isGuest) {
            val job = CoroutineScope(Dispatchers.IO).launch {
                localUpdateTrigger.collect {
                    val cards = LocalCardStorage.loadCards(context)
                    trySend(cards)
                }
            }
            awaitClose { job.cancel() }
        } else {
            val uid = currentUserId() ?: "default"
            val path = "users/$uid/cards"
            val query = db.collection("users").document(uid).collection("cards")
            val subscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val cards = snapshot.toObjects(BusinessCard::class.java)
                    // Also cache locally for offline/export
                    CoroutineScope(Dispatchers.IO).launch {
                        roomDb.cardDao().insertCards(cards.map { com.example.data.local.LocalBusinessCard.fromBusinessCard(it) })
                    }
                    trySend(cards)
                }
            }
            awaitClose { subscription.remove() }
        }
    }

    private fun notifyLocalCardsUpdated() {
        localUpdateTrigger.tryEmit(Unit)
        val prefs = context.getSharedPreferences("cardwallet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("local_cards_updated_trigger", System.currentTimeMillis()).apply()
    }

    suspend fun addCard(card: BusinessCard): String {
        if (isGuest) {
            val current = LocalCardStorage.loadCards(context).toMutableList()
            val newId = if (card.id.isNotBlank()) card.id else "card_${System.currentTimeMillis()}"
            val newCard = card.copy(id = newId, userId = "guest")
            current.removeAll { it.id == newId }
            current.add(0, newCard)
            LocalCardStorage.saveCards(context, current)
            notifyLocalCardsUpdated()
            return newId
        }

        val uid = currentUserId() ?: throw IllegalStateException("User must be signed in with Google")
        val colRef = db.collection("users").document(uid).collection("cards")
        val docRef = if (card.id.isNotBlank()) colRef.document(card.id) else colRef.document()
        val cardId = docRef.id

        val payload = createCardPayload(card, uid, isCreate = true)

        try {
            docRef.set(payload).await()
            // Keep local storage in sync as well
            val current = LocalCardStorage.loadCards(context).toMutableList()
            current.removeAll { it.id == cardId }
            current.add(0, card.copy(id = cardId, userId = uid))
            LocalCardStorage.saveCards(context, current)
            notifyLocalCardsUpdated()
            return cardId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCard(card: BusinessCard) {
        // Always update local cache immediately
        val current = LocalCardStorage.loadCards(context).toMutableList()
        val index = current.indexOfFirst { it.id == card.id }
        if (index != -1) {
            current[index] = card
        } else {
            current.add(0, card)
        }
        LocalCardStorage.saveCards(context, current)
        notifyLocalCardsUpdated()

        if (isGuest) return

        val uid = currentUserId() ?: throw IllegalStateException("User must be signed in with Google")
        require(card.id.isNotBlank()) { "Card ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("cards").document(card.id)

        val payload = createCardPayload(card, uid, isCreate = false)

        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFavorite(cardId: String, currentFavorite: Boolean) {
        val targetFavorite = !currentFavorite

        // 1. Always update local storage first so offline/guest is instant
        val current = LocalCardStorage.loadCards(context).toMutableList()
        val index = current.indexOfFirst { it.id == cardId }
        if (index != -1) {
            current[index] = current[index].copy(isFavorite = targetFavorite)
            LocalCardStorage.saveCards(context, current)
            notifyLocalCardsUpdated()
        }

        if (isGuest) return

        val uid = currentUserId() ?: return
        val docRef = db.collection("users").document(uid).collection("cards").document(cardId)
        try {
            docRef.update(
                mapOf(
                    "isFavorite" to targetFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCard(cardId: String) {
        if (isGuest) {
            val current = LocalCardStorage.loadCards(context).toMutableList()
            current.removeAll { it.id == cardId }
            LocalCardStorage.saveCards(context, current)
            notifyLocalCardsUpdated()
            return
        }

        val uid = currentUserId() ?: return
        val docRef = db.collection("users").document(uid).collection("cards").document(cardId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    private fun createCardPayload(card: BusinessCard, uid: String, isCreate: Boolean): HashMap<String, Any?> {
        val payload = hashMapOf<String, Any?>(
            "userId" to uid,
            "name" to card.name,
            "company" to card.company,
            "title" to card.title,
            "phone" to card.phone,
            "secondaryPhone" to card.secondaryPhone,
            "telephone" to card.telephone,
            "secondaryName" to card.secondaryName,
            "secondaryPhonePerson" to card.secondaryPhonePerson,
            "email" to card.email,
            "website" to card.website,
            "address" to card.address,
            "notes" to card.notes,
            "category" to card.category,
            "cardColor" to card.cardColor,
            "frontImageUri" to card.frontImageUri,
            "backImageUri" to card.backImageUri,
            "frontTextRaw" to card.frontTextRaw,
            "backTextRaw" to card.backTextRaw,
            "isFavorite" to card.isFavorite,
            "tags" to card.tags,
            "isVertical" to card.isVertical,
            "scaleMode" to card.scaleMode,
            "isVirtual" to card.isVirtual,
            "websiteUrl" to card.websiteUrl,
            "thumbnailUrl" to card.thumbnailUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }
        return payload
    }

    fun exportWalletJson(): String {
        val cards = LocalCardStorage.loadCards(context)
        return LocalCardStorage.exportCardsToJson(cards)
    }

    suspend fun importWalletJson(jsonString: String): Int {
        val imported = LocalCardStorage.importCardsFromJson(jsonString)
        if (imported.isEmpty()) return 0

        var count = 0
        for (card in imported) {
            try {
                addCard(card)
                count++
            } catch (e: Exception) {
                Log.e(TAG, "Error importing card ${card.name}", e)
            }
        }
        return count
    }

    suspend fun populateStarterCardsIfEmpty(cardsCount: Int) {
        // No-op: Only user-added cards allowed
    }
}
