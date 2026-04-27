package com.dusk.app.data.repository

import kotlinx.coroutines.flow.Flow

data class PremiumTier(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "USD",
    val features: List<String> = emptyList(),
    val popular: Boolean = false
)

data class PremiumSubscription(
    val id: String = "",
    val userId: String = "",
    val tierId: String = "",
    val status: String = "active",
    val startedAt: Long = 0L,
    val expiresAt: Long = 0L
)

interface PremiumRepository {
    fun getTiers(): Flow<List<PremiumTier>>
    suspend fun getCurrentSubscription(): Result<PremiumSubscription?>
    suspend fun subscribe(tierId: String, paymentMethod: String): Result<PremiumSubscription>
    suspend fun cancelSubscription(): Result<Unit>
}
