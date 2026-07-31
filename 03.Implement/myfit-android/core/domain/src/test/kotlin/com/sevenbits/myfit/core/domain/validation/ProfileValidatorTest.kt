package com.sevenbits.myfit.core.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileValidatorTest {

    private val today = LocalDate.of(2026, 7, 30)

    // ── 키 ───────────────────────────────────────────────

    @Test
    fun `키 미입력은 허용한다`() {
        assertTrue(ProfileValidator.validateHeight(null) is ProfileValidator.Result.Valid)
    }

    @Test
    fun `키 경계값 100과 250은 유효하다`() {
        assertTrue(ProfileValidator.validateHeight(100.0) is ProfileValidator.Result.Valid)
        assertTrue(ProfileValidator.validateHeight(250.0) is ProfileValidator.Result.Valid)
    }

    @Test
    fun `키가 범위를 벗어나면 오류다`() {
        assertTrue(ProfileValidator.validateHeight(99.9) is ProfileValidator.Result.Invalid)
        assertTrue(ProfileValidator.validateHeight(250.1) is ProfileValidator.Result.Invalid)
    }

    // ── 생년월일 ──────────────────────────────────────────

    @Test
    fun `생년월일 미입력은 허용한다`() {
        assertTrue(ProfileValidator.validateBirthDate(null, today) is ProfileValidator.Result.Valid)
        assertTrue(ProfileValidator.validateBirthDate("", today) is ProfileValidator.Result.Valid)
    }

    @Test
    fun `형식이 어긋나면 오류다`() {
        assertTrue(
            ProfileValidator.validateBirthDate("1990-13-01", today)
                is ProfileValidator.Result.Invalid,
        )
        assertTrue(
            ProfileValidator.validateBirthDate("19900101", today)
                is ProfileValidator.Result.Invalid,
        )
    }

    @Test
    fun `미래 날짜는 오류다`() {
        assertTrue(
            ProfileValidator.validateBirthDate("2026-07-31", today)
                is ProfileValidator.Result.Invalid,
        )
    }

    @Test
    fun `만 나이 10세 미만 또는 100세 초과는 오류다`() {
        // 2017-07-30 생 -> 만 9세
        assertTrue(
            ProfileValidator.validateBirthDate("2017-07-30", today)
                is ProfileValidator.Result.Invalid,
        )
        // 1925-01-01 생 -> 만 101세
        assertTrue(
            ProfileValidator.validateBirthDate("1925-01-01", today)
                is ProfileValidator.Result.Invalid,
        )
    }

    @Test
    fun `상식 범위 안의 생년월일은 유효하다`() {
        assertTrue(
            ProfileValidator.validateBirthDate("1990-01-31", today)
                is ProfileValidator.Result.Valid,
        )
    }

    // ── 만 나이 ──────────────────────────────────────────

    @Test
    fun `생일이 지났으면 연도 차 그대로다`() {
        assertEquals(36, ProfileValidator.calculateAge(LocalDate.of(1990, 1, 31), today))
    }

    @Test
    fun `생일 당일에는 이미 나이를 먹은 것으로 본다`() {
        assertEquals(36, ProfileValidator.calculateAge(LocalDate.of(1990, 7, 30), today))
    }

    /** dayOfYear 비교로 구현하면 윤년이 낀 해에 하루가 어긋난다 */
    @Test
    fun `윤년이 끼어도 생일 당일 나이는 정확하다`() {
        assertEquals(
            26,
            ProfileValidator.calculateAge(LocalDate.of(2000, 3, 1), LocalDate.of(2026, 3, 1)),
        )
    }

    @Test
    fun `생일이 아직이면 1을 뺀다`() {
        assertEquals(35, ProfileValidator.calculateAge(LocalDate.of(1990, 7, 31), today))
        assertEquals(35, ProfileValidator.calculateAge(LocalDate.of(1990, 12, 31), today))
    }
}
