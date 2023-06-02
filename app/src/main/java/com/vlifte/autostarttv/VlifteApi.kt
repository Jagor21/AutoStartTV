package com.vlifte.autostarttv

import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.AdResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VlifteApi {

    @GET("_tv/{token}")
    fun getAds(@Path("token") token: String)

    @POST("api/monitors/data")
    suspend fun getAdsData(@Body token: AdRequest): AdResponse


}