package com.example.data.repository

import com.example.data.entity.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AppRepository {
    private val db: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    companion object {
        private val localUsers = java.util.concurrent.ConcurrentHashMap<String, UserEntity>().apply {
            val defaultUsers = listOf(
                UserEntity(
                    id = "user_demo",
                    username = "مستخدم عادي",
                    email = "user@play.com",
                    passwordHash = "user",
                    role = "USER",
                    developerName = "",
                    balance = 100.0
                ),
                UserEntity(
                    id = "dev_demo",
                    username = "استوديو الألعاب المتميز",
                    email = "dev@play.com",
                    passwordHash = "dev",
                    role = "DEVELOPER",
                    developerName = "استوديو الألعاب المتميز",
                    balance = 50.0
                ),
                UserEntity(
                    id = "admin_demo",
                    username = "مدير النظام",
                    email = "admin@play.com",
                    passwordHash = "admin",
                    role = "ADMIN",
                    developerName = "",
                    balance = 9999.0
                )
            )
            defaultUsers.forEach { put(it.id, it) }
        }
        private val localApps = java.util.concurrent.ConcurrentHashMap<String, AppEntity>()
    }

    // --- User Operations ---
    suspend fun getUserByEmail(email: String): UserEntity? {
        val cached = localUsers.values.find { it.email.equals(email, ignoreCase = true) }
        if (cached != null) return cached

        val firestore = db ?: return null
        return try {
            val userFromDb = kotlinx.coroutines.withTimeoutOrNull(2000) {
                val snapshot = firestore.collection("users").whereEqualTo("email", email).get().await()
                snapshot.documents.firstOrNull()?.toObject(UserEntity::class.java)
            }
            if (userFromDb != null) {
                localUsers[userFromDb.id] = userFromDb
            }
            userFromDb
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUserById(id: String): Flow<UserEntity?> = callbackFlow {
        val cached = localUsers[id]
        if (cached != null) {
            trySend(cached)
        }
        val firestore = db
        if (firestore == null) {
            if (cached == null) trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("users").document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localUsers[id])
                    return@addSnapshotListener
                }
                val fetched = try { snapshot?.toObject(UserEntity::class.java) } catch (e: Exception) { null }
                if (fetched != null) {
                    localUsers[fetched.id] = fetched
                    trySend(fetched)
                } else {
                    trySend(localUsers[id])
                }
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localUsers[id])
            awaitClose { }
        }
    }

    suspend fun getUserByIdOneShot(id: String): UserEntity? {
        val cached = localUsers[id]
        if (cached != null) return cached

        val firestore = db ?: return null
        return try {
            val userFromDb = kotlinx.coroutines.withTimeoutOrNull(2000) {
                firestore.collection("users").document(id).get().await().toObject(UserEntity::class.java)
            }
            if (userFromDb != null) {
                localUsers[userFromDb.id] = userFromDb
            }
            userFromDb
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertUser(user: UserEntity): String {
        val generatedId = if (user.id.isNotBlank()) user.id else "usr_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().take(5)
        val newUser = user.copy(id = generatedId)
        localUsers[generatedId] = newUser

        val firestore = db ?: return generatedId
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                firestore.collection("users").document(generatedId).set(newUser).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return generatedId
    }

    suspend fun updateUser(user: UserEntity) {
        localUsers[user.id] = user
        val firestore = db ?: return
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                firestore.collection("users").document(user.id).set(user).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllUsers(): Flow<List<UserEntity>> = callbackFlow {
        trySend(localUsers.values.toList())
        val firestore = db
        if (firestore == null) {
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("users").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localUsers.values.toList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { 
                    try { it.toObject(UserEntity::class.java) } catch (e: Exception) { null }
                } ?: emptyList()
                users.forEach { localUsers[it.id] = it }
                trySend(localUsers.values.toList())
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localUsers.values.toList())
            awaitClose { }
        }
    }

    suspend fun deleteUser(user: UserEntity) {
        val firestore = db ?: return
        try {
            firestore.collection("users").document(user.id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- App Operations ---
    fun getAllApps(): Flow<List<AppEntity>> = callbackFlow {
        trySend(localApps.values.toList())
        val firestore = db
        if (firestore == null) {
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("apps").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localApps.values.toList())
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { 
                    try { it.toObject(AppEntity::class.java) } catch (e: Exception) { null }
                } ?: emptyList()
                apps.forEach { localApps[it.id] = it }
                trySend(localApps.values.toList())
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localApps.values.toList())
            awaitClose { }
        }
    }

    fun getAppsByType(type: String): Flow<List<AppEntity>> = callbackFlow {
        trySend(localApps.values.filter { it.type == type })
        val firestore = db
        if (firestore == null) {
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("apps").whereEqualTo("type", type).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localApps.values.filter { it.type == type })
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { 
                    try { it.toObject(AppEntity::class.java) } catch (e: Exception) { null }
                } ?: emptyList()
                apps.forEach { localApps[it.id] = it }
                trySend(localApps.values.filter { it.type == type })
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localApps.values.filter { it.type == type })
            awaitClose { }
        }
    }

    fun getAppById(id: String): Flow<AppEntity?> = callbackFlow {
        val cached = localApps[id]
        if (cached != null) trySend(cached)

        val firestore = db
        if (firestore == null) {
            if (cached == null) trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("apps").document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localApps[id])
                    return@addSnapshotListener
                }
                val app = try { snapshot?.toObject(AppEntity::class.java) } catch (e: Exception) { null }
                if (app != null) {
                    localApps[app.id] = app
                    trySend(app)
                } else {
                    trySend(localApps[id])
                }
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localApps[id])
            awaitClose { }
        }
    }

    suspend fun getAppByIdOneShot(id: String): AppEntity? {
        val cached = localApps[id]
        if (cached != null) return cached

        val firestore = db ?: return null
        return try {
            val fetched = kotlinx.coroutines.withTimeoutOrNull(2000) {
                firestore.collection("apps").document(id).get().await().toObject(AppEntity::class.java)
            }
            if (fetched != null) localApps[fetched.id] = fetched
            fetched
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertApp(app: AppEntity): String {
        val generatedId = if (app.id.isNotBlank()) app.id else "app_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().take(5)
        val newApp = app.copy(id = generatedId)
        localApps[generatedId] = newApp

        val firestore = db ?: return generatedId
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                firestore.collection("apps").document(generatedId).set(newApp).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return generatedId
    }

    suspend fun updateApp(app: AppEntity) {
        localApps[app.id] = app
        val firestore = db ?: return
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                firestore.collection("apps").document(app.id).set(app).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteApp(app: AppEntity) {
        localApps.remove(app.id)
        val firestore = db ?: return
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                firestore.collection("apps").document(app.id).delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAppsByDeveloper(developerId: String): Flow<List<AppEntity>> = callbackFlow {
        trySend(localApps.values.filter { it.developerId == developerId })
        val firestore = db
        if (firestore == null) {
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("apps").whereEqualTo("developerId", developerId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localApps.values.filter { it.developerId == developerId })
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { try { it.toObject(AppEntity::class.java) } catch(e: Exception) { null } } ?: emptyList()
                apps.forEach { localApps[it.id] = it }
                trySend(localApps.values.filter { it.developerId == developerId })
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localApps.values.filter { it.developerId == developerId })
            awaitClose { }
        }
    }

    fun getUnverifiedApps(): Flow<List<AppEntity>> = callbackFlow {
        trySend(localApps.values.filter { !it.isVerified })
        val firestore = db
        if (firestore == null) {
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("apps").whereEqualTo("isVerified", false).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localApps.values.filter { !it.isVerified })
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { try { it.toObject(AppEntity::class.java) } catch(e: Exception) { null } } ?: emptyList()
                apps.forEach { localApps[it.id] = it }
                trySend(localApps.values.filter { !it.isVerified })
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localApps.values.filter { !it.isVerified })
            awaitClose { }
        }
    }

    fun getVerifiedApps(): Flow<List<AppEntity>> = callbackFlow {
        trySend(localApps.values.toList())
        val firestore = db
        if (firestore == null) {
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("apps").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(localApps.values.toList())
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { try { it.toObject(AppEntity::class.java) } catch(e: Exception) { null } } ?: emptyList()
                apps.forEach { localApps[it.id] = it }
                trySend(localApps.values.toList())
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(localApps.values.toList())
            awaitClose { }
        }
    }

    // --- Review Operations ---
    fun getReviewsForApp(appId: String): Flow<List<ReviewEntity>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("reviews").whereEqualTo("appId", appId).orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reviews = snapshot?.documents?.mapNotNull { try { it.toObject(ReviewEntity::class.java) } catch(e: Exception) { null } } ?: emptyList()
                trySend(reviews)
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            awaitClose { }
        }
    }

    suspend fun insertReview(review: ReviewEntity): String {
        val firestore = db ?: return "local_review_" + System.currentTimeMillis()
        return try {
            val ref = firestore.collection("reviews").document()
            val newReview = review.copy(id = ref.id)
            ref.set(newReview).await()
            ref.id
        } catch (e: Exception) {
            e.printStackTrace()
            "local_review_" + System.currentTimeMillis()
        }
    }

    suspend fun deleteReview(review: ReviewEntity) {
        val firestore = db ?: return
        try {
            firestore.collection("reviews").document(review.id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Download Operations ---
    fun getDownloadsForUser(userId: String): Flow<List<DownloadEntity>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("downloads").whereEqualTo("userId", userId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val downloads = snapshot?.documents?.mapNotNull { try { it.toObject(DownloadEntity::class.java) } catch(e: Exception) { null } } ?: emptyList()
                trySend(downloads)
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            awaitClose { }
        }
    }

    fun getDownloadStatus(userId: String, appId: String): Flow<DownloadEntity?> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        try {
            val listener = firestore.collection("downloads")
                .whereEqualTo("userId", userId)
                .whereEqualTo("appId", appId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.documents?.firstOrNull()?.let { try { it.toObject(DownloadEntity::class.java) } catch(e: Exception) { null } })
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
            awaitClose { }
        }
    }

    suspend fun getDownloadOneShot(userId: String, appId: String): DownloadEntity? {
        val firestore = db ?: return null
        return try {
            val snapshot = firestore.collection("downloads")
                .whereEqualTo("userId", userId)
                .whereEqualTo("appId", appId)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(DownloadEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertDownload(download: DownloadEntity): String {
        val firestore = db ?: return "local_dl_" + System.currentTimeMillis()
        return try {
            val ref = firestore.collection("downloads").document()
            val newDownload = download.copy(id = ref.id)
            ref.set(newDownload).await()
            ref.id
        } catch (e: Exception) {
            e.printStackTrace()
            "local_dl_" + System.currentTimeMillis()
        }
    }

    suspend fun updateDownload(download: DownloadEntity) {
        val firestore = db ?: return
        try {
            firestore.collection("downloads").document(download.id).set(download).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteDownload(download: DownloadEntity) {
        val firestore = db ?: return
        try {
            firestore.collection("downloads").document(download.id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
