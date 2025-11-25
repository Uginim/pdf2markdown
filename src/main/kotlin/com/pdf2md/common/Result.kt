package com.pdf2md.common

/**
 * 함수형 에러 처리를 위한 Result 타입
 *
 * 성공 또는 실패를 나타내며, 예외를 사용하지 않고 에러를 처리할 수 있게 합니다.
 */
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()

    /**
     * 성공 값을 변환합니다.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(value))
            is Error -> this
        }
    }

    /**
     * 성공 값을 다른 Result로 변환합니다.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(value)
            is Error -> this
        }
    }

    /**
     * 성공 시 액션을 수행합니다.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }

    /**
     * 실패 시 액션을 수행합니다.
     */
    inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, cause)
        return this
    }

    /**
     * 성공 값을 반환하거나, 실패 시 기본값을 반환합니다.
     */
    inline fun getOrElse(default: (String) -> @UnsafeVariance T): T {
        return when (this) {
            is Success -> value
            is Error -> default(message)
        }
    }

    /**
     * 성공 값을 반환하거나, 실패 시 null을 반환합니다.
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> value
            is Error -> null
        }
    }

    /**
     * 성공 여부를 반환합니다.
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * 실패 여부를 반환합니다.
     */
    fun isError(): Boolean = this is Error

    companion object {
        /**
         * 예외를 발생시킬 수 있는 블록을 Result로 감쌉니다.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e.message ?: "Unknown error", e)
            }
        }
    }
}
