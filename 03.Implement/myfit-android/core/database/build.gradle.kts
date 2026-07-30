plugins {
    alias(libs.plugins.myfit.android.library)
    alias(libs.plugins.myfit.android.hilt)
    alias(libs.plugins.myfit.android.room)
}

android {
    namespace = "com.sevenbits.myfit.core.database"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
}
