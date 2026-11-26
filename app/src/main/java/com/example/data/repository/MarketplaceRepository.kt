package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.CoverPreset
import com.example.data.model.Favorite
import com.example.data.model.Property
import com.example.data.model.PropertyStatus
import com.example.data.model.PropertyType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("کاربر باید با حساب گوگل وارد شده باشد.")
    }

    suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "عدم تطابق شناسه کاربر" }
            val safeName = displayName.trim().ifBlank { "کاربر بی‌واسطه" }.take(80)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeActiveProperties(): Flow<List<Property>> = flow {
        val uid = requireUserId()
        val path = "properties"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", PropertyStatus.ACTIVE.name)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Property::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyProperties(userId: String): Flow<List<Property>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "دسترسی غیرمجاز" }
        val path = "properties"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Property::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeProperty(propertyId: String): Flow<Property?> = flow {
        val uid = requireUserId()
        val path = "properties/$propertyId"
        emitAll(
            db.collection("properties").document(propertyId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            Property::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeFavoriteIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "دسترسی غیرمجاز" }
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Favorite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.propertyId }.toSet()
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun toggleFavorite(userId: String, propertyId: String, currentlyFavorite: Boolean): Result<Unit> {
        val path = "users/$userId/favorites/$propertyId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "عدم تطابق شناسه کاربر" }
            require(propertyId.isNotBlank()) { "شناسه آگهی نامعتبر است" }
            val docRef = db.collection("users").document(uid).collection("favorites").document(propertyId)
            if (currentlyFavorite) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "propertyId" to propertyId,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createProperty(
        ownerName: String,
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
    ): Result<String> {
        var path = "properties"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanCity = city.trim()
            val cleanNeighborhood = neighborhood.trim()
            val cleanDesc = description.trim()
            val cleanOwnerName = ownerName.trim().ifBlank { "مالک خانه" }.take(80)

            require(cleanTitle.length in 3..120) { "عنوان آگهی باید بین ۳ تا ۱۲۰ کاراکتر باشد." }
            require(cleanCity.length in 2..60) { "نام شهر باید بین ۲ تا ۶۰ کاراکتر باشد." }
            require(cleanNeighborhood.length in 1..80) { "نام محله باید بین ۱ تا ۸۰ کاراکتر باشد." }
            require(cleanDesc.length <= 1500) { "توضیحات نباید بیشتر از ۱۵۰۰ کاراکتر باشد." }
            require(priceToman in 1..1_000_000_000_000L) { "قیمت واردشده معتبر نیست." }
            require(areaSqm in 10..100_000L) { "متراژ باید بین ۱۰ تا ۱۰۰,۰۰۰ متر باشد." }
            require(bedrooms in 0..20L) { "تعداد خواب باید بین ۰ تا ۲۰ باشد." }
            require(buildYear in 1300..1410L) { "سال ساخت باید بین ۱۳۰۰ تا ۱۴۱۰ باشد." }
            require(floor in -2..60L) { "طبقه باید بین -۲ تا ۶۰ باشد." }
            if (photoBase64 != null) {
                require(photoBase64.length <= 180_000) { "حجم تصویر انتخابی بیش از حد مجاز است." }
            }

            val docRef = db.collection("properties").document()
            path = docRef.path
            val payload = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "ownerName" to cleanOwnerName,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "propertyType" to propertyType.name,
                "status" to PropertyStatus.ACTIVE.name,
                "city" to cleanCity,
                "neighborhood" to cleanNeighborhood,
                "priceToman" to priceToman,
                "areaSqm" to areaSqm,
                "bedrooms" to bedrooms,
                "buildYear" to buildYear,
                "floor" to floor,
                "hasParking" to hasParking,
                "hasElevator" to hasElevator,
                "hasStorage" to hasStorage,
                "hasTitleDeed" to hasTitleDeed,
                "coverPreset" to coverPreset.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (!photoBase64.isNullOrBlank()) {
                payload["photoBase64"] = photoBase64
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProperty(
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
    ): Result<Unit> {
        val path = "properties/$propertyId"
        return try {
            requireUserId()
            require(propertyId.isNotBlank()) { "شناسه آگهی نامعتبر است" }
            val cleanTitle = title.trim()
            val cleanCity = city.trim()
            val cleanNeighborhood = neighborhood.trim()
            val cleanDesc = description.trim()

            require(cleanTitle.length in 3..120) { "عنوان آگهی باید بین ۳ تا ۱۲۰ کاراکتر باشد." }
            require(cleanCity.length in 2..60) { "نام شهر باید بین ۲ تا ۶۰ کاراکتر باشد." }
            require(cleanNeighborhood.length in 1..80) { "نام محله باید بین ۱ تا ۸۰ کاراکتر باشد." }
            require(cleanDesc.length <= 1500) { "توضیحات نباید بیشتر از ۱۵۰۰ کاراکتر باشد." }
            require(priceToman in 1..1_000_000_000_000L) { "قیمت واردشده معتبر نیست." }
            require(areaSqm in 10..100_000L) { "متراژ باید بین ۱۰ تا ۱۰۰,۰۰۰ متر باشد." }
            require(bedrooms in 0..20L) { "تعداد خواب باید بین ۰ تا ۲۰ باشد." }
            require(buildYear in 1300..1410L) { "سال ساخت باید بین ۱۳۰۰ تا ۱۴۱۰ باشد." }
            require(floor in -2..60L) { "طبقه باید بین -۲ تا ۶۰ باشد." }
            if (photoBase64 != null) {
                require(photoBase64.length <= 180_000) { "حجم تصویر انتخابی بیش از حد مجاز است." }
            }

            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "propertyType" to propertyType.name,
                "city" to cleanCity,
                "neighborhood" to cleanNeighborhood,
                "priceToman" to priceToman,
                "areaSqm" to areaSqm,
                "bedrooms" to bedrooms,
                "buildYear" to buildYear,
                "floor" to floor,
                "hasParking" to hasParking,
                "hasElevator" to hasElevator,
                "hasStorage" to hasStorage,
                "hasTitleDeed" to hasTitleDeed,
                "coverPreset" to coverPreset.name,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (!photoBase64.isNullOrBlank()) {
                updates["photoBase64"] = photoBase64
            } else {
                updates["photoBase64"] = FieldValue.delete()
            }

            db.collection("properties").document(propertyId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markPropertySold(propertyId: String): Result<Unit> {
        val path = "properties/$propertyId"
        return try {
            requireUserId()
            require(propertyId.isNotBlank()) { "شناسه آگهی نامعتبر است" }
            db.collection("properties").document(propertyId).update(
                mapOf(
                    "status" to PropertyStatus.SOLD.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProperty(propertyId: String): Result<Unit> {
        val path = "properties/$propertyId"
        return try {
            requireUserId()
            require(propertyId.isNotBlank()) { "شناسه آگهی نامعتبر است" }
            db.collection("properties").document(propertyId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "دسترسی غیرمجاز" }
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> = flow {
        val uid = requireUserId()
        val path = "conversations/$conversationId"
        emitAll(
            db.collection("conversations").document(conversationId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            Conversation::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "دسترسی غیرمجاز" }
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId)
                .collection("messages")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun startOrOpenConversation(
        property: Property,
        buyerName: String,
    ): Result<String> {
        var path = "conversations"
        return try {
            val uid = requireUserId()
            require(property.id.isNotBlank()) { "شناسه ملک نامعتبر است" }
            require(property.ownerId != uid) { "نمی‌توانید با خودتان گفتگو کنید." }
            val convId = "${property.id}_$uid"
            path = "conversations/$convId"
            val docRef = db.collection("conversations").document(convId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeBuyerName = buyerName.trim().ifBlank { "خریدار" }.take(80)
                val safeSellerName = property.ownerName.trim().ifBlank { "مالک" }.take(80)
                docRef.set(
                    mapOf(
                        "propertyId" to property.id,
                        "propertyTitle" to property.title.take(120),
                        "propertyCity" to property.city.take(60),
                        "propertyPriceToman" to property.priceToman,
                        "buyerId" to uid,
                        "buyerName" to safeBuyerName,
                        "sellerId" to property.ownerId,
                        "sellerName" to safeSellerName,
                        "participantUids" to listOf(uid, property.ownerId),
                        "lastMessage" to "",
                        "lastSenderId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderName: String,
        text: String,
    ): Result<Unit> {
        val cleanText = text.trim()
        val convId = conversation.id
        var path = "conversations/$convId/messages"
        return try {
            val uid = requireUserId()
            require(convId.isNotBlank()) { "شناسه گفتگو نامعتبر است" }
            require(cleanText.length in 1..1000) { "متن پیام باید بین ۱ تا ۱۰۰۰ کاراکتر باشد." }
            require(uid in conversation.participantUids) { "شما عضو این گفتگو نیستید." }

            val convRef = db.collection("conversations").document(convId)
            val msgRef = convRef.collection("messages").document()
            path = msgRef.path
            val safeSenderName = senderName.trim().ifBlank { "کاربر" }.take(80)

            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "conversationId" to convId,
                    "senderId" to uid,
                    "senderName" to safeSenderName,
                    "participantUids" to conversation.participantUids,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
