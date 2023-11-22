import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin


internal fun Project.registerPubTask(repositoryName: String) {
    this.tasks.register("publishPluginTo${repositoryName}") {
        this.group = "plugin publishing"
        dependsOn(
            "publishInfraPluginPluginMarkerMavenPublicationTo${repositoryName}",
            "publishPluginMavenPublicationTo${repositoryName}"
        )
    }
}

fun Project.registerPubTasks() {
    extensions.findByType(PublishingExtension::class.java)?.repositories?.forEach {
        registerPubTask("${it.name}Repository")
    }
    registerPubTask("MavenLocal")
}