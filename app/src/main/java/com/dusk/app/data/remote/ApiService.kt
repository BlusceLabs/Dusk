package com.dusk.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class ApiResponse<T>(val success: Boolean, val data: T?, val error: String?)
data class StreamStartRequest(val streamId: String, val streamerId: String, val title: String)
data class PushTokenRequest(val token: String, val platform: String = "android")
data class PaymentIntentRequest(val amount: Double, val currency: String = "usd")
data class PaymentIntentResponse(@SerializedName("clientSecret") val clientSecret: String)
data class ContentFlagRequest(val targetId: String, val targetType: String, val reporterId: String, val reason: String)

interface ApiService {
    @POST("media/upload")
    suspend fun requestUploadUrl(@Body body: Map<String, String>): ApiResponse<Map<String, String>>
    @POST("social/follow")
    suspend fun followUser(@Body body: Map<String, String>): ApiResponse<Boolean>
    @POST("social/unfollow")
    suspend fun unfollowUser(@Body body: Map<String, String>): ApiResponse<Boolean>
    @POST("payments/create-intent")
    suspend fun createPaymentIntent(@Body body: PaymentIntentRequest): ApiResponse<PaymentIntentResponse>
    @POST("push/register")
    suspend fun registerPushToken(@Body body: PushTokenRequest): ApiResponse<Boolean>
    @POST("events/stream-started")
    suspend fun streamStarted(@Body body: StreamStartRequest): ApiResponse<Boolean>
    @POST("events/content-flagged")
    suspend fun contentFlagged(@Body body: ContentFlagRequest): ApiResponse<Boolean>
}
