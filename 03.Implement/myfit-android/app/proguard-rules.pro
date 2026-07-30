# ── kotlinx.serialization ─────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.sevenbits.myfit.**$$serializer { *; }
-keepclasseswithmembers class com.sevenbits.myfit.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Room ──────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ── Retrofit / OkHttp ─────────────────────────────────
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**

# ── 백업 JSON DTO 는 필드명이 파일 포맷 계약이므로 난독화 제외 ──
# 구버전 백업 파일 복구 불가를 방지한다. (FN-DAT-006/007)
-keep class com.sevenbits.myfit.core.data.backup.model.** { *; }

# ── 크래시 로그 가독성 ─────────────────────────────────
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
