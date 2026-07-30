import com.sevenbits.myfit.buildlogic.configureKotlin
import com.sevenbits.myfit.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * 순수 Kotlin(JVM) 라이브러리.
 *
 * `:core:domain` 에 이 플러그인만 적용함으로써 **Android 의존을 컴파일 단계에서 차단**한다.
 * (설계 원칙 P3 / 의존 규칙 R-1)
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        configureKotlin()

        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            // @Inject 애너테이션. Hilt 가 있는 모듈에서 이 클래스를 주입 대상으로 인식하려면
            // 컴파일 클래스패스에 노출되어야 하므로 api 로 둔다.
            add("api", libs.findLibrary("javax-inject").get())
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("truth").get())
            add("testImplementation", libs.findLibrary("mockk").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
