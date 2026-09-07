package com.offlinecurrencyconverter.app.domain.model

sealed interface SyncError {
    data object Network : SyncError
    data object Server : SyncError
    data object InvalidResponse : SyncError
    data object EmptyResponse : SyncError
    data class Http(val code: Int) : SyncError
}

class SyncErrorException(val syncError: SyncError) : Exception(
    when (syncError) {
        SyncError.Network -> "Network connection failed"
        SyncError.Server -> "Server error"
        SyncError.InvalidResponse -> "Invalid exchange rate data"
        SyncError.EmptyResponse -> "Empty response"
        is SyncError.Http -> "HTTP error ${syncError.code}"
    }
)

fun Throwable.isRetryable(): Boolean = when (this) {
    is SyncErrorException -> when (syncError) {
        SyncError.Network -> true
        SyncError.Server -> true
        is SyncError.Http -> syncError.code >= 500
        SyncError.InvalidResponse -> false
        SyncError.EmptyResponse -> false
    }
    is java.io.IOException -> true
    else -> true
}