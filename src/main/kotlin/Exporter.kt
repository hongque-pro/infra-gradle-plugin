import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.labijie.infra.gradle.InfraExtension
import com.labijie.infra.gradle.Utils.apply
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.jvm.tasks.ProcessResources

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
fun Project.infra(action: Action<in InfraExtension>){
    this.apply(plugin="com.labijie.infra")
    this.apply(plugin="com.github.ben-manes.versions")

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

    val ext = this.extensions.findByName(InfraExtension.Name)
    if(ext != null && ext is InfraExtension) {
        this.extensions.configure(InfraExtension::class.java, action)
    }
}

fun Project.getPropertyOrCmdArgs(propertyAndEnvVarName: String, cmdArgName: String): String? {
    val project = this
    val propertyValue = project.findProperty(propertyAndEnvVarName)?.toString()
    return (System.getProperty(cmdArgName) ?: propertyValue) ?: System.getenv(propertyAndEnvVarName)?.ifEmpty { null }
}

inline fun <reified C : Task> Project.configureTask(name: String, configuration: C.() -> Unit) {
    (this.tasks.getByName(name) as C).configuration()
}

fun Project.processResources(configure: ProcessResources.() -> Unit) {
    this.configureTask(name ="processResources", configuration = configure)
}