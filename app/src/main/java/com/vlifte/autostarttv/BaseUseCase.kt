package com.vlifte.autostarttv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseUseCase<in Param, out T> {

    abstract suspend fun run(param: Param): Result<T>

    suspend fun execute(param: Param, onResult: (Result<T>) -> Unit) {
        val funcResult = withContext(Dispatchers.IO) {
            run(param)
        }
        onResult(funcResult)
    }

}

abstract class BaseLocalUseCase<in Param, out T> {

    abstract suspend fun run(param: Param): T

    suspend fun execute(param: Param, onResult: (T) -> Unit) {
        val funcResult = withContext(Dispatchers.IO) {
            run(param)
        }
        onResult(funcResult)
    }

}