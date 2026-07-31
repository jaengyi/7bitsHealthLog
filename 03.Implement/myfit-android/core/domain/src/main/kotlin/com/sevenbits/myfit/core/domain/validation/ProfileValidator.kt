package com.sevenbits.myfit.core.domain.validation

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

/**
 * 프로필 입력 검증. (FN-CMN-005/007 / 07_핵심로직설계서 §16)
 *
 * 검증 규칙은 도메인에 한 곳만 둔다. UI 와 DB 양쪽에 중복 구현하면 언젠가 어긋난다.
 */
object ProfileValidator {

    /** 키 범위 — 이 밖의 값은 오입력으로 본다 */
    const val MIN_HEIGHT_CM = 100.0
    const val MAX_HEIGHT_CM = 250.0

    private const val MIN_AGE = 10
    private const val MAX_AGE = 100

    sealed interface Result {
        data object Valid : Result
        data class Invalid(val message: String) : Result
    }

    fun validateHeight(heightCm: Double?): Result = when {
        heightCm == null -> Result.Valid // 미입력 허용 — 분석 시점에 안내한다
        heightCm < MIN_HEIGHT_CM || heightCm > MAX_HEIGHT_CM ->
            Result.Invalid("키는 ${MIN_HEIGHT_CM.toInt()}~${MAX_HEIGHT_CM.toInt()}cm 범위로 입력해 주세요")
        else -> Result.Valid
    }

    /**
     * 생년월일 검증. `yyyy-MM-dd` 형식이어야 하며 만 나이가 상식 범위여야 한다.
     */
    fun validateBirthDate(birthDate: String?, today: LocalDate = LocalDate.now()): Result {
        if (birthDate.isNullOrBlank()) return Result.Valid

        val parsed = try {
            LocalDate.parse(birthDate)
        } catch (e: DateTimeParseException) {
            return Result.Invalid("날짜 형식이 올바르지 않습니다 (예: 1990-01-31)")
        }

        if (parsed.isAfter(today)) return Result.Invalid("미래 날짜는 입력할 수 없습니다")

        val age = calculateAge(parsed, today)
        return if (age < MIN_AGE || age > MAX_AGE) {
            Result.Invalid("생년월일을 다시 확인해 주세요")
        } else {
            Result.Valid
        }
    }

    /**
     * 만 나이 산출. (FN-CMN-007)
     *
     * 생일이 지나지 않았으면 1을 뺀다. `dayOfYear` 를 비교하면 윤년이 낀 해에 하루가
     * 어긋나므로 [Period] 로 계산한다.
     */
    fun calculateAge(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Int =
        Period.between(birthDate, today).years
}
