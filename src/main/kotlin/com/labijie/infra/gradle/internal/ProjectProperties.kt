package com.labijie.infra.gradle.internal

import com.labijie.infra.gradle.DEFAULT_INFRA_BOM_VERSION
import com.labijie.infra.gradle.DEFAULT_JDK_VERSION
import com.labijie.infra.gradle.DEFAULT_KOTLIN_VERSION

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
class ProjectProperties {
    var infraBomVersion: String = DEFAULT_INFRA_BOM_VERSION
    var kotlinVersion: String = DEFAULT_KOTLIN_VERSION
    var jdkVersion: String = DEFAULT_JDK_VERSION
    var includeSource: Boolean = false
    var includeDocument: Boolean = false
    var useMavenProxy: Boolean = false
    var gitPropertiesPluginEnabled = true
    var mavenPublishingOldHost = false

    /**
     * GihHub repo
     *
     * key: owner
     *
     * value: repostory name
     */
    internal val githubRepositories = mutableMapOf<String, MutableSet<String>>()

    fun addGitHubRepository(owner: String, vararg repositories: String) {
        if (repositories.isNotEmpty() && owner.isNotBlank()) {
            val list = this.githubRepositories.getOrPut(owner) { mutableSetOf() }
            repositories.forEach {
                list.add(it)
            }
        }
    }
}