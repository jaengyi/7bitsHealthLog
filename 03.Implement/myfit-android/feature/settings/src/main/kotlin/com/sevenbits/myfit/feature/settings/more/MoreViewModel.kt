package com.sevenbits.myfit.feature.settings.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** SCR-CMN-003 마이페이지 상태. (06_화면설계서 §3.3) */
data class MoreUiState(
    val profile: UserProfile? = null,
)

/**
 * 더보기 허브.
 *
 * 프로필 요약만 읽는다. 메뉴 구성은 정적이므로 상태가 아니다.
 */
@HiltViewModel
class MoreViewModel @Inject constructor(
    repository: UserRepository,
) : ViewModel() {

    val uiState = repository.observeProfile()
        .map { MoreUiState(profile = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoreUiState(),
        )
}
