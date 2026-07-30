pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "myfit-android"

// Gradle 8.x 에서 projects.* 타입 세이프 접근자를 쓰기 위해 필요
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


include(":app")

// core — 계층 모듈
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:designsystem")
include(":core:ui")

// feature — Phase 1 대상
include(":feature:exercise")
include(":feature:workout")
include(":feature:routine")
include(":feature:calendar")
include(":feature:settings")

// Phase 2/3 착수 시 추가
// include(":feature:report")
// include(":feature:body")
// include(":feature:diet")
// include(":feature:tool")
// include(":wear")
// include(":benchmark")
