import com.android.build.api.dsl.LibraryExtension
import com.sevenbits.myfit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose 를 사용하는 Android 라이브러리 모듈.
 * `myfit.android.library` 위에 얹어 사용한다.
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("myfit.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            buildFeatures.compose = true
        }

        val bom = libs.findLibrary("compose-bom").get()
        dependencies {
            add("implementation", platform(bom))
            add("implementation", libs.findLibrary("compose-ui").get())
            add("implementation", libs.findLibrary("compose-ui-graphics").get())
            add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
            add("implementation", libs.findLibrary("compose-material3").get())
            add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())

            add("androidTestImplementation", platform(bom))
            add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
            add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
        }
    }
}
