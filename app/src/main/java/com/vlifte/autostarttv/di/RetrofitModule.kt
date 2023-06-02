package com.sgvdev.autostart.di

import com.google.gson.Gson
import com.vlifte.autostarttv.VlifteApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val BASE_URL = "https://vlifte.by/"

private const val CONNECT_TIMEOUT_S = 15L
private const val READ_TIMEOUT_S = 15L
private const val WRITE_TIMEOUT_S = 15L
private const val CONNECT_TIMEOUT_M = 1L
private const val READ_TIMEOUT_M = 1L
private const val WRITE_TIMEOUT_M = 1L

@InstallIn(SingletonComponent::class)
@Module
class RetrofitModule {

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return Gson()
    }

    @Singleton
    @Provides
    fun provideGsonConverter(gson: Gson): GsonConverterFactory {
        return GsonConverterFactory.create(gson)
    }

    @Singleton
    @Provides
    fun provideRetrofit(gsonConverter: GsonConverterFactory, client: OkHttpClient) =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(gsonConverter)
            .client(client)

    @Singleton
    @Provides
    fun provideHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_M, TimeUnit.MINUTES)
            .readTimeout(READ_TIMEOUT_M, TimeUnit.MINUTES)
            .writeTimeout(WRITE_TIMEOUT_M, TimeUnit.MINUTES)
//            .pingInterval(5, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Singleton
    @Provides
    fun providesHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        return httpLoggingInterceptor.apply {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Singleton
    @Provides
    fun provideVlifteApi(retrofit: Retrofit.Builder) =
        retrofit.build().create(VlifteApi::class.java)
}