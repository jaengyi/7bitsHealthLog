plugins {
    alias(libs.plugins.myfit.android.library)
    alias(libs.plugins.myfit.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sevenbits.myfit.core.network"
    buildFeatures.buildConfig = true
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.timber)
}
