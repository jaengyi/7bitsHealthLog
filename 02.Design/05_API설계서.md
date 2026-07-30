# API 설계서

**개인용 운동기록 애플리케이션 (가칭) MyFit · v1.1**
**서버: Spring Boot 3 + Kotlin / PostgreSQL 16 (개인 Ubuntu 서버, `7bits.mooo.com`)**

> 관련 요구사항: `REQ-DAT-002`(개인 서버 동기화), `REQ-CMN-001`(인증), `REQ-NFR-004`(보안), `REQ-NFR-010`(운영), `REQ-DIE-002`(음식DB)

---

## 1. 설계 방침

| # | 방침 | 설명 |
|---|---|---|
| A1 | **동기화 중심 API** | 앱은 로컬 DB로 완결 동작하므로(P1), 서버 API는 도메인별 CRUD가 아니라 **델타 Push/Pull 2개 엔드포인트**를 축으로 한다. 왕복 횟수와 배터리 소모를 최소화한다. |
| A2 | **멱등성(Idempotency)** | 모든 Push는 레코드 `id`(UUID) 기준 upsert. 네트워크 재시도로 중복 전송되어도 결과가 동일하다. |
| A3 | **서버는 상태를 판단하지 않는다** | 충돌 해소는 `updated_at` 비교(LWW)라는 기계적 규칙만 적용한다. 서버가 도메인 규칙을 재검증하지 않는다(단일 사용자 전제). |
| A4 | **부분 실패 허용** | 배치 중 일부 레코드 실패 시 전체를 롤백하지 않고 `failed[]` 로 반환한다. 앱은 실패분만 재시도한다. |
| A5 | **바이너리 미전송** | 신체·식단 사진은 동기화 대상에서 제외한다(개인 서버 용량). 메타데이터만 전송. |

---

## 2. 공통 규약

### 2.1 기본 정보

| 항목 | 값 |
|---|---|
| Base URL (운영) | `https://7bits.mooo.com/api/v1` |
| Base URL (개발) | `https://dev.7bits.mooo.com/api/v1` |
| 프로토콜 | **HTTPS 전용** (HTTP는 301 리다이렉트) — Let's Encrypt |
| 인코딩 | UTF-8 |
| Content-Type | `application/json` |
| 압축 | `Content-Encoding: gzip` (동기화 배치 필수) |
| 시각 표기 | **ISO-8601 UTC** (`2026-07-30T04:12:33.482Z`) |
| 날짜 표기 | `yyyy-MM-dd` (로컬 날짜, 타임존 미적용) |
| 버전 정책 | URL 경로 버전(`/v1`). 하위 호환 불가 변경 시에만 `/v2` 신설 |

### 2.2 요청 헤더

| 헤더 | 필수 | 설명 |
|---|:--:|---|
| `Authorization: Bearer {accessToken}` | ● | 인증 API 제외 전 엔드포인트 |
| `X-Device-Id` | ● | 단말 식별자(설치 시 생성 UUID). 다기기 동기화 추적 |
| `X-App-Version` | ● | `1.0.0` — 서버 측 호환성 판단 |
| `X-Schema-Version` | ● | 로컬 DB 버전(정수). 서버 스키마와 불일치 시 `426` 응답 |

### 2.3 응답 포맷

**성공**

```json
{
  "success": true,
  "data": { },
  "serverTime": "2026-07-30T04:12:33.482Z"
}
```

**실패**

```json
{
  "success": false,
  "error": {
    "code": "SYNC_SCHEMA_MISMATCH",
    "message": "클라이언트 스키마 버전(2)이 서버(3)보다 낮습니다.",
    "field": null
  },
  "serverTime": "2026-07-30T04:12:33.482Z"
}
```

### 2.4 HTTP 상태 코드

| 코드 | 사용 상황 | 앱 처리 |
|---|---|---|
| `200` | 조회·동기화 성공 | 정상 처리 |
| `201` | 리소스 생성 | |
| `204` | 변경분 없음 (Pull 결과 공백) | 동기화 종료 |
| `400` | 요청 형식 오류 | 로그 기록, 재시도 안 함 |
| `401` | 토큰 만료/무효 | **Refresh 시도 → 실패 시 로그아웃** |
| `403` | 타 사용자 리소스 접근 | 로그 기록 |
| `404` | 리소스 없음 | |
| `409` | 동기화 충돌(해소 불가) | 서버 확정본 채택 |
| `413` | 배치 크기 초과 | 배치 분할 후 재시도 |
| `426` | 스키마 버전 불일치 | 앱 업데이트 안내 |
| `429` | 요청 과다 | 지수 백오프 |
| `500` / `503` | 서버 오류 | **무음 처리 + 백오프 재시도** (P2 원칙) |

### 2.5 에러 코드

| 코드 | HTTP | 설명 |
|---|:--:|---|
| `AUTH_INVALID_TOKEN` | 401 | Access Token 무효·만료 |
| `AUTH_REFRESH_EXPIRED` | 401 | Refresh Token 만료 → 재로그인 필요 |
| `AUTH_PROVIDER_FAILED` | 401 | 소셜 ID 토큰 검증 실패 |
| `AUTH_FORBIDDEN` | 403 | 타 사용자 리소스 |
| `VALIDATION_FAILED` | 400 | 필드 검증 실패 (`field` 에 대상 명시) |
| `SYNC_BATCH_TOO_LARGE` | 413 | 배치 레코드 수/크기 초과 |
| `SYNC_SCHEMA_MISMATCH` | 426 | `X-Schema-Version` 불일치 |
| `SYNC_UNKNOWN_ENTITY` | 400 | 미지원 엔티티명 |
| `RATE_LIMITED` | 429 | 요청 한도 초과 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## 3. 제한 사항

| 항목 | 제한 | 초과 시 |
|---|---|---|
| 요청 본문 크기 | **10MB** (nginx `client_max_body_size`) | `413` |
| Push 배치 레코드 수 | 엔티티당 **500건**, 전체 **2,000건** | `413` → 앱이 분할 재전송 |
| Pull 페이지 크기 | 기본 500건, 최대 1,000건 | 커서 페이징 |
| Rate Limit | 사용자당 **60 req/min** | `429` |
| Access Token TTL | **30분** | |
| Refresh Token TTL | **30일** (사용 시 회전) | |

---

## 4. 인증 API

### 4.1 `POST /auth/social-login` — 소셜 로그인 (FN-CMN-002)

**Request**

```json
{
  "provider": "GOOGLE",
  "idToken": "eyJhbGciOi...",
  "deviceId": "0f8b2c1e-....",
  "deviceInfo": "SM-S928N / Android 15"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|:--:|---|
| `provider` | enum | ● | `GOOGLE` / `KAKAO` |
| `idToken` | string | ● | OAuth 제공자 ID 토큰 |
| `deviceId` | string | ● | 단말 UUID |

**Response 200**

```json
{
  "success": true,
  "data": {
    "userId": "7a1c...",
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "accessExpiresIn": 1800,
    "isNewUser": false,
    "profile": { "email": "...", "displayName": "..." }
  }
}
```

**처리 흐름**

```
① provider ID 토큰 서명·audience·만료 검증 (Google/Kakao 공개키)
② provider_user_id 로 app_user 조회 → 없으면 신규 생성 (isNewUser=true)
③ Access(30분) / Refresh(30일) JWT 발급, refresh_token 테이블에 해시 저장
④ 앱: EncryptedSharedPreferences 에 토큰 저장 (REQ-NFR-004)
```

### 4.2 `POST /auth/refresh` — 토큰 갱신 (FN-CMN-004)

```json
// Request
{ "refreshToken": "eyJ...", "deviceId": "0f8b..." }
// Response 200
{ "success": true, "data": { "accessToken": "eyJ...", "refreshToken": "eyJ...", "accessExpiresIn": 1800 } }
```

> **Refresh Token 회전**: 사용 즉시 기존 토큰을 `revoked_at` 처리하고 신규 발급한다. 탈취 토큰 재사용 시 해당 사용자 전 토큰을 폐기한다.

### 4.3 `POST /auth/logout`

```json
{ "refreshToken": "eyJ...", "deviceId": "0f8b..." }
```
→ `204`. 서버는 해당 Refresh Token만 폐기. 로컬은 토큰 삭제 후 **데이터는 유지**(게스트 모드로 계속 사용 가능).

### 4.4 `POST /auth/claim-guest` — 게스트 데이터 승계 (FN-CMN-003)

```json
{ "guestLocalUserId": "9c2f...", "recordCount": 1247 }
```

**Response 200**

```json
{ "success": true, "data": { "userId": "7a1c...", "claimed": true } }
```

> 실제 데이터 이동은 별도 API가 아니라 **앱이 로컬 `user_id` 를 일괄 갱신한 뒤 `is_dirty=1` 로 만들어 정규 Sync Push 로 업로드**한다.
> 이 엔드포인트는 승계 허용 여부 확인 및 서버 계정 준비만 담당한다.

---

## 5. 동기화 API ★ 핵심

### 5.1 프로토콜 개요

```
     [앱]                                          [서버]
       │                                              │
       │ ① POST /sync/push  (로컬 변경분 + tombstone)    │
       ├─────────────────────────────────────────────▶│
       │                                              │  updated_at 비교 (LWW)
       │◀─────────────────────────────────────────────┤
       │   accepted[] / rejected[] / conflicted[]      │
       │                                              │
       │ ② GET /sync/pull?since={lastSyncedAt}         │
       ├─────────────────────────────────────────────▶│
       │◀─────────────────────────────────────────────┤
       │   entities{...} + deletions[] + nextCursor    │
       │                                              │
   Room upsert / 삭제 반영                              │
   is_dirty=0, sync_state.last_synced_at 갱신           │
```

**순서가 중요하다**: 반드시 **Push → Pull** 순으로 수행한다. 반대 순서는 로컬 미전송 변경분이 서버 값으로 덮여 유실될 수 있다.

### 5.2 `POST /sync/push` — 변경분 업로드 (FN-DAT-002, FN-DAT-004)

**Request**

```json
{
  "deviceId": "0f8b2c1e-...",
  "clientTime": "2026-07-30T04:12:33.482Z",
  "entities": {
    "workout_log": [
      {
        "id": "3f2a...",
        "workoutDate": "2026-07-29",
        "sessionNo": 1,
        "status": "COMPLETED",
        "routineId": "9b1e...",
        "routineNameSnapshot": "가슴/삼두",
        "startedAt": "2026-07-29T10:02:11.000Z",
        "endedAt": "2026-07-29T11:14:50.000Z",
        "totalVolumeKg": 8420.0,
        "totalSetCount": 21,
        "memo": "벤치 폼 안정적",
        "createdAt": "2026-07-29T10:02:11.000Z",
        "updatedAt": "2026-07-29T11:15:02.000Z"
      }
    ],
    "workout_set": [
      {
        "id": "77c1...",
        "logExerciseId": "5d3b...",
        "setNo": 3,
        "setType": "NORMAL",
        "weightKg": 80.0,
        "reps": 8,
        "rpe": 8.5,
        "isCompleted": true,
        "completedAt": "2026-07-29T10:31:02.000Z",
        "createdAt": "2026-07-29T10:28:00.000Z",
        "updatedAt": "2026-07-29T10:31:02.000Z"
      }
    ]
  },
  "deletions": [
    { "entityName": "workout_set", "targetId": "aa19...", "deletedAt": "2026-07-29T10:44:00.000Z" }
  ]
}
```

**Response 200**

```json
{
  "success": true,
  "data": {
    "accepted":   [ { "entityName": "workout_log", "id": "3f2a...", "serverUpdatedAt": "2026-07-29T11:15:02.000Z" } ],
    "conflicted": [ { "entityName": "workout_set", "id": "77c1...", "reason": "SERVER_NEWER",
                      "serverRecord": { "id": "77c1...", "weightKg": 82.5, "updatedAt": "2026-07-29T12:00:00.000Z" } } ],
    "rejected":   [ { "entityName": "workout_set", "id": "bb02...", "code": "VALIDATION_FAILED", "message": "reps 범위 초과" } ],
    "deletionsApplied": 1,
    "serverSyncedAt": "2026-07-30T04:12:34.100Z"
  }
}
```

**서버 처리 규칙 (레코드 단위)**

| 상황 | 처리 | 응답 분류 |
|---|---|---|
| 서버에 없음 | INSERT | `accepted` |
| 서버에 있고 `client.updatedAt > server.updatedAt` | UPDATE | `accepted` |
| 서버에 있고 `client.updatedAt < server.updatedAt` | **무시** + 서버 레코드 반환 | `conflicted` (앱이 서버본 채택) |
| `client.updatedAt == server.updatedAt` | 무시 (동일본) | `accepted` |
| 필드 검증 실패 | 무시 | `rejected` (앱은 재전송하지 않고 로그만) |
| `deletions` 항목 | 서버 레코드 물리 삭제 + 서버 tombstone 기록 | `deletionsApplied` |

> **삭제 우선 원칙**: 동일 레코드에 대해 삭제와 수정이 충돌하면 **삭제를 우선**한다.
> (되살아난 레코드보다 삭제 유지가 사용자 의도에 부합)

**앱 후처리**

```
accepted   → is_dirty = 0
conflicted → 서버 레코드로 로컬 덮어쓰기, is_dirty = 0
rejected   → is_dirty = 0 (무한 재시도 방지) + Timber 경고 로그
```

### 5.3 `GET /sync/pull` — 변경분 다운로드 (FN-DAT-003)

**Request**

```
GET /sync/pull?since=2026-07-29T00:00:00.000Z&limit=500&cursor=&entities=workout_log,workout_set
```

| 파라미터 | 필수 | 설명 |
|---|:--:|---|
| `since` | ● | `sync_state.last_synced_at`. 최초 동기화는 생략(전체 조회) |
| `limit` | | 기본 500, 최대 1,000 |
| `cursor` | | 이전 응답의 `nextCursor` |
| `entities` | | 특정 엔티티만 조회(쉼표 구분). 미지정 시 전체 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "entities": {
      "workout_log": [ { "id": "...", "updatedAt": "..." } ],
      "workout_set": [ { "id": "...", "updatedAt": "..." } ]
    },
    "deletions": [ { "entityName": "workout_set", "targetId": "aa19...", "deletedAt": "..." } ],
    "nextCursor": "eyJ0IjoxNzUx...",
    "hasMore": true,
    "serverSyncedAt": "2026-07-30T04:12:35.000Z"
  }
}
```

**앱 처리**

```
① deletions 먼저 반영 (로컬 삭제)          ※ 순서 중요 — 삭제 후 upsert 시 되살아남 방지
② entities 를 부모→자식 순서로 upsert
   user → exercise → workout_log → workout_log_exercise → workout_set
   (FK 제약 위반 방지)
③ hasMore=true 이면 nextCursor 로 반복
④ 완료 시 sync_state.last_synced_at = serverSyncedAt
```

> `since` 는 **서버 시각 기준**이다. 단말 시계 오차로 인한 누락을 막기 위해 앱은 자신의 시계를 기준으로 삼지 않고
> 응답의 `serverSyncedAt` 을 그대로 저장한다.

### 5.4 `GET /sync/status` — 동기화 상태 조회

```json
{
  "success": true,
  "data": {
    "lastSyncedAt": "2026-07-30T04:12:35.000Z",
    "serverSchemaVersion": 3,
    "recordCounts": { "workout_log": 412, "workout_set": 8930 }
  }
}
```

설정 화면의 "마지막 동기화" 표시 및 기기 이전(FN-DAT-010) 전 건수 대조에 사용한다.

### 5.5 동기화 대상 엔티티

| 엔티티명 | 동기화 | 비고 |
|---|:--:|---|
| `user`, `user_setting` | ● | |
| `exercise` (사용자 정의만) | ● | 기본 종목은 앱 내장이므로 미전송 |
| `exercise_favorite`, `exercise_note` | ● | |
| `exercise_reference` | △ | **`is_default=0`(사용자 등록 링크)만 전송**. 기본 제공 영상 링크는 앱 내장 시드 |
| `workout_log`, `workout_log_exercise`, `workout_set` | ● | 최대 볼륨 |
| `routine`, `routine_exercise`, `routine_version` | ● | |
| `program`, `program_routine`, `periodization_rule` | ● | |
| `personal_record` | ● | |
| `body_composition`, `body_measurement` | ● | |
| `body_photo` | △ | **메타데이터만**. 파일 미전송 |
| `food`(사용자 등록), `food_set`, `food_set_item`, `diet_log`, `diet_log_item` | ● | Phase 3 |
| `user_plate` | ● | |
| `body_part` | ✕ | 앱 내장 마스터 |
| `sync_state`, `note_fts` | ✕ | 로컬 전용 |

---

## 6. 마스터 데이터 API

### 6.1 `GET /master/exercises` — 기본 종목 갱신 (FN-EXR-001 보조)

```
GET /master/exercises?since=2026-01-01T00:00:00.000Z
```

앱 내장 시드 이후 추가·수정된 기본 종목과 **기본 제공 자세 영상 링크(`exercise_reference`, `is_default=1`)** 를 증분 제공한다.
미호출해도 앱은 정상 동작한다(선택적).

```json
{
  "success": true,
  "data": {
    "exercises": [
      { "id": "ex-0012", "name": "벤치프레스", "guideImageAsset": "guide/bench_press.webp", "updatedAt": "..." }
    ],
    "references": [
      { "id": "ref-0031", "exerciseId": "ex-0012", "refType": "VIDEO", "provider": "YOUTUBE",
        "videoId": "xxxxxxxxxxx", "startSec": 12, "title": "벤치프레스 기본 자세", "isDefault": true }
    ]
  }
}
```

> **자세 가이드 이미지 파일 자체는 API로 전송하지 않는다.** 앱 내장 asset이 원본이며,
> 서버는 asset 경로 문자열만 갱신한다. 신규 이미지 추가는 앱 업데이트로 배포한다.

### 6.2 `GET /master/foods` — 음식 검색 (FN-DIE-004, Phase 3)

```
GET /master/foods?q=닭가슴살&limit=30&cursor=
```

```json
{
  "success": true,
  "data": {
    "items": [
      { "id": "f-1029", "name": "닭가슴살(생것)", "servingG": 100,
        "kcal": 106.0, "carbG": 0.0, "proteinG": 22.97, "fatG": 1.42, "source": "MFDS" }
    ],
    "nextCursor": null
  }
}
```

> 식약처 식품영양성분 공공데이터를 서버에 적재하여 제공한다(REQ-DIE-002).
> **상용 앱의 음식 DB를 복제하지 않는다** (REQ-NFR-009).

---

## 7. 운영 API

| 엔드포인트 | 인증 | 설명 |
|---|:--:|---|
| `GET /actuator/health` | 불필요 | 컨테이너 healthcheck (내부 바인딩만 노출) |
| `GET /meta/version` | 불필요 | `{ "apiVersion":"v1", "schemaVersion":3, "minAppVersion":"1.0.0" }` |

> `minAppVersion` 미만 앱은 업데이트를 안내한다. `/actuator/*` 는 nginx에서 외부 차단한다.

---

## 8. 보안 설계 (REQ-NFR-004)

| 항목 | 설계 |
|---|---|
| 전송 구간 | TLS 1.2/1.3 강제, HSTS `max-age=31536000` |
| 인증 방식 | JWT (HS256). 시크릿은 서버 `.env` (600 권한), 최소 32바이트 |
| Access Token | 30분. 바디·URL이 아닌 `Authorization` 헤더로만 전달 |
| Refresh Token | 30일, **회전 방식**. 서버는 원문이 아닌 **해시**만 저장 |
| 토큰 보관(앱) | `EncryptedSharedPreferences` (Android Keystore 기반) |
| 인가 | 모든 쿼리에 `user_id = {JWT subject}` 조건 강제. 서비스 계층 공통 필터로 적용 |
| 입력 검증 | Bean Validation + 도메인 범위 검증(중량 0~500kg, 횟수 1~200 등) |
| 로깅 | 요청 본문 로깅 금지(신체 정보 포함). ID·엔티티명·건수만 기록 |
| CORS | **미허용** (웹 클라이언트 없음) |
| Rate Limit | nginx `limit_req` + 앱 레벨 60 req/min |
| 사진 | 서버 미전송. 앱 내부 저장소에만 보관, EXIF 제거 후 저장 |

**OkHttp 인터셉터 구성 (앱)**

```kotlin
OkHttpClient.Builder()
    .addInterceptor(HeaderInterceptor())        // Authorization, X-Device-Id, X-App-Version, X-Schema-Version
    .authenticator(TokenAuthenticator())        // 401 → /auth/refresh 자동 재시도 (1회 한정)
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) BODY else NONE   // release 에서 본문 로깅 차단
    })
    .connectTimeout(10, SECONDS)
    .readTimeout(30, SECONDS)                   // 동기화 배치 고려
    .build()
```

---

## 9. 서버 구현 구조 (Spring Boot 3 + Kotlin)

```
myfit-api/
├── src/main/kotlin/com/sevenbits/myfit/
│   ├── MyFitApiApplication.kt
│   ├── config/          SecurityConfig, JwtConfig, JacksonConfig, WebConfig
│   ├── security/        JwtProvider, JwtAuthenticationFilter, CurrentUserResolver
│   ├── auth/
│   │   ├── controller/  AuthController
│   │   ├── service/     AuthService, GoogleTokenVerifier, KakaoTokenVerifier
│   │   └── dto/         SocialLoginRequest, TokenResponse
│   ├── sync/            ★ 핵심
│   │   ├── controller/  SyncController          (push / pull / status)
│   │   ├── service/     SyncService, ConflictResolver(LWW), EntityRegistry
│   │   ├── dto/         SyncPushRequest, SyncPullResponse, SyncRecordDto
│   │   └── handler/     엔티티별 UpsertHandler (workout_log, workout_set …)
│   ├── master/          ExerciseMasterController, FoodController
│   ├── domain/          JPA Entity + Repository (스키마는 04_데이터베이스설계서 §6)
│   └── common/          ApiResponse, ErrorCode, GlobalExceptionHandler
├── src/main/resources/
│   ├── application.yml / application-prod.yml
│   └── db/migration/    V1__init.sql, V2__add_diet.sql   (Flyway)
└── Dockerfile
```

**`EntityRegistry` 설계** — 엔티티가 30종이므로 개별 컨트롤러 대신 **레지스트리 패턴**으로 일원화한다.

```kotlin
interface SyncEntityHandler<T : Any> {
    val entityName: String                        // "workout_log"
    fun upsert(userId: UUID, dto: JsonObject): UpsertResult   // LWW 판정 포함
    fun findUpdatedSince(userId: UUID, since: Instant, limit: Int, cursor: String?): PagedRecords
    fun delete(userId: UUID, targetId: UUID, deletedAt: Instant)
}

@Service
class SyncService(handlers: List<SyncEntityHandler<*>>) {
    private val registry = handlers.associateBy { it.entityName }
    // push/pull 시 entityName 으로 위임 → 엔티티 추가 시 Handler 1개만 구현하면 됨
}
```

> 이 구조로 Phase 3 식단 엔티티 추가 시 **Sync 컨트롤러·서비스 수정 없이** Handler 구현체만 추가하면 된다.

---

## 10. 앱 측 Retrofit 인터페이스

```kotlin
interface MyFitApi {
    @POST("auth/social-login")
    suspend fun socialLogin(@Body body: SocialLoginRequest): ApiResponse<TokenResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): ApiResponse<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>

    @POST("sync/push")
    suspend fun push(@Body body: SyncPushRequest): ApiResponse<SyncPushResponse>

    @GET("sync/pull")
    suspend fun pull(
        @Query("since") since: String?,
        @Query("limit") limit: Int = 500,
        @Query("cursor") cursor: String? = null,
    ): ApiResponse<SyncPullResponse>

    @GET("sync/status")
    suspend fun syncStatus(): ApiResponse<SyncStatusResponse>

    @GET("master/foods")
    suspend fun searchFoods(
        @Query("q") query: String,
        @Query("limit") limit: Int = 30,
    ): ApiResponse<FoodSearchResponse>
}
```

---

## 11. 동기화 시나리오별 동작

| 시나리오 | 동작 |
|---|---|
| **헬스장 지하(오프라인) 기록** | 전 기록이 `is_dirty=1` 로 로컬 적재. UI에 동기화 관련 표시 없음(P2) |
| **네트워크 복귀** | `SyncWorker` 가 제약 충족으로 실행 → Push → Pull |
| **두 기기에서 같은 세트 수정** | `updated_at` 늦은 쪽 채택. 진 쪽 기기는 다음 Pull에서 서버본으로 덮어씀 |
| **기기 A에서 삭제, 기기 B에서 수정** | 삭제 우선 — B의 수정본은 Pull 시 삭제 반영으로 제거 |
| **신규 기기 이전 (FN-DAT-010)** | 로그인 → `since` 없이 전체 Pull → UUID 기준 upsert이므로 **중복 없음** (REQ-DAT-004) |
| **서버 장기 다운** | 로컬 계속 사용. `fail_count` 누적 시 설정 화면에만 배지 표시 |
| **앱 재설치(백업 복구)** | 백업 JSON 복구 후 Push → 서버와 UUID 기준 병합 |

---

## 12. 관련 문서

- [01_시스템아키텍처설계서.md](./01_시스템아키텍처설계서.md) §4.2 — 동기화 데이터 흐름
- [04_데이터베이스설계서.md](./04_데이터베이스설계서.md) §6 — 서버 PostgreSQL 스키마
- [02_개발환경및빌드정의서.md](./02_개발환경및빌드정의서.md) §7 — Docker Compose·nginx 구성
- [08_개발표준및운영정의서.md](./08_개발표준및운영정의서.md) — 배포·운영 절차
