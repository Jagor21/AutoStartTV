package com.vlifte.autostarttv.data.remote

import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.AdResponse
import com.vlifte.autostarttv.Result

interface AdRemoteDataSource {

    suspend fun getAds(token: AdRequest): Result<AdResponse>
}