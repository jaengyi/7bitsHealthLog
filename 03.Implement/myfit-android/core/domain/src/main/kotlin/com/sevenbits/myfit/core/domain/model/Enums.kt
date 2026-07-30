package com.sevenbits.myfit.core.domain.model

/**
 * 종목 기록 유형 — 유형에 따라 세트 입력 필드가 달라진다.
 * (REQ-EXR-007 / FN-EXR-012 / 04_데이터베이스설계서 §3.10)
 */
enum class RecordType {
    /** ① 중량 + 횟수 — 벤치프레스, 스쿼트 */
    WEIGHT_REPS,

    /** ② 횟수만 — 푸시업, 풀업 */
    REPS_ONLY,

    /** ③ 시간 — 플랭크 */
    TIME,

    /** ④ 거리 + 시간 — 러닝, 사이클 */
    DISTANCE_TIME,

    /** ⑤ 중량 + 시간 — 파머스 워크 */
    WEIGHT_TIME,
}

/** 세트 타입 — WARMUP 은 볼륨·강도 집계에서 제외된다. (REQ-WRK-004 / FN-WRK-013) */
enum class SetType { NORMAL, WARMUP, DROP, FAILURE }

/** 운동일지 상태 (FN-CAL-005 / FN-WRK-019) */
enum class WorkoutStatus { PLANNED, IN_PROGRESS, COMPLETED }

/** 1RM 추정 공식 (FN-RPT-002) */
enum class OneRmFormula { EPLEY, BRZYCKI, LOMBARDI }

/** 추정 1RM 신뢰도 (FN-RPT-003) */
enum class Confidence { HIGH, NORMAL, LOW }

/** 강도 구간 (FN-RPT-006) */
enum class IntensityZone { STRENGTH, HYPERTROPHY, ENDURANCE }

/** 개인기록 유형 (REQ-RPT-006) */
enum class PrType { MAX_WEIGHT, MAX_E1RM, MAX_SET_VOLUME }

/** 운동 종목 대분류 */
enum class ExerciseType { WEIGHT, CARDIO, BODYWEIGHT, STRETCHING }

/** 장비 분류 (REQ-EXR-002) */
enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, ETC }

/** 동작 성격 */
enum class MovementType { COMPOUND, ISOLATION }

/** 동작 패턴 — 밸런스 분석 기준 (FN-RPT-014) */
enum class MovementPattern { PUSH, PULL, SQUAT, HINGE, CARRY, CORE }
