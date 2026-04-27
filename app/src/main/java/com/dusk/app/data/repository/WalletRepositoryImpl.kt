package com.dusk.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : WalletRepository {

    override fun getTransactions(): Flow<List<WalletTransaction>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); return@callbackFlow }
        val listener = firestore.collection("users").document(uid)
            .collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val transactions = snapshots.documents.mapNotNull {
                        it.toObject<WalletTransaction>()?.copy(id = it.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getBalance(): Result<WalletBalance> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val doc = firestore.collection("wallets").document(uid).get().await()
        doc.toObject<WalletBalance>() ?: WalletBalance()
    }

    override suspend fun createTransaction(type: String, amount: Double, description: String): Result<WalletTransaction> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val tx = mapOf(
            "userId" to uid,
            "type" to type,
            "amount" to amount,
            "currency" to "USD",
            "status" to "completed",
            "description" to description,
            "createdAt" to System.currentTimeMillis()
        )
        val ref = firestore.collection("users").document(uid)
            .collection("transactions").add(tx).await()
        // Update wallet balance
        firestore.collection("wallets").document(uid)
            .set(mapOf("balance" to com.google.firebase.firestore.FieldValue.increment(amount)), com.google.firebase.firestore.SetOptions.merge()).await()
        WalletTransaction(id = ref.id, userId = uid, type = type, amount = amount, description = description)
    }

    override suspend fun initiatePaypalPayment(amount: Double): Result<String> = runCatching {
        // In production, this would call a Firebase Function or backend API
        // For now, return a placeholder URL
        "https://paypal.com/checkout?amount=$amount&currency=USD"
    }
}
