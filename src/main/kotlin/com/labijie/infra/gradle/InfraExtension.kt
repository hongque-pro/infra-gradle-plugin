package com.labijie.infra.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.labijie.infra.gradle.BuildConfig.useDefault
import com.labijie.infra.gradle.BuildConfig.useNexusPublishPlugin
import com.labijie.infra.gradle.BuildConfig.usePublishing
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.internal.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException
import java.io.File
import kotlin.io.path.Path

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
        if (project.parent == null) {
            this.project.useNexusPublishPlugin()
        } else {
            project.logger.debug("${project.name} is not a root project, useNexusPublish skipped.")
        }
    }

    fun useInfraOrmGenerator(version: String = "1.0.0", outputDir: String? = null, packageName:String? = null) {
        if (!project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
            project.apply(plugin = "com.google.devtools.ksp")
        }
        project.dependencies.apply {
            this.add("ksp", "com.labijie.orm:exposed-generator:${version}")
        }
        if(!outputDir.isNullOrBlank() || !packageName.isNullOrBlank()){
            project.configureFor(KspExtension::class.java){
                if(!outputDir.isNullOrBlank()){
                    var dir: String = outputDir
                    if(!File(dir).isAbsolute){
                        dir = Path(project.projectDir.absolutePath, outputDir).toString()
                    }
                    this.arg("exg_out_dir", dir)
                }
                if(!packageName.isNullOrBlank()){
                    this.arg("exg_package", packageName)
                }
            }
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