package com.vlifte.autostarttv.di

import android.content.Context
import androidx.datastore.dataStore
import com.vlifte.autostarttv.AppSettings
import com.vlifte.autostarttv.utils.SettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class SettingsModule {
    @Singleton
    @Provides
    fun provideSettings(@ApplicationContext context: Context) = AppSettings(context.dataStore)

    private val Context.dataStore by dataStore(
        fileName = "prefs.proto",
        serializer = SettingsSerializer
    )
}