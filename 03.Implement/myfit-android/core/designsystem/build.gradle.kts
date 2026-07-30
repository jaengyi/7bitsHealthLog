plugins {
    alias(libs.plugins.myfit.android.library.compose)
}

android {
    namespace = "com.sevenbits.myfit.core.designsystem"
}

dependencies {
    api(libs.compose.material.icons)
}
