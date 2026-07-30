// :core:domain — 순수 Kotlin 모듈.
// Android 플러그인을 적용하지 않음으로써 android.* import 자체가 컴파일 에러가 된다. (P3 / R-1)
plugins {
    alias(libs.plugins.myfit.jvm.library)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(projects.core.common)
}
