package com.sevenbits.myfit.feature.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.CareerLevel
import com.sevenbits.myfit.core.domain.model.Gender
import com.sevenbits.myfit.core.domain.model.GoalType
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.repository.UserRepository
import com.sevenbits.myfit.core.domain.validation.ProfileValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** SCR-CMN-004 프로필 편집 상태. (06_화면설계서 §3.4) */
data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    /** 생년월일로 산출한 만 나이. 미입력이면 null (FN-CMN-007) */
    val age: Int? = null,
    val heightError: String? = null,
    val birthDateError: String? = null,
)

sealed interface ProfileEditAction {
    data class OnGenderChange(val value: Gender) : ProfileEditAction
    data class OnBirthDateChange(val value: String) : ProfileEditAction
    data class OnHeightChange(val value: Double) : ProfileEditAction
    data class OnCareerChange(val value: CareerLevel) : ProfileEditAction
    data class OnGoalChange(val value: GoalType) : ProfileEditAction
    data class OnNameChange(val value: String) : ProfileEditAction
    data object OnBack : ProfileEditAction
}

/**
 * 프로필 편집.
 *
 * 값 변경은 즉시 저장한다(P5). 다만 **검증에 걸린 값은 저장하지 않는다** —
 * 화면에는 입력한 그대로 남겨 두고 인라인 오류만 띄운다. 입력 중인 값을 되돌려
 * 버리면 사용자가 무엇을 잘못 넣었는지 알 수 없다.
 */
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.findProfile() ?: UserProfile()
            _uiState.update {
                it.copy(isLoading = false, profile = profile, age = ageOf(profile.birthDate))
            }
        }
    }

    fun onAction(action: ProfileEditAction) {
        when (action) {
            is ProfileEditAction.OnGenderChange -> edit { it.copy(gender = action.value) }
            is ProfileEditAction.OnCareerChange -> edit { it.copy(careerLevel = action.value) }
            is ProfileEditAction.OnGoalChange -> edit { it.copy(goalType = action.value) }
            is ProfileEditAction.OnNameChange ->
                edit { it.copy(displayName = action.value.ifBlank { null }) }

            is ProfileEditAction.OnHeightChange -> {
                val error = errorOf(ProfileValidator.validateHeight(action.value))
                _uiState.update { it.copy(heightError = error) }
                edit(skipSave = error != null) { it.copy(heightCm = action.value) }
            }

            is ProfileEditAction.OnBirthDateChange -> {
                val error = errorOf(ProfileValidator.validateBirthDate(action.value))
                _uiState.update {
                    it.copy(birthDateError = error, age = if (error == null) ageOf(action.value) else null)
                }
                edit(skipSave = error != null) { it.copy(birthDate = action.value) }
            }

            ProfileEditAction.OnBack -> Unit
        }
    }

    private fun edit(skipSave: Boolean = false, transform: (UserProfile) -> UserProfile) {
        val updated = transform(_uiState.value.profile)
        _uiState.update { it.copy(profile = updated) }
        if (skipSave) return
        viewModelScope.launch { repository.upsertProfile(updated) }
    }

    private fun errorOf(result: ProfileValidator.Result): String? =
        (result as? ProfileValidator.Result.Invalid)?.message

    private fun ageOf(birthDate: String?): Int? {
        if (birthDate.isNullOrBlank()) return null
        return runCatching { ProfileValidator.calculateAge(LocalDate.parse(birthDate)) }.getOrNull()
    }
}
