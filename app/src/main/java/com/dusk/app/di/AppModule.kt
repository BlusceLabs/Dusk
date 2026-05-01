package com.dusk.app.di

import android.content.Context
import com.dusk.app.data.remote.ApiService
import com.dusk.app.data.remote.RetrofitClient
import com.dusk.app.data.repository.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideApiService(): ApiService = RetrofitClient.apiService

    @Provides @Singleton
    fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository = impl

    @Provides @Singleton
    fun providePostRepository(impl: PostRepositoryImpl): PostRepository = impl

    @Provides @Singleton
    fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl

    @Provides @Singleton
    fun provideChatRepository(impl: ChatRepositoryImpl): ChatRepository = impl

    @Provides @Singleton
    fun provideStoryRepository(impl: StoryRepositoryImpl): StoryRepository = impl

    @Provides @Singleton
    fun provideNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository = impl

    @Provides @Singleton
    fun providePremiumRepository(impl: PremiumRepositoryImpl): PremiumRepository = impl

    @Provides @Singleton
    fun provideReelsRepository(impl: ReelsRepositoryImpl): ReelsRepository = impl

    @Provides @Singleton
    fun provideLiveRepository(impl: LiveRepositoryImpl): LiveRepository = impl

    @Provides @Singleton
    fun provideCommunityRepository(impl: CommunityRepositoryImpl): CommunityRepository = impl

    @Provides @Singleton
    fun provideWalletRepository(impl: WalletRepositoryImpl): WalletRepository = impl

    @Provides @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.dusk.app.R.string.google_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
}
