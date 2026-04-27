package com.dusk.app.di

import com.dusk.app.data.remote.ApiService
import com.dusk.app.data.remote.RetrofitClient
import com.dusk.app.data.repository.AuthRepository
import com.dusk.app.data.repository.AuthRepositoryImpl
import com.dusk.app.data.repository.ChatRepository
import com.dusk.app.data.repository.ChatRepositoryImpl
import com.dusk.app.data.repository.PostRepository
import com.dusk.app.data.repository.PostRepositoryImpl
import com.dusk.app.data.repository.StoryRepository
import com.dusk.app.data.repository.StoryRepositoryImpl
import com.dusk.app.data.repository.UserRepository
import com.dusk.app.data.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
