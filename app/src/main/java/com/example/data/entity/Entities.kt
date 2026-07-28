package com.example.data.entity

import com.google.firebase.firestore.DocumentId

data class UserEntity(
    @DocumentId val id: String = "",
    val username: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val role: String = "", // "USER", "DEVELOPER", "ADMIN"
    val developerName: String = "",
    val balance: Double = 100.0, // Preloaded with $100 for simulated e-payment!
    val createdAt: Long = System.currentTimeMillis()
)

data class AppEntity(
    @DocumentId val id: String = "",
    val title: String = "",
    val packageName: String = "",
    val description: String = "",
    val developerId: String = "",
    val developerName: String = "",
    val category: String = "", // "Tools", "Social", "Finance", "Productivity", "Action", "Puzzle", "RPG", "Racing", etc.
    val type: String = "", // "APP" or "GAME"
    val price: Double = 0.0, // 0.0 means free
    val rating: Float = 0.0f,
    val ratingsCount: Int = 0,
    val downloadsCount: Int = 0,
    val size: String = "25 MB",
    val version: String = "1.0.0",
    val isVerified: Boolean = true, // Always verified and live
    val apkFileName: String = "",
    val bannerColor: String = "#FF6200EE", // Dynamic styling color
    val iconUri: String? = null,
    val screenshotUris: String = "", // Comma separated URIs
    val videoUrl: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ReviewEntity(
    @DocumentId val id: String = "",
    val appId: String = "",
    val userId: String = "",
    val username: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class DownloadEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appId: String = "",
    val isInstalled: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0 during download simulation
    val purchasedPrice: Double = 0.0,
    val installedAt: Long = System.currentTimeMillis()
)
