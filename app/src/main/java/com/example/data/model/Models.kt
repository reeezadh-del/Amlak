package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PropertyType(val labelFa: String) {
    APARTMENT("آپارتمان"),
    VILLA("ویلایی"),
    LAND("زمین و کلنگی");

    companion object {
        fun fromRaw(raw: String?): PropertyType =
            entries.firstOrNull { it.name == raw } ?: APARTMENT
    }
}

enum class PropertyStatus(val labelFa: String) {
    ACTIVE("فعال"),
    SOLD("فروخته شد");

    companion object {
        fun fromRaw(raw: String?): PropertyStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class CoverPreset(val labelFa: String) {
    HERO_HOME("نمای آپارتمان مدرن"),
    VILLA_NORTH("نمای ویلای حیاط‌دار"),
    INTERIOR_LIVING("فضای داخلی نورگیر"),
    CUSTOM_PHOTO("تصویر اختصاصی مالک");

    companion object {
        fun fromRaw(raw: String?): CoverPreset =
            entries.firstOrNull { it.name == raw } ?: HERO_HOME
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Property(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val propertyType: String = PropertyType.APARTMENT.name,
    val status: String = PropertyStatus.ACTIVE.name,
    val city: String = "",
    val neighborhood: String = "",
    val priceToman: Long = 0L,
    val areaSqm: Long = 0L,
    val bedrooms: Long = 0L,
    val buildYear: Long = 1400L,
    val floor: Long = 1L,
    val hasParking: Boolean = false,
    val hasElevator: Boolean = false,
    val hasStorage: Boolean = false,
    val hasTitleDeed: Boolean = true,
    val coverPreset: String = CoverPreset.HERO_HOME.name,
    val photoBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun typeEnum(): PropertyType = PropertyType.fromRaw(propertyType)
    fun statusEnum(): PropertyStatus = PropertyStatus.fromRaw(status)
    fun coverEnum(): CoverPreset = CoverPreset.fromRaw(coverPreset)
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val propertyId: String = "",
    val propertyTitle: String = "",
    val propertyCity: String = "",
    val propertyPriceToman: Long = 0L,
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val participantUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantUids: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Favorite(
    @DocumentId val id: String = "",
    val userId: String = "",
    val propertyId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
