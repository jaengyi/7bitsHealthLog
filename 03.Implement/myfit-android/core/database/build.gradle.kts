plugins {
    alias(libs.plugins.myfit.android.library)
    alias(libs.plugins.myfit.android.hilt)
    alias(libs.plugins.myfit.android.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sevenbits.myfit.core.database"

    // Room DAO 테스트를 Robolectric 으로 JVM 에서 실행하기 위해 필요.
    // 실기기·에뮬레이터 없이도 CI 에서 검증할 수 있다.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
