package com.labijie.infra.gradle

import com.labijie.infra.gradle.BuildConfig.useDefault
import com.labijie.infra.gradle.BuildConfig.useNexusPublishPlugin
import com.labijie.infra.gradle.BuildConfig.usePublishing
import com.labijie.infra.gradle.internal.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
open class InfraExtension(private val project: Project) {
    companion object {
        const val Name = "infra"
    }

    fun useNexusPublish() {
        if(project.parent == null) {
            this.project.useNexusPublishPlugin()
        }else{
            project.logger.warn("${project.name} is not a root project, useNexusPublish skipped.")
        }
    }

    fun useDefault(action: Action<in ProjectProperties>) {
        val properties = ProjectProperties()
        action.execute(properties)
        this.project.useDefault(
            properties.kotlinVersion,
            properties.jvmVersion,
            properties.includeSource,
            properties.useMavenProxy,
            properties.infraBomVersion
        )
    }

    fun usePublish(action: Action<in PomInfo>) {
        val pom = PomInfo()
        action.execute(pom)

        if (pom.description.isBlank()) throw MissingValueException("${pom::description.name} is missing, set in labijie publish block")
        if (pom.projectUrl.isBlank()) throw MissingValueException("${pom::projectUrl.name} is missing, set in labijie publish block")
        if (pom.gitUrl.isBlank()) throw MissingValueException("${pom::gitUrl.name} is missing, set in labijie publish block")
        if (pom.githubScmUrl.isBlank()) throw MissingValueException("${pom::githubScmUrl.name} is missing, set in labijie publish block")

        this.project.usePublishing(pom)
    }

}