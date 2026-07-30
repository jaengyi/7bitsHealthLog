plugins {
    alias(libs.plugins.myfit.android.library.compose)
}

android {
    namespace = "com.sevenbits.myfit.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.domain)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
