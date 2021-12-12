import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.labijie.infra.gradle.InfraExtension
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.configureFor
import org.gradle.api.Action
import org.gradle.api.Project

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