package com.actito.go.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.actito.go.BuildConfig
import com.actito.go.core.DeepLinksService
import com.actito.go.network.assets.AssetsService
import com.actito.go.network.push.PushService
import com.actito.go.storage.db.ActitoDatabase
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ActitoDatabase {
        return Room.databaseBuilder(context, ActitoDatabase::class.java, "notificare-app.db")
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun provideAuthenticatedHttpClient(preferences: ActitoSharedPreferences): OkHttpClient {
        val logger = HttpLoggingInterceptor()
            .setLevel(
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            )

        return OkHttpClient.Builder()
            .authenticator(Authenticator { _, response ->
                val credentials = preferences.appConfiguration?.let { configuration ->
                    Credentials.basic(configuration.applicationKey, configuration.applicationSecret)
                } ?: return@Authenticator response.request

                return@Authenticator response.request.newBuilder()
                    .header("Authorization", credentials)
                    .build()
            })
            .addInterceptor(logger)
            .build()
    }

    @Provides
    fun provideAssetsService(): AssetsService {
        return AssetsService()
    }

    @Provides
    fun providePushService(client: OkHttpClient, moshi: Moshi): PushService {
        return Retrofit.Builder()
            .client(client)
            .baseUrl("https://push.notifica.re")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PushService::class.java)
    }

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDeepLinksServices(): DeepLinksService {
        return DeepLinksService()
    }
}
