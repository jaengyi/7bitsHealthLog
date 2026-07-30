package com.sevenbits.myfit.core.common

/**
 * 계층 간 결과 전달 규약. (03_애플리케이션모듈설계서 §7)
 *
 * Data 계층은 예외를 위로 던지지 않고 [AppResult.Failure] 로 변환한다.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    /** 네트워크 없음 — 무음 처리 대상 (설계 원칙 P2) */
    data object NoNetwork : AppError

    data class Server(val code: String, val message: String? = null) : AppError

    data object Unauthorized : AppError

    /** 도메인 규칙 위반 — 사용자에게 인라인으로 표시 */
    data class Validation(val field: String, val rule: String) : AppError

    /** 로컬 DB 실패 — 기록 유실 위험이므로 반드시 사용자에게 알린다 */
    data class LocalStorage(val throwable: Throwable) : AppError

    data class Unknown(val throwable: Throwable) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Success) action(data)
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Failure) action(error)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
