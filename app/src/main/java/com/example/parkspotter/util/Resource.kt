package com.example.parkspotter.util

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()

    /** [codigo] = código HTTP cuando el error vino del backend (null si fue de red). */
    data class Error(val message: String, val codigo: Int? = null) : Resource<Nothing>()

    object Loading : Resource<Nothing>()
}

inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> this
    Resource.Loading -> Resource.Loading
}
