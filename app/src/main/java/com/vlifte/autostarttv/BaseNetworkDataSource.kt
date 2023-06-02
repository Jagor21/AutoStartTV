package com.vlifte.autostarttv

import android.util.Log
import com.vlifte.autostarttv.Result.Companion.SOCKET_TIMEOUT_EXCEPTION
import org.json.JSONObject
import retrofit2.HttpException
import org.json.JSONException
import java.lang.Exception

open class BaseNetworkDataSource {

    suspend inline fun <T> execute(crossinline block: suspend () -> T): Result<T> {
        return try {
            val response = block.invoke()
            Log.d("AD_RESPONSE", "SUCCESS: BaseNetworkDataSource execute() $response")
            Result.Success(response)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    Log.d("AD_RESPONSE", "FAILURE: BaseNetworkDataSource execute() $e")
                    var obj: JSONObject? = null
                    var errorsPairs: MutableList<Pair<String, String>>? = null
                    e.response()?.errorBody()?.toString()?.let {
                        try {
                            obj = JSONObject(it)
                        } catch (e: JSONException) {
                            Log.d("BaseNetworkDataSource", e.toString())
                        }
                    }
                    obj?.let {
                        val errors = it.getJSONObject("errors")
                        val keys = errors.keys()
                        errorsPairs = mutableListOf()
                        keys.forEach { key ->
                            errorsPairs!!.add(
                                Pair(
                                    key,
                                    errors.getString(key)
                                        .replace("[\"", "")
                                        .replace("\"]", "")
                                )
                            )
                        }
                    }
                    Result.ErrorResult.NetworkErrorResponse(
                        code = e.code(),
                        errorMessage = e.message(),
                        errorsList = errorsPairs
                    )
                }
                else -> {
                    e.printStackTrace()
                    Result.ErrorResult.NetworkErrorResponse(
                        code = SOCKET_TIMEOUT_EXCEPTION,
                        errorMessage = e.message ?: "",
                        errorsList = null
                    )
                }
            }
        }
    }
}