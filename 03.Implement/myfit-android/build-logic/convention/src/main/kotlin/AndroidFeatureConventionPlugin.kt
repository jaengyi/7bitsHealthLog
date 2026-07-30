import com.sevenbits.myfit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * feature 모듈 공통 설정.
 *
 * 의존 규칙 R-2 (02.Design/03_애플리케이션모듈설계서.md §1.3):
 * feature 는 :core:data / :core:database / :core:network 를 참조하지 않는다.
 * Repository 는 :core:domain 의 인터페이스로만 접근한다.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("myfit.android.library.compose")
        pluginManager.apply("myfit.android.hilt")

        dependencies {
            api(project(":core:domain"))
            api(project(":core:common"))
            api(project(":core:designsystem"))
            api(project(":core:ui"))

            implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            implementation(libs.findLibrary("androidx-navigation-compose").get())
            implementation(libs.findLibrary("hilt-navigation-compose").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
        }

        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    }

    private fun DependencyHandler.api(dep: Any) = add("api", dep)
    private fun DependencyHandler.implementation(dep: Any) = add("implementation", dep)
}
