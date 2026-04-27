package com.dusk.app.data.repository

import kotlinx.coroutines.flow.Flow

data class WalletTransaction(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // tip, subscription, withdrawal, deposit
    val amount: Double = 0.0,
    val currency: String = "USD",
    val status: String = "completed",
    val description: String = "",
    val createdAt: Long = 0L
)

data class WalletBalance(
    val balance: Double = 0.0,
    val currency: String = "USD",
    val lastUpdated: Long = 0L
)

interface WalletRepository {
    fun getTransactions(): Flow<List<WalletTransaction>>
    suspend fun getBalance(): Result<WalletBalance>
    suspend fun createTransaction(type: String, amount: Double, description: String): Result<WalletTransaction>
    suspend fun initiatePaypalPayment(amount: Double): Result<String> // returns approval URL
}
