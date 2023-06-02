package com.vlifte.autostarttv.domain.use_case

import com.sgvdev.autostart.models.AdResponse
import javax.inject.Inject
import com.sgvdev.autostart.models.AdRequest
import com.vlifte.autostarttv.BaseUseCase
import com.vlifte.autostarttv.data.remote.AdRepository
import com.vlifte.autostarttv.Result

class GetAdUseCase @Inject constructor(
    private val adRepository: AdRepository
) : BaseUseCase<AdRequest, AdResponse>() {
    override suspend fun run(param: AdRequest): Result<AdResponse> {
        return adRepository.getAds(param)
    }
}