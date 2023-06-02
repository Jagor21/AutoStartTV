package com.sgvdev.autostart.di

import com.vlifte.autostarttv.data.remote.AdRemoteDataSource
import com.sgvdev.autostart.data.remote.AdRemoteDataSourceImpl
import com.vlifte.autostarttv.data.remote.AdRepository
import com.sgvdev.autostart.data.remote.AdRepositoryImpl
import com.vlifte.autostarttv.VlifteApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RepositoryModel {

    @Singleton
    @Provides
    fun provideAdDataSource(vlifteApi: VlifteApi): AdRemoteDataSource =
        AdRemoteDataSourceImpl(vlifteApi)

    @Singleton
    @Provides
    fun provideAdRepository(adRemoteDataSource: AdRemoteDataSource): AdRepository =
        AdRepositoryImpl(adRemoteDataSource)
}