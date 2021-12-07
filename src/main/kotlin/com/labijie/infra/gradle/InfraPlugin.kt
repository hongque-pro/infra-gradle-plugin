package com.labijie.infra.gradle

import com.labijie.infra.gradle.BuildConfig.useDefault
import com.labijie.infra.gradle.Utils.apply
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
class InfraPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        target.apply(plugin = "kotlin")
        target.apply(plugin = "kotlin-spring")
        target.apply(plugin = "java-library")
        target.extensions.create(InfraExtension.Name, InfraExtension::class.java, target)
    }
}