package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.CoverPreset
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.UiState
import com.example.data.repository.MarketplaceRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PropertyFilterType(val labelFa: String) {
    ALL("همه خانه‌ها"),
    APARTMENT("آپارتمان"),
    VILLA("ویلایی"),
    LAND("زمین و کلنگی"),
}

enum class PropertySortOption(val labelFa: String) {
    NEWEST("جدیدترین"),
    PRICE_ASC("ارزان‌ترین"),
    PRICE_DESC("گران‌ترین"),
    AREA_DESC("بزرگ‌ترین متراژ"),
}

class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    val currentUserId: String,
    val currentUserName: String,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow(PropertyFilterType.ALL)
    val selectedType: StateFlow<PropertyFilterType> = _selectedType.asStateFlow()

    private val _sortOption = MutableStateFlow(PropertySortOption.NEWEST)
    val sortOption: StateFlow<PropertySortOption> = _sortOption.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, currentUserName)
        }
    }

    private val rawActiveProperties: StateFlow<UiState<List<Property>>> =
        repository.observeActiveProperties()
            .map<List<Property>, UiState<List<Property>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "خطا در دریافت آگهی‌ها")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val filteredPropertiesState: StateFlow<UiState<List<Property>>> =
        combine(
            rawActiveProperties,
            _searchQuery,
            _selectedType,
            _sortOption,
        ) { state, query, filterType, sort ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> {
                    val trimmed = query.trim()
                    val filtered = state.data.filter { prop ->
                        val typeMatches = when (filterType) {
                            PropertyFilterType.ALL -> true
                            PropertyFilterType.APARTMENT -> prop.propertyType == PropertyType.APARTMENT.name
                            PropertyFilterType.VILLA -> prop.propertyType == PropertyType.VILLA.name
                            PropertyFilterType.LAND -> prop.propertyType == PropertyType.LAND.name
                        }
                        val queryMatches = if (trimmed.isEmpty()) true else {
                            prop.title.contains(trimmed, ignoreCase = true) ||
                                prop.city.contains(trimmed, ignoreCase = true) ||
                                prop.neighborhood.contains(trimmed, ignoreCase = true) ||
                                prop.description.contains(trimmed, ignoreCase = true)
                        }
                        typeMatches && queryMatches
                    }
                    val sorted = when (sort) {
                        PropertySortOption.NEWEST -> filtered
                        PropertySortOption.PRICE_ASC -> filtered.sortedBy { it.priceToman }
                        PropertySortOption.PRICE_DESC -> filtered.sortedByDescending { it.priceToman }
                        PropertySortOption.AREA_DESC -> filtered.sortedByDescending { it.areaSqm }
                    }
                    UiState.Success(sorted)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val myPropertiesState: StateFlow<UiState<List<Property>>> =
        repository.observeMyProperties(currentUserId)
            .map<List<Property>, UiState<List<Property>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "خطا در دریافت آگهی‌های شما")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val favoriteIds: StateFlow<Set<String>> =
        repository.observeFavoriteIds(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing favorites", firestoreError ?: error)
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet(),
            )

    val savedPropertiesState: StateFlow<UiState<List<Property>>> =
        combine(rawActiveProperties, favoriteIds) { state, favs ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> UiState.Success(state.data.filter { it.id in favs })
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId)
            .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "خطا در دریافت گفتگوها")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    fun observePropertyDetail(propertyId: String): Flow<UiState<Property?>> =
        repository.observeProperty(propertyId)
            .map<Property?, UiState<Property?>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "خطا در بارگذاری جزئیات ملک")) }

    fun observeConversationDetail(conversationId: String): Flow<UiState<Conversation?>> =
        repository.observeConversation(conversationId)
            .map<Conversation?, UiState<Conversation?>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "خطا در بارگذاری گفتگو")) }

    fun observeChatMessages(conversationId: String): Flow<UiState<List<ChatMessage>>> =
        repository.observeMessages(conversationId, currentUserId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "خطا در بارگذاری پیام‌ها")) }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterTypeSelected(type: PropertyFilterType) {
        _selectedType.value = type
    }

    fun onSortOptionSelected(option: PropertySortOption) {
        _sortOption.value = option
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun toggleFavorite(propertyId: String) {
        val isFav = propertyId in favoriteIds.value
        viewModelScope.launch {
            repository.toggleFavorite(currentUserId, propertyId, isFav)
                .onSuccess {
                    _snackbarMessage.value = if (isFav) "از نشان‌شده‌ها حذف شد" else "به نشان‌شده‌ها اضافه شد"
                }
                .onFailure {
                    _snackbarMessage.value = "خطا در تغییر وضعیت نشان"
                }
        }
    }

    fun createProperty(
        title: String,
        description: String,
        propertyType: PropertyType,
        city: String,
        neighborhood: String,
        priceToman: Long,
        areaSqm: Long,
        bedrooms: Long,
        buildYear: Long,
        floor: Long,
        hasParking: Boolean,
        hasElevator: Boolean,
        hasStorage: Boolean,
        hasTitleDeed: Boolean,
        coverPreset: CoverPreset,
        photoBase64: String?,
        onSuccess: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.createProperty(
                ownerName = currentUserName,
                title = title,
                description = description,
                propertyType = propertyType,
                city = city,
                neighborhood = neighborhood,
                priceToman = priceToman,
                areaSqm = areaSqm,
                bedrooms = bedrooms,
                buildYear = buildYear,
                floor = floor,
                hasParking = hasParking,
                hasElevator = hasElevator,
                hasStorage = hasStorage,
                hasTitleDeed = hasTitleDeed,
                coverPreset = coverPreset,
                photoBase64 = photoBase64,
            ).onSuccess { newId ->
                _isBusy.value = false
                _snackbarMessage.value = "آگهی شما با موفقیت منتشر شد"
                onSuccess(newId)
            }.onFailure { err ->
                _isBusy.value = false
                _snackbarMessage.value = err.message ?: "خطا در ثبت آگهی"
            }
        }
    }

    fun updateProperty(
        propertyId: String,
        title: String,
        description: String,
        propertyType: PropertyType,
        city: String,
        neighborhood: String,
        priceToman: Long,
        areaSqm: Long,
        bedrooms: Long,
        buildYear: Long,
        floor: Long,
        hasParking: Boolean,
        hasElevator: Boolean,
        hasStorage: Boolean,
        hasTitleDeed: Boolean,
        coverPreset: CoverPreset,
        photoBase64: String?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateProperty(
                propertyId = propertyId,
                title = title,
                description = description,
                propertyType = propertyType,
                city = city,
                neighborhood = neighborhood,
                priceToman = priceToman,
                areaSqm = areaSqm,
                bedrooms = bedrooms,
                buildYear = buildYear,
                floor = floor,
                hasParking = hasParking,
                hasElevator = hasElevator,
                hasStorage = hasStorage,
                hasTitleDeed = hasTitleDeed,
                coverPreset = coverPreset,
                photoBase64 = photoBase64,
            ).onSuccess {
                _isBusy.value = false
                _snackbarMessage.value = "تغییرات آگهی ذخیره شد"
                onSuccess()
            }.onFailure { err ->
                _isBusy.value = false
                _snackbarMessage.value = err.message ?: "خطا در ویرایش آگهی"
            }
        }
    }

    fun markAsSold(propertyId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.markPropertySold(propertyId)
                .onSuccess {
                    _isBusy.value = false
                    _snackbarMessage.value = "وضعیت آگهی به «فروخته شد» تغییر یافت"
                    onSuccess()
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _snackbarMessage.value = err.message ?: "خطا در تغییر وضعیت آگهی"
                }
        }
    }

    fun deleteProperty(propertyId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteProperty(propertyId)
                .onSuccess {
                    _isBusy.value = false
                    _snackbarMessage.value = "آگهی حذف شد"
                    onSuccess()
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _snackbarMessage.value = err.message ?: "خطا در حذف آگهی"
                }
        }
    }

    fun openDirectChat(property: Property, onConversationReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.startOrOpenConversation(property, currentUserName)
                .onSuccess { convId ->
                    _isBusy.value = false
                    onConversationReady(convId)
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _snackbarMessage.value = err.message ?: "خطا در شروع گفتگو"
                }
        }
    }

    fun sendChatMessage(conversation: Conversation, text: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            repository.sendMessage(conversation, currentUserName, text)
                .onSuccess { onSent() }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "خطا در ارسال پیام"
                }
        }
    }

    private fun unwrapFirestoreError(error: Throwable, fallback: String): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallback, rootError)
        return UiState.Error(rootError.message ?: fallback)
    }

    private companion object {
        const val TAG = "MarketplaceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
