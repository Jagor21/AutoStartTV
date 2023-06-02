package com.vlifte.autostarttv.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.AdResponse
import com.vlifte.autostarttv.domain.use_case.GetAdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val getAdUseCase: GetAdUseCase
) : ViewModel() {

//    private val _ad: MutableStateFlow<AdResponse?> = MutableStateFlow(null)
//    val ad = _ad.asStateFlow()

    private val _ad: MutableLiveData<AdResponse> = MutableLiveData()
    val ad: LiveData<AdResponse>
        get() = _ad

    var needLoadAd = true

    fun getAd(token: AdRequest) {
        viewModelScope.launch {
            while (needLoadAd) {
                getAdUseCase.execute(token) {
                    it.fold(
                        onSuccess = {
                            if (_ad.value?.success?.checksum != it.success.checksum) {
                                _ad.value = it
                            }
                        },
                        onFailure = {}
                    )
                }
                delay(60000L)
            }
        }
    }
}