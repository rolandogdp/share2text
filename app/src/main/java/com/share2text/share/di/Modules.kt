package com.share2text.share.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.share2text.share.download.ModelRepository
import com.share2text.share.nativebridge.WhisperBridge
import com.share2text.share.repo.TranscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object Modules {

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    @Provides @Singleton
    fun provideModelRepository(@ApplicationContext ctx: Context, client: OkHttpClient): ModelRepository =
        ModelRepository(ctx, client)

    @Provides @Singleton
    fun provideWhisperBridge(@ApplicationContext ctx: Context): WhisperBridge = WhisperBridge(ctx)

    @Provides @Singleton
    fun provideTranscriptionRepository(
        @ApplicationContext ctx: Context,
        whisper: WhisperBridge
    ): TranscriptionRepository = TranscriptionRepository(ctx, whisper)
}
