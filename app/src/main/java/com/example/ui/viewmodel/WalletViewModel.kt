package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiMatchResult
import com.example.ai.CardAiAssistant
import com.example.ai.GeminiCardOcr
import com.example.data.contacts.DeviceContact
import com.example.data.contacts.GoogleContactsHelper
import com.example.data.model.BusinessCard
import com.example.data.ocr.CardOcrHelper
import com.example.data.repository.CardRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CardsUiState {
    data object Loading : CardsUiState
    data class Success(val cards: List<BusinessCard>) : CardsUiState
    data class Error(val message: String) : CardsUiState
}

sealed interface AiFinderUiState {
    data object Idle : AiFinderUiState
    data object Searching : AiFinderUiState
    data class Result(val match: AiMatchResult) : AiFinderUiState
    data class Error(val message: String) : AiFinderUiState
}

enum class CardSortOrder {
    RECENT,
    NAME_ASC,
    COMPANY_ASC
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)
    private val aiAssistant = CardAiAssistant()
    private val geminiOcr = GeminiCardOcr()

    private val prefs = application.getSharedPreferences("cardwallet_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    fun continueAsGuest() {
        prefs.edit().putBoolean("is_guest_mode", true).apply()
        _isGuestMode.value = true
    }

    fun exitGuestMode() {
        prefs.edit().putBoolean("is_guest_mode", false).apply()
        _isGuestMode.value = false
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(CardSortOrder.RECENT)
    val sortOrder: StateFlow<CardSortOrder> = _sortOrder.asStateFlow()

    // Filter by type: ALL, PHYSICAL, VIRTUAL
    private val _cardTypeFilter = MutableStateFlow("ALL")
    val cardTypeFilter: StateFlow<String> = _cardTypeFilter.asStateFlow()

    fun setCardTypeFilter(type: String) {
        _cardTypeFilter.value = type
    }

    private val _rawCards = MutableStateFlow<List<BusinessCard>>(emptyList())

    val filteredCards: StateFlow<List<BusinessCard>> = combine(
        _rawCards,
        _searchQuery,
        _selectedCategory,
        _sortOrder,
        _cardTypeFilter
    ) { cards, query, category, sort, typeFilter ->
        val filtered = cards.filter { card ->
            val matchesCategory = (category == "All" || card.category.equals(category, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    card.name.contains(query, ignoreCase = true) ||
                    card.company.contains(query, ignoreCase = true) ||
                    card.title.contains(query, ignoreCase = true) ||
                    card.notes.contains(query, ignoreCase = true) ||
                    card.tags.any { it.contains(query, ignoreCase = true) }
            val matchesType = when (typeFilter) {
                "PHYSICAL" -> !card.isVirtual && !card.tags.any { it.equals("virtual", ignoreCase = true) }
                "VIRTUAL" -> card.isVirtual || card.tags.any { it.equals("virtual", ignoreCase = true) } || card.websiteUrl.isNotBlank()
                else -> true
            }
            matchesCategory && matchesQuery && matchesType
        }

        when (sort) {
            CardSortOrder.RECENT -> filtered.sortedByDescending { it.createdAt }
            CardSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            CardSortOrder.COMPANY_ASC -> filtered.sortedBy { it.company.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Persistent settings
    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("pref_haptics", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _cardTilt3dEnabled = MutableStateFlow(prefs.getBoolean("pref_tilt3d", true))
    val cardTilt3dEnabled: StateFlow<Boolean> = _cardTilt3dEnabled.asStateFlow()

    private val _autoGeminiOcr = MutableStateFlow(prefs.getBoolean("pref_gemini_ocr", true))
    val autoGeminiOcr: StateFlow<Boolean> = _autoGeminiOcr.asStateFlow()

    private val _compactStackView = MutableStateFlow(prefs.getBoolean("pref_compact_stack", false))
    val compactStackView: StateFlow<Boolean> = _compactStackView.asStateFlow()

    private val _useMaterialDynamicTheme = MutableStateFlow(prefs.getBoolean("pref_material_theme", false))
    val useMaterialDynamicTheme: StateFlow<Boolean> = _useMaterialDynamicTheme.asStateFlow()

    private val _showDistanceEta = MutableStateFlow(prefs.getBoolean("pref_distance_eta", true))
    val showDistanceEta: StateFlow<Boolean> = _showDistanceEta.asStateFlow()

    fun setShowDistanceEta(enabled: Boolean) {
        _showDistanceEta.value = enabled
        prefs.edit().putBoolean("pref_distance_eta", enabled).apply()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs.edit().putBoolean("pref_haptics", enabled).apply()
    }

    fun setCardTilt3dEnabled(enabled: Boolean) {
        _cardTilt3dEnabled.value = enabled
        prefs.edit().putBoolean("pref_tilt3d", enabled).apply()
    }

    fun setAutoGeminiOcr(enabled: Boolean) {
        _autoGeminiOcr.value = enabled
        prefs.edit().putBoolean("pref_gemini_ocr", enabled).apply()
    }

    fun setCompactStackView(enabled: Boolean) {
        _compactStackView.value = enabled
        prefs.edit().putBoolean("pref_compact_stack", enabled).apply()
    }

    fun setUseMaterialDynamicTheme(enabled: Boolean) {
        _useMaterialDynamicTheme.value = enabled
        prefs.edit().putBoolean("pref_material_theme", enabled).apply()
    }

    private val _selectedPaletteTheme = MutableStateFlow(prefs.getString("pref_palette_theme", "DEFAULT") ?: "DEFAULT")
    val selectedPaletteTheme: StateFlow<String> = _selectedPaletteTheme.asStateFlow()

    fun setSelectedPaletteTheme(theme: String) {
        _selectedPaletteTheme.value = theme
        prefs.edit().putString("pref_palette_theme", theme).apply()
    }

    // On-device local AI model vs Cloud Gemini toggle
    private val _useLocalAiModel = MutableStateFlow(prefs.getBoolean("pref_local_ai_model", false))
    val useLocalAiModel: StateFlow<Boolean> = _useLocalAiModel.asStateFlow()

    fun setUseLocalAiModel(enabled: Boolean) {
        _useLocalAiModel.value = enabled
        prefs.edit().putBoolean("pref_local_ai_model", enabled).apply()
    }

    // Duplicate Card Detection
    data class DuplicateDetection(
        val incomingCard: BusinessCard,
        val existingCard: BusinessCard,
        val matchReason: String
    )

    private val _duplicateDetection = MutableStateFlow<DuplicateDetection?>(null)
    val duplicateDetection: StateFlow<DuplicateDetection?> = _duplicateDetection.asStateFlow()

    fun dismissDuplicatePrompt() {
        _duplicateDetection.value = null
    }

    // Batch Selection Mode
    private val _selectedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCardIds: StateFlow<Set<String>> = _selectedCardIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun startSelectionMode(cardId: String) {
        _isSelectionMode.value = true
        _selectedCardIds.value = setOf(cardId)
    }

    fun toggleCardSelection(cardId: String) {
        val current = _selectedCardIds.value.toMutableSet()
        if (current.contains(cardId)) {
            current.remove(cardId)
            if (current.isEmpty()) {
                _isSelectionMode.value = false
            }
        } else {
            current.add(cardId)
            _isSelectionMode.value = true
        }
        _selectedCardIds.value = current
    }

    fun selectAllCards(cards: List<BusinessCard>) {
        _isSelectionMode.value = true
        _selectedCardIds.value = cards.map { it.id }.toSet()
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedCardIds.value = emptySet()
    }

    fun deleteSelectedCards() {
        val idsToDelete = _selectedCardIds.value.toList()
        clearSelection()
        viewModelScope.launch {
            for (id in idsToDelete) {
                try {
                    repository.deleteCard(id)
                } catch (e: Exception) {
                    Log.e("WalletVM", "Error deleting card $id", e)
                }
            }
        }
    }

    fun addTagToSelectedCards(tag: String) {
        if (tag.isBlank()) return
        val selectedIds = _selectedCardIds.value
        val cardsToUpdate = _rawCards.value.filter { it.id in selectedIds }
        clearSelection()
        viewModelScope.launch {
            for (card in cardsToUpdate) {
                if (!card.tags.contains(tag)) {
                    val updated = card.copy(tags = card.tags + tag)
                    try {
                        repository.updateCard(updated)
                    } catch (e: Exception) {
                        Log.e("WalletVM", "Error updating tags for card ${card.id}", e)
                    }
                }
            }
        }
    }

    val cardsUiState: StateFlow<CardsUiState> = repository.observeCards()
        .map<List<BusinessCard>, CardsUiState> { rawList ->
            val context = getApplication<android.app.Application>()
            val processedCards = rawList.map { card ->
                if (!card.isVertical && card.frontImageUri.isNotBlank()) {
                    val isImageVertical = com.example.data.storage.CardImageHelper.isImageVertical(context, card.frontImageUri)
                    if (isImageVertical == true) {
                        card.copy(isVertical = true)
                    } else card
                } else card
            }
            _rawCards.value = processedCards
            CardsUiState.Success(processedCards)
        }
        .catch { error ->
            Log.e("WalletVM", "Failed to observe cards", error)
            emit(CardsUiState.Error(error.localizedMessage ?: "Failed to sync cards"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CardsUiState.Loading
        )

    // AI Finder state
    private val _aiFinderState = MutableStateFlow<AiFinderUiState>(AiFinderUiState.Idle)
    val aiFinderState: StateFlow<AiFinderUiState> = _aiFinderState.asStateFlow()

    // Scanner state
    private val _scanFrontImage = MutableStateFlow<Bitmap?>(null)
    val scanFrontImage: StateFlow<Bitmap?> = _scanFrontImage.asStateFlow()

    private val _scanBackImage = MutableStateFlow<Bitmap?>(null)
    val scanBackImage: StateFlow<Bitmap?> = _scanBackImage.asStateFlow()

    private val _draftCard = MutableStateFlow<BusinessCard?>(null)
    val draftCard: StateFlow<BusinessCard?> = _draftCard.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _ocrEngineStatus = MutableStateFlow("Ready")
    val ocrEngineStatus: StateFlow<String> = _ocrEngineStatus.asStateFlow()

    // Google Contacts state
    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    init {
        Firebase.auth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSortOrderChanged(order: CardSortOrder) {
        _sortOrder.value = order
    }


    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCard(cardId)
            } catch (e: Exception) {
                Log.e("WalletVM", "Failed to delete card", e)
            }
        }
    }

    fun checkDuplicate(card: BusinessCard): Pair<BusinessCard, String>? {
        val cleanCardPhone = card.phone.filter { it.isDigit() }
        val cleanEmail = card.email.trim().lowercase()
        val cleanName = card.name.trim().lowercase()
        val cleanCompany = card.company.trim().lowercase()

        for (existing in _rawCards.value) {
            if (existing.id == card.id) continue
            val existingPhone = existing.phone.filter { it.isDigit() }
            val existingEmail = existing.email.trim().lowercase()
            val existingName = existing.name.trim().lowercase()
            val existingCompany = existing.company.trim().lowercase()

            if (cleanCardPhone.length >= 7 && cleanCardPhone == existingPhone) {
                return Pair(existing, "Matching phone number (${existing.phone})")
            }
            if (cleanEmail.isNotBlank() && cleanEmail == existingEmail) {
                return Pair(existing, "Matching email address (${existing.email})")
            }
            if (cleanName.isNotBlank() && cleanCompany.isNotBlank() &&
                cleanName == existingName && cleanCompany == existingCompany) {
                return Pair(existing, "Identical name & company (${existing.name} at ${existing.company})")
            }
        }
        return null
    }

    fun saveDraftCard(card: BusinessCard, force: Boolean = false, onSuccess: () -> Unit) {
        if (!force && card.id.isBlank()) {
            val duplicate = checkDuplicate(card)
            if (duplicate != null) {
                _duplicateDetection.value = DuplicateDetection(
                    incomingCard = card,
                    existingCard = duplicate.first,
                    matchReason = duplicate.second
                )
                return
            }
        }

        val colorToUse = if (card.cardColor.isNotBlank() && card.cardColor != "default" && card.cardColor != "cyber_neon") {
            card.cardColor
        } else {
            listOf("obsidian_gold", "electric_purple", "sunset_blaze", "deep_stealth", "emerald_matrix", "peach_sunset").random()
        }
        val cardWithColor = card.copy(cardColor = colorToUse)

        viewModelScope.launch {
            try {
                if (cardWithColor.id.isNotBlank()) {
                    repository.updateCard(cardWithColor)
                } else {
                    repository.addCard(cardWithColor)
                }
                clearScanner()
                _duplicateDetection.value = null
                onSuccess()
            } catch (e: Exception) {
                Log.e("WalletVM", "Failed to save draft card", e)
            }
        }
    }

    fun updateExistingWithDraft(existing: BusinessCard, draft: BusinessCard, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val merged = existing.copy(
                    title = draft.title.ifBlank { existing.title },
                    phone = draft.phone.ifBlank { existing.phone },
                    secondaryPhone = draft.secondaryPhone.ifBlank { existing.secondaryPhone },
                    telephone = draft.telephone.ifBlank { existing.telephone },
                    email = draft.email.ifBlank { existing.email },
                    website = draft.website.ifBlank { existing.website },
                    address = draft.address.ifBlank { existing.address },
                    notes = if (draft.notes.isNotBlank() && !existing.notes.contains(draft.notes)) {
                        "${existing.notes}\n${draft.notes}".trim()
                    } else existing.notes,
                    tags = (existing.tags + draft.tags).distinct(),
                    isVertical = draft.isVertical,
                    scaleMode = draft.scaleMode,
                    frontImageUri = draft.frontImageUri.ifBlank { existing.frontImageUri },
                    backImageUri = draft.backImageUri.ifBlank { existing.backImageUri }
                )
                repository.updateCard(merged)
                clearScanner()
                _duplicateDetection.value = null
                onSuccess()
            } catch (e: Exception) {
                Log.e("WalletVM", "Error updating existing card", e)
            }
        }
    }

    fun deduplicateEntireWallet(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val cards = _rawCards.value
            val seenKeys = mutableSetOf<String>()
            val idsToDelete = mutableListOf<String>()

            for (card in cards) {
                val cleanPhone = card.phone.filter { it.isDigit() }
                val cleanEmail = card.email.trim().lowercase()
                val nameCompany = "${card.name.trim().lowercase()}_${card.company.trim().lowercase()}"

                val key = when {
                    cleanPhone.length >= 7 -> "phone_$cleanPhone"
                    cleanEmail.isNotBlank() -> "email_$cleanEmail"
                    card.name.isNotBlank() && card.company.isNotBlank() -> "nc_$nameCompany"
                    else -> "id_${card.id}"
                }

                if (seenKeys.contains(key)) {
                    idsToDelete.add(card.id)
                } else {
                    seenKeys.add(key)
                }
            }

            for (id in idsToDelete) {
                try {
                    repository.deleteCard(id)
                } catch (e: Exception) {
                    Log.e("WalletVM", "Error deleting duplicate card $id", e)
                }
            }
            onResult(idsToDelete.size)
        }
    }

    // OCR Scanning with Gemini Flash-Lite & ML Kit fallback
    fun processScanBitmap(bitmap: Bitmap, isBack: Boolean) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            _ocrEngineStatus.value = "Analyzing with Gemini Flash-Lite..."
            try {
                val imageUri = CardOcrHelper.saveBitmapToStorage(
                    context = getApplication(),
                    bitmap = bitmap,
                    prefix = if (isBack) "card_back" else "card_front"
                )

                // 1. Fast local OCR extraction with ML Kit for initial text structure
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val localResult = try {
                    CardOcrHelper.processImage(inputImage)
                } catch (e: Exception) {
                    null
                }

                val rawDetectedText = localResult?.rawText.orEmpty()

                // 2. Multimodal intelligence pass with Gemini 3.5 Flash-Lite
                val aiExtractedCard = geminiOcr.extractCardDetails(
                    bitmap = bitmap,
                    isBackSide = isBack,
                    rawOcrHint = rawDetectedText
                )

                val isVertical = bitmap.height > bitmap.width
                val effectiveCard = if (aiExtractedCard != null && (aiExtractedCard.name.isNotBlank() || aiExtractedCard.company.isNotBlank())) {
                    _ocrEngineStatus.value = "Extracted with Gemini 3.5 Flash-Lite"
                    aiExtractedCard.copy(isVertical = isVertical)
                } else {
                    _ocrEngineStatus.value = "Extracted with ML Kit On-Device"
                    (localResult?.parsedCard ?: BusinessCard()).copy(isVertical = isVertical)
                }

                if (isBack) {
                    _scanBackImage.value = bitmap
                    val current = _draftCard.value ?: effectiveCard
                    _draftCard.value = current.copy(
                        backImageUri = imageUri,
                        backTextRaw = rawDetectedText,
                        isVertical = isVertical, // Ensure back side detection also applies
                        website = current.website.ifBlank { effectiveCard.website },
                        address = current.address.ifBlank { effectiveCard.address },
                        notes = if (current.notes.isBlank()) effectiveCard.notes.ifBlank { rawDetectedText.take(200) } else current.notes
                    )
                } else {
                    _scanFrontImage.value = bitmap
                    val current = _draftCard.value
                    _draftCard.value = if (current == null) {
                        effectiveCard.copy(frontImageUri = imageUri, frontTextRaw = rawDetectedText, isVertical = isVertical)
                    } else {
                        effectiveCard.copy(
                            frontImageUri = imageUri,
                            frontTextRaw = rawDetectedText,
                            backImageUri = current.backImageUri,
                            backTextRaw = current.backTextRaw,
                            isVertical = isVertical,
                            notes = current.notes.ifBlank { effectiveCard.notes }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("WalletVM", "OCR scan error", e)
                _ocrEngineStatus.value = "OCR processing error"
            } finally {
                _isOcrProcessing.value = false
            }
        }
    }



    fun updateDraftCard(card: BusinessCard) {
        _draftCard.value = card
    }

    fun clearScanner() {
        _scanFrontImage.value = null
        _scanBackImage.value = null
        _draftCard.value = null
        _isOcrProcessing.value = false
        _ocrEngineStatus.value = "Ready"
    }

    // AI Finder search
    fun searchWithAi(problemQuery: String) {
        if (problemQuery.isBlank()) return
        viewModelScope.launch {
            _aiFinderState.value = AiFinderUiState.Searching
            try {
                val match = aiAssistant.findCardForProblem(
                    query = problemQuery,
                    cards = _rawCards.value,
                    useLocalModel = _useLocalAiModel.value
                )
                _aiFinderState.value = AiFinderUiState.Result(match)
            } catch (e: Exception) {
                Log.e("WalletVM", "AI assistant error", e)
                _aiFinderState.value = AiFinderUiState.Error(e.localizedMessage ?: "Failed to find matching card")
            }
        }
    }

    fun resetAiFinder() {
        _aiFinderState.value = AiFinderUiState.Idle
    }

    // Google Contacts integration
    fun loadDeviceContacts(context: Context) {
        viewModelScope.launch {
            val contacts = GoogleContactsHelper.fetchDeviceContacts(context)
            _deviceContacts.value = contacts
        }
    }

    fun importContactAsCard(contact: DeviceContact, onSuccess: () -> Unit) {
        val uid = _currentUser.value?.uid ?: return
        val card = GoogleContactsHelper.convertToBusinessCard(contact, uid)
        viewModelScope.launch {
            try {
                repository.addCard(card)
                onSuccess()
            } catch (e: Exception) {
                Log.e("WalletVM", "Failed to import contact", e)
            }
        }
    }

    fun importAllBusinessContacts(contacts: List<DeviceContact>, onComplete: (Int) -> Unit) {
        val uid = _currentUser.value?.uid ?: return
        val businessContacts = contacts.filter { it.isBusinessContact }
        if (businessContacts.isEmpty()) {
            onComplete(0)
            return
        }
        viewModelScope.launch {
            var count = 0
            for (contact in businessContacts) {
                val card = GoogleContactsHelper.convertToBusinessCard(contact, uid)
                try {
                    repository.addCard(card)
                    count++
                } catch (e: Exception) {
                    Log.e("WalletVM", "Error batch importing card", e)
                }
            }
            onComplete(count)
        }
    }

    fun exportWalletJson(): String {
        return repository.exportWalletJson()
    }

    fun importWalletJson(json: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importWalletJson(json)
            onComplete(count)
        }
    }

    fun createVirtualCardFromUrl(url: String, onComplete: (BusinessCard) -> Unit) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            _ocrEngineStatus.value = "Fetching site metadata..."
            try {
                val card = com.example.data.web.WebCardHelper.fetchSiteMetadata(url)
                _draftCard.value = card
                _ocrEngineStatus.value = "Extracted digital card from site"
                onComplete(card)
            } catch (e: Exception) {
                Log.e("WalletVM", "Error fetching site card", e)
                _ocrEngineStatus.value = "Failed to load website details"
            } finally {
                _isOcrProcessing.value = false
            }
        }
    }

    fun parseAndApplyVCard(vCardText: String, onComplete: (BusinessCard) -> Unit) {
        val card = com.example.data.web.WebCardHelper.parseVCard(vCardText)
        _draftCard.value = card
        _ocrEngineStatus.value = "Extracted contact from vCard"
        onComplete(card)
    }
}
