plugins {
    alias(libs.plugins.myfit.android.library)
    alias(libs.plugins.myfit.android.hilt)
}

android {
    namespace = "com.sevenbits.myfit.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.network)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.timber)
}
