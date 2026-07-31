package com.sevenbits.myfit.core.domain.repository

/**
 * 앱 최초 실행 초기화 계약. (03_모듈설계서 §6 "AppInitializer 1회성")
 *
 * 종목 마스터 시드 적재가 여기 걸린다. 네트워크 없이도 최초 실행부터 종목을
 * 쓸 수 있어야 하므로(REQ-EXR-001), **어떤 화면이 DB 를 읽기 전에** 끝나야 한다.
 *
 * `:app` 은 `:core:database` 를 직접 보지 않으므로(R-2) 이 계약을 통해 호출한다.
 */
interface AppInitializer {

    /**
     * 이미 적재되어 있으면 아무 것도 하지 않는다(멱등). 매 실행 호출해도 안전하다.
     *
     * @return 새로 적재한 종목 수. 이미 적재된 상태면 0.
     */
    suspend fun initialize(): Int
}
