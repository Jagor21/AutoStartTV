package com.sgvdev.autostart.data.remote

import com.sgvdev.autostart.models.AdRequest
import javax.inject.Inject
import com.sgvdev.autostart.models.AdResponse
import com.vlifte.autostarttv.BaseNetworkDataSource
import com.vlifte.autostarttv.VlifteApi
import com.vlifte.autostarttv.Result
import com.vlifte.autostarttv.data.remote.AdRemoteDataSource

class AdRemoteDataSourceImpl @Inject constructor (private val vlifteApi: VlifteApi) : BaseNetworkDataSource(),
    AdRemoteDataSource {

    override suspend fun getAds(token: AdRequest): Result<AdResponse> {
        return execute { vlifteApi.getAdsData(token) }
    }
}