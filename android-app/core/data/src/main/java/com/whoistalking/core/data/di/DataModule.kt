package com.whoistalking.core.data.di

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.whoistalking.core.data.db.AppDatabase
import com.whoistalking.core.data.featureflag.FeatureFlagRepositoryImpl
import com.whoistalking.core.data.network.SessionApi
import com.whoistalking.core.data.repository.SessionRepositoryImpl
import com.whoistalking.core.domain.repository.FeatureFlagRepository
import com.whoistalking.core.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "who_is_talking.db",
    ).build()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .certificatePinner(
            CertificatePinner.Builder()
                .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .add("api.example.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
                .build(),
        )
        .build()

    @Provides
    @Singleton
    fun provideSessionApi(client: OkHttpClient): SessionApi = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
        .client(client)
        .build()
        .create(SessionApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {
    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindFeatureFlagRepository(impl: FeatureFlagRepositoryImpl): FeatureFlagRepository
}
