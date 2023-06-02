package com.sgvdev.autostart.data.remote

import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.AdResponse
import com.vlifte.autostarttv.data.remote.AdRemoteDataSource
import com.vlifte.autostarttv.data.remote.AdRepository
import com.vlifte.autostarttv.Result
import javax.inject.Inject

class AdRepositoryImpl @Inject constructor(
    private val adRemoteDataSource: AdRemoteDataSource
) : AdRepository {

    override suspend fun getAds(token: AdRequest): Result<AdResponse> {
        return adRemoteDataSource.getAds(token)
    }
}