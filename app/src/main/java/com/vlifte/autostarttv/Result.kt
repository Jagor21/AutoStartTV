package com.vlifte.autostarttv

sealed class Result<out T> {
    class Success<out T>(val data: T) : Result<T>()
    sealed class ErrorResult : Result<Nothing>() {
        //        class NetworkErrorResponse(val code: Int, val error: String) : ErrorResult()
        class NetworkErrorResponse(
            val code: Int,
            val errorMessage: String,
            val errorsList: List<Pair<String, String>>? = null
        ) : ErrorResult()

        class LocalErrorResponse(val error: Throwable) : ErrorResult()
        class UnsecuredErrorResponse(val error: Throwable) : ErrorResult()
    }

    inline fun fold(
        onSuccess: (data: T) -> Unit,
        onFailure: (error: ErrorResult) -> Unit = {}
    ) {
        when (this) {
            is Success<T> -> onSuccess.invoke(data)
            is ErrorResult -> onFailure.invoke(this)
        }
    }

    companion object {
        const val SOCKET_TIMEOUT_EXCEPTION = -100
    }
}