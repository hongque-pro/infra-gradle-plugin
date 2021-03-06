import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.labijie.infra.gradle.InfraExtension
import com.labijie.infra.gradle.Utils
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.configureFor
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.*

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
fun Project.infra(isBom: Boolean = false, action: Action<in InfraExtension>) {
    if (!Utils.initedProjects.contains(this)) {

        val props =  Utils.initedProjects.getOrPut(this) { Properties() }

        val file = project.rootProject.file("local.properties")
        if (file.isFile && file.exists()) {
            project.rootProject.file("local.properties").inputStream().use { it ->
                props.load(it)
            }
        }

        this.apply(plugin = "com.labijie.infra")
        this.apply(plugin = "com.github.ben-manes.versions")
        if (isBom) {
            this.apply(plugin = "java-platform")
            this.configureFor(JavaPlatformExtension::class.java) {
                this.allowDependencies()
            }
        } else {
            this.apply(plugin = "kotlin")
            this.apply(plugin = "kotlin-spring")
            this.apply(plugin = "java-library")
        }


        this.tasks.withType(DependencyUpdatesTask::class.java) { dependencyUpdatesTask ->
            dependencyUpdatesTask.checkConstraints = true
            dependencyUpdatesTask.resolutionStrategy { strategyWithCurrent ->
                strategyWithCurrent.componentSelection { current ->
                    current.all { c ->
                        val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea")
                            .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-+]*") }
                            .any { it.matches(c.candidate.version) }
                        if (rejected) {
                            c.reject("Release candidate")
                        }
                    }
                }
            }
        }
    }
    val ext = this.extensions.findByName(InfraExtension.Name)
    if (ext != null && ext is InfraExtension) {
        this.extensions.configure(InfraExtension::class.java, action)
    }
}

fun Project.findPropertyAndLocal(propertyName: String): String? {
    return Utils.initedProjects.getOrDefault(project, null)?.get(propertyName)?.toString() ?: project.findProperty(propertyName)?.toString()
}

fun Project.getPropertyOrCmdArgs(propertyAndEnvVarName: String, cmdArgName: String? = null): String? {
    val project = this
    return (System.getProperty(cmdArgName ?: propertyAndEnvVarName) ?: System.getenv(propertyAndEnvVarName)?.ifEmpty { null }) ?: project.findPropertyAndLocal(propertyAndEnvVarName)
}

inline fun <reified C : Task> Project.configureTask(name: String, configuration: C.() -> Unit) {
    (this.tasks.getByName(name) as C).configuration()
}

fun Project.processResources(configure: ProcessResources.() -> Unit) {
    this.configureTask(name = "processResources", configuration = configure)
}