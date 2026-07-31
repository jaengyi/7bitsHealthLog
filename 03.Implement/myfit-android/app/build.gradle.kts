import java.util.Properties

plugins {
    alias(libs.plugins.myfit.android.application)
    alias(libs.plugins.myfit.android.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * 릴리즈 서명 정보. (08_개발표준및운영정의서 §7)
 *
 * 키스토어와 비밀번호는 **저장소에 커밋하지 않는다**. 다음 순서로 찾는다.
 *   1. `myfit-android/keystore.properties` (로컬 개발자 — .gitignore 대상)
 *   2. 환경변수 `MYFIT_STORE_FILE` / `MYFIT_STORE_PASSWORD` / `MYFIT_KEY_ALIAS` /
 *      `MYFIT_KEY_PASSWORD` (CI Secret)
 *
 * 둘 다 없으면 릴리즈 빌드는 **debug 키로 서명된다**. 키가 없다고 빌드가 깨지면
 * 새로 받은 사람이 프로젝트를 열어 볼 수조차 없다. 대신 경고를 남긴다.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "MYFIT_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "MYFIT_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "MYFIT_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "MYFIT_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it != null } && file(releaseStoreFile!!).exists()

android {
    namespace = "com.sevenbits.myfit"

    defaultConfig {
        applicationId = "com.sevenbits.myfit"
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // v1 은 뺀다. minSdk 26 이면 v2 만으로 충분하고 설치가 빠르다.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // 실사용 앱과 개발 빌드를 실기기에 동시 설치할 수 있게 한다.
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://dev.7bits.mooo.com/api/v1/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", "\"https://7bits.mooo.com/api/v1/\"")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "[MyFit] 릴리즈 서명 키를 찾지 못해 debug 키로 서명합니다. " +
                        "배포용 APK 가 아닙니다. keystore.properties 를 확인하세요.",
                )
                signingConfigs.getByName("debug")
            }
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Repository 인터페이스 ↔ 구현 바인딩은 :app 에서만 수행한다 (R-2)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)

    implementation(projects.feature.exercise)
    implementation(projects.feature.workout)
    implementation(projects.feature.routine)
    implementation(projects.feature.calendar)
    implementation(projects.feature.settings)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
