import com.labijie.infra.gradle.InfraExtension
import com.labijie.infra.gradle.Utils.apply
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
    val ext = this.extensions.findByName(InfraExtension.Name)
    if(ext != null && ext is InfraExtension) {
        this.extensions.configure(InfraExtension::class.java, action)
    }
}