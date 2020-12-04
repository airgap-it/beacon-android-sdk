package it.airgap.beaconsdk.internal.utils

import it.airgap.beaconsdk.exception.UnknownException

internal sealed class InternalResult<T> {
    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    @Throws(Exception::class)
    fun get(): T =
        when (this) {
            is Success -> value
            is Failure -> failWith(cause = error)
        }

    fun getOrNull(): T? =
        when (this) {
            is Success -> value
            is Failure -> null
        }

    fun errorOrNull(): Throwable? =
        when (this) {
            is Success -> null
            is Failure -> error
        }

    fun <S> map(transform: (T) -> S): InternalResult<S> =
        when (this) {
            is Success -> Success(transform(value))
            is Failure -> Failure(error)
        }

    suspend fun <S> mapSuspend(transform: suspend (T) -> S): InternalResult<S> =
        when (this) {
            is Success -> Success(transform(value))
            is Failure -> Failure(error)
        }

    fun mapError(transform: (Throwable) -> Throwable): InternalResult<T> =
        when (this) {
            is Success -> this
            is Failure -> Failure(transform(error))
        }

    suspend fun mapErrorSuspend(transform: suspend (Throwable) -> Throwable): InternalResult<T> =
        when (this) {
            is Success -> this
            is Failure -> Failure(transform(error))
        }

    fun <S> flatMap(transform: (T) -> InternalResult<S>): InternalResult<S> =
        when (this) {
            is Success -> transform(value)
            is Failure -> Failure(error)
        }

    suspend fun <S> flatMapSuspend(transform: suspend (T) -> InternalResult<S>): InternalResult<S> =
        when (this) {
            is Success -> transform(value)
            is Failure -> Failure(error)
        }

    inline fun alsoIfSuccess(block: (T) -> Unit): InternalResult<T> =
        also { it.getOrNull()?.let(block) }
}

internal data class Success<T>(val value: T) : InternalResult<T>()
internal data class Failure<T>(val error: Throwable = UnknownException()) : InternalResult<T>()

internal fun Success(): InternalResult<Unit> = Success(Unit)