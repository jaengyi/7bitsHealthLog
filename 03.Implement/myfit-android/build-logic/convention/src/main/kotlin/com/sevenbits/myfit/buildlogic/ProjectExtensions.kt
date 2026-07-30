package com.sevenbits.myfit.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** 버전 카탈로그 접근자 — 컨벤션 플러그인은 버전을 하드코딩하지 않는다. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

internal fun VersionCatalog.intVersion(alias: String): Int = version(alias).toInt()

/**
 * Android 모듈 공통 설정.
 * SDK 레벨·Java 타깃·desugaring 을 전 모듈에 동일하게 적용한다.
 * (02.Design/02_개발환경및빌드정의서.md §2.1)
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    // AGP 9 의 CommonExtension 은 getter 만 노출하고 `defaultConfig { }` 같은
    // 람다 설정 메서드를 제공하지 않는다. 프로퍼티로 직접 설정한다.
    commonExtension.compileSdk = libs.intVersion("compileSdk")
    commonExtension.defaultConfig.minSdk = libs.intVersion("minSdk")

    commonExtension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // minSdk 26 에서 java.time 사용 (설계서 §2.1)
        isCoreLibraryDesugaringEnabled = true
    }

    configureKotlin()

    dependencies.add(
        "coreLibraryDesugaring",
        libs.findLibrary("desugar-jdk-libs").get(),
    )
}

/** Kotlin 컴파일 공통 옵션 (Android / JVM 공용) */
internal fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // 경고를 방치하지 않되, 초기 골격 단계에서는 빌드를 막지 않는다.
            allWarningsAsErrors.set(false)
        }
    }
}
