# 03. Implement — 구현

**개인용 운동기록 애플리케이션 (가칭) MyFit**

| 산출물 | 경로 | 상태 |
|---|---|---|
| Android 앱 | [`myfit-android/`](./myfit-android) | 🟡 프로젝트 골격 (Phase 1 진행 중) |
| 백엔드 API | `myfit-api/` | ⬜ 예정 (Phase 2) |

설계 근거는 [02.Design](../02.Design/README.md), 진행 순서는
[08_개발표준및운영정의서 §10](../02.Design/08_개발표준및운영정의서.md) 참조.

---

## myfit-android

### 확정 환경

> 설계서(`02.Design/02_개발환경및빌드정의서.md`)의 버전은 **baseline**이며, 착수 시 실제 저장소에서
> 최신 안정 버전을 조회해 확정했다. 아래가 확정값이며 `gradle/libs.versions.toml` 이 단일 출처다.

| 항목 | 설계 baseline | **확정값** | 사유 |
|---|---|---|---|
| JDK | 17 | **21** (Temurin) | 로컬 설치본. Java 17 바이트코드 타깃은 유지 |
| Gradle | 8.x | **8.13** | AGP 8.13.2 요구 버전, 로컬 캐시 보유 |
| AGP | 8.7.3 | **8.13.2** | 8.x 최신 안정 (§AGP 9 미채택 사유 참조) |
| Kotlin / KSP | 2.1.0 | **2.3.10 / 2.3.10** | KSP 지원 상한이 2.3.10 → Kotlin을 여기에 고정 |
| compileSdk / targetSdk | 35 | **36** | 로컬 SDK에 35·37 미설치 (34 / 36 / 36.1 보유) |
| minSdk | 26 | **26** | REQ-NFR-005 (변경 없음) |
| Compose BOM | 2024.12.01 | **2026.06.01** | 최신 안정 |
| Hilt | 2.53 | **2.58** | 2.59+ 는 AGP 9.0 이상 요구 |
| Room | 2.6.1 | **2.8.4** | 최신 안정 |
| androidx.core | — | **1.18.0** | 1.19.0 은 `minCompileSdk 37` 요구 |
| androidx.lifecycle | — | **2.10.0** | 2.11.0 은 `minCompileSdk 37` 요구 |
| androidx.hilt | — | **1.3.0** | 1.4.0 은 `minCompileSdk 37` 요구 |

### AGP 9 / compileSdk 37 미채택 사유

착수 시점의 최신 라인은 AGP 9.3.1 + compileSdk 37 이지만, 골격 단계에서는 채택하지 않았다.
버전 선택은 아래 **제약 사슬**로 결정된다 — 하나를 올리면 나머지가 연쇄로 끌려온다.

```
로컬 SDK(android-37 미설치)  →  compileSdk 36
        ↓
compileSdk 36  →  core 1.18.0 / lifecycle 2.10.0 / androidx.hilt 1.3.0
        ↓
AGP 8.13.2     →  Hilt 2.58   (2.59+ 는 AGP 9.0 요구)
        ↓
AGP 8.13.2     →  Gradle 8.13 (AGP 9.3.1 은 Gradle 9.5.0 요구)
```

AGP 9 로 올리려면 아래가 함께 필요하다. **별도 과제로 분리한다.**

1. SDK 플랫폼 **android-37** 설치 (현재 cmdline-tools 미설치)
2. Gradle **9.5.0** 이상으로 상향
3. **AGP 9 의 Kotlin 내장 지원 이관** — `org.jetbrains.kotlin.android` 플러그인 적용이 금지되므로
   KSP·Compose·Hilt 적용 경로를 전부 재구성해야 한다
4. AGP 9 DSL 변경 대응 — `CommonExtension` 이 비제네릭이 되고 `defaultConfig { }` 등
   람다 설정 메서드가 제거되어 프로퍼티 접근으로 바꿔야 한다 (본 저장소는 이미 프로퍼티 방식으로 작성됨)

> 버전은 전부 `gradle/libs.versions.toml` 한 곳에 있으므로, 이관 시 이 파일과 Convention Plugin만 손대면 된다.

### 빌드 검증 결과 (2026-07-30)

| 항목 | 결과 |
|---|---|
| `assembleDebug` | ✅ `app/build/outputs/apk/debug/app-debug.apk` (22MB) |
| `testDebugUnitTest` + `:core:domain:test` | ✅ **35건 전체 통과** (실패 0 / 오류 0) |
| Room 스키마 export | ✅ `core/database/schemas/…MyFitDatabase/1.json` |

### 모듈 구성

```
myfit-android/
├── build-logic/convention/     Convention Plugin 7종 (모듈 공통 설정 일원화)
├── gradle/libs.versions.toml   ★ 버전 단일 관리 지점
├── app/                        진입점 · NavHost · 하단 탭 · Hilt 바인딩
├── core/
│   ├── common/                 JVM  — AppResult/AppError, 단위 변환
│   ├── domain/                 JVM  — 모델 · Calculator · Repository 인터페이스 ★순수 Kotlin
│   ├── data/                   AAR  — Repository 구현 (예정)
│   ├── database/               AAR  — Room DB · Entity · DAO
│   ├── network/                AAR  — Retrofit (예정)
│   ├── designsystem/           AAR  — 테마 · 색상 · 타이포
│   └── ui/                     AAR  — 도메인 공용 Composable
└── feature/
    ├── exercise/  workout/  routine/  calendar/  settings/    (Phase 1)
```

**의존 규칙은 Gradle이 강제한다.** `:core:domain` 은 `myfit.jvm.library` 플러그인만 적용하므로
`import android.*` 자체가 컴파일 에러가 된다. (설계 원칙 P3 / 의존 규칙 R-1)

### Convention Plugin

| 플러그인 ID | 적용 대상 | 역할 |
|---|---|---|
| `myfit.android.application` | `:app` | compileSdk/minSdk/targetSdk, Java 17, desugaring |
| `myfit.android.library` | `:core:*` (Android) | 라이브러리 공통 + 테스트 의존성 |
| `myfit.android.library.compose` | Compose 사용 모듈 | Compose 플러그인 + BOM |
| `myfit.android.feature` | `:feature:*` | library.compose + hilt + core 의존 + 네비게이션 |
| `myfit.android.hilt` | DI 사용 모듈 | KSP + Hilt |
| `myfit.android.room` | `:core:database` | Room + KSP + `room.schemaLocation` |
| `myfit.jvm.library` | `:core:common`, `:core:domain` | 순수 Kotlin (Android 차단) |

### 빌드

```bash
cd 03.Implement/myfit-android

./gradlew assembleDebug          # 디버그 APK
./gradlew testDebugUnitTest      # 단위 테스트
./gradlew :core:domain:test      # 도메인 계산 로직만
./gradlew :core:domain:koverHtmlReport   # 커버리지 리포트
```

- Android SDK 경로는 `ANDROID_HOME` 환경변수 또는 `local.properties` 의 `sdk.dir` 로 지정한다.
- `local.properties`, 키스토어(`*.jks`)는 **커밋하지 않는다.** (08_개발표준 §7)

### 현재 구현 범위

**구현 완료**

| 대상 | 내용 |
|---|---|
| `:core:domain` | `OneRepMaxCalculator`(Epley/Brzycki/Lombardi), `VolumeCalculator`, `IntensityCalculator`, `PlateSolver` — **설계서 §17 검증 케이스를 단위 테스트로 작성** |
| `:core:common` | `AppResult`/`AppError`, `UnitConverter`(kg·cm·km 기준 저장) |
| `:core:database` | Room DB v1, `ExerciseEntity` + `ExerciseDao`(초성 검색 쿼리 포함), Hilt 모듈 |
| `:core:designsystem` | 다크 우선 테마, 색상·타이포 토큰 |
| `:app` | Application(Hilt), MainActivity, NavHost, 하단 탭 5종 |
| `:feature:*` | 화면 플레이스홀더 + 네비게이션 그래프 확장 (feature 간 참조 없음) |

**미구현 (후속)**

- Phase 1 잔여 엔티티 12종 및 DAO — `workout_log` / `workout_log_exercise` / `workout_set` 등
- Repository 구현(`:core:data`), 세트 입력 화면, 휴식 타이머 Foreground Service
- 종목 시드 DB(300종) 및 자세 가이드 asset
- ktlint / detekt / CI 파이프라인
