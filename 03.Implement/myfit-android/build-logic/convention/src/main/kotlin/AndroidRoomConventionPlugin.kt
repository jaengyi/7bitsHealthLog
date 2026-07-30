import com.google.devtools.ksp.gradle.KspExtension
import com.sevenbits.myfit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room 사용 모듈 설정.
 *
 * `room.schemaLocation` 을 반드시 지정한다. 스키마 JSON 은 VCS 에 커밋하며
 * 마이그레이션 테스트의 입력이 된다. (02.Design/04_데이터베이스설계서.md §5)
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "${projectDir}/schemas")
            arg("room.generateKotlin", "true")
        }

        dependencies {
            add("implementation", libs.findLibrary("androidx-room-runtime").get())
            add("implementation", libs.findLibrary("androidx-room-ktx").get())
            add("ksp", libs.findLibrary("androidx-room-compiler").get())
            add("testImplementation", libs.findLibrary("androidx-room-testing").get())
        }
    }
}
