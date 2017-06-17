package net.zdremann.util

class AsyncTaskResult<out T> private constructor(val value: T?, val error: Exception?) {
    companion object {
        @JvmStatic
        fun <T> value(value: T): AsyncTaskResult<T> = AsyncTaskResult(value, null)

        @JvmStatic
        fun <T> error(error: Exception): AsyncTaskResult<T> = AsyncTaskResult(null, error)
    }

    val isValue: Boolean
        get() = error == null
}
