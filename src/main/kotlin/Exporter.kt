import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent
import com.labijie.infra.gradle.CheckUpdatePluginId
import com.labijie.infra.gradle.DEFAULT_KOTLIN_VERSION
import com.labijie.infra.gradle.InfraPluginExtension
import com.labijie.infra.gradle.Utils
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.configureFor
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlatformExtension
import java.io.File
import java.util.*

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
fun Project.infra(isBom: Boolean = false, action: Action<in InfraPluginExtension>) {
    if (!Utils.initedProjects.contains(this)) {

        val props = Utils.initedProjects.getOrPut(this) { Properties() }

        val file = project.rootProject.file("local.properties")
        if (file.isFile && file.exists()) {
            project.rootProject.file("local.properties").inputStream().use { it ->
                props.load(it)
            }
        }

        this.apply(plugin = "com.labijie.infra")
        if (isBom) {
            this.apply(plugin = "java-platform")
            this.configureFor(JavaPlatformExtension::class.java) {
                this.allowDependencies()
            }
        } else {
            this.apply(plugin = "org.jetbrains.kotlin.jvm")
            this.apply(plugin = "org.jetbrains.kotlin.plugin.spring")
            this.apply(plugin = "java-library")
        }
        this.apply(plugin = CheckUpdatePluginId)


        this.tasks.withType(DependencyUpdatesTask::class.java) { dependencyUpdatesTask ->
            dependencyUpdatesTask.checkConstraints = true
            dependencyUpdatesTask.resolutionStrategy { strategyWithCurrent ->

                strategyWithCurrent.componentSelection { current ->
                    current.all { c: ComponentSelectionWithCurrent ->
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
    val ext = this.extensions.findByName(InfraPluginExtension.Name)
    if (ext != null && ext is InfraPluginExtension) {
        this.extensions.configure(InfraPluginExtension::class.java, action)
    }
}

fun Project.getProjectFile(file: String): String {
    val f = File(file)
    return if (f.isAbsolute) {
        f.absolutePath
    } else {
        File(this.projectDir, file).absolutePath
    }
}

fun Project.findPropertyAndLocal(propertyName: String): String? {
    return Utils.initedProjects.getOrDefault(project, null)?.get(propertyName)?.toString() ?: project.findProperty(
        propertyName
    )?.toString()
}

fun Project.getPropertyOrCmdArgs(envVarName: String, cmdArgName: String? = null): String? {
    val project = this
    return (System.getProperty(cmdArgName ?: envVarName) ?: System.getenv(envVarName)?.ifEmpty { null })
        ?: project.findPropertyAndLocal(cmdArgName ?: envVarName)
}

inline fun <reified C : Task> Project.configureTask(name: String, configuration: C.() -> Unit) {
    (this.tasks.getByName(name) as C).configuration()
}


fun Project.forceDependencyGroupVersion(group: String, version: String) {
    if (group.isNotBlank() && version.isNotBlank()) {
        configurations.all {
            it.resolutionStrategy.eachDependency { details ->
                val requested = details.requested
                if (requested.group == group) {
                    details.useVersion(DEFAULT_KOTLIN_VERSION)
                }
            }
        }
    } else {
        project.logger.warn("forceDependencyGroupVersion require group and version parameter !")
    }
}

fun Project.forceDependencyVersion(group: String, name: String, version: String) {
    if (group.isNotBlank() && version.isNotBlank()) {
        configurations.all {
            it.resolutionStrategy.eachDependency { details ->
                val requested = details.requested
                if (requested.group == group && (name.isBlank() || requested.name == name)) {
                    details.useVersion(version)
                }
            }
        }
    } else {
        project.logger.warn("forceDependencyVersion require group, name, version parameters !")
    }
}

fun Project.forceDependencyVersion(group: String, version: String) {
    forceDependencyVersion(group, "", version)
}

fun isGithubAction(): Boolean {
    return !System.getenv("GITHUB_JOB").isNullOrBlank()
}

fun isJenkinsPipeline(): Boolean {
    return !System.getenv("BUILD_NUMBER").isNullOrBlank() && !System.getenv("BUILD_ID").isNullOrBlank()
}

fun isTeamCityPipeline(): Boolean {
    return !System.getenv("TEAMCITY_VERSION").isNullOrBlank()
}

/**
 * Check whether project is building in ci pipeline.
 *
 * Currently, we check below ci software:
 *
 * GitHub, Jenkins, TemCity
 *
 *  @see isTeamCityPipeline
 *  @see isJenkinsPipeline
 *  @see isGithubAction
 */
fun isInCIPipeline(): Boolean {
    return isGithubAction() || !System.getenv("CI").isNullOrBlank() || isJenkinsPipeline() || isTeamCityPipeline()
}

fun isDisableMavenProxy(): Boolean {
    return !System.getenv("NO_MAVEN_PROXY").isNullOrBlank()
}