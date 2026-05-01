package com.dusk.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(val success: Boolean, val data: T?, val error: String?)

@JsonClass(generateAdapter = true)
data class StreamStartRequest(val streamId: String, val streamerId: String, val title: String)

@JsonClass(generateAdapter = true)
data class PushTokenRequest(val token: String, val platform: String = "android")

@JsonClass(generateAdapter = true)
data class PaymentIntentRequest(val amount: Double, val currency: String = "usd")

@JsonClass(generateAdapter = true)
data class PaymentIntentResponse(@Json(name = "clientSecret") val clientSecret: String)

@JsonClass(generateAdapter = true)
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
