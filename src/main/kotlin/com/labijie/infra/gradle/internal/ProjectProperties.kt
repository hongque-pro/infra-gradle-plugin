package com.labijie.infra.gradle.internal

import com.labijie.infra.gradle.DEFAULT_INFRA_BOM_VERSION
import com.labijie.infra.gradle.DEFAULT_JDK_VERSION
import com.labijie.infra.gradle.DEFAULT_KOTLIN_VERSION
import com.labijie.infra.gradle.Utils

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
class ProjectProperties {
    var infraBomVersion: String = DEFAULT_INFRA_BOM_VERSION
    var kotlinVersion: String = DEFAULT_KOTLIN_VERSION
    var jvmVersion: String = DEFAULT_JDK_VERSION
    var includeSource: Boolean = false
    var includeDocument: Boolean = false
    var useMavenProxy: Boolean = !Utils.isGithubAction() && !Utils.isDisableMavenPRoxy()
    /**
     * GihHub repo
     *
     * key: owner
     *
     * value: repostory name
     */
    internal val githubRepositories = mutableMapOf<String, MutableSet<String>>()

    fun addGitHubPackages(owner: String, vararg repositories: String) {
        if (repositories.isNotEmpty() && owner.isNotBlank()) {
            val list = this.githubRepositories.getOrPut(owner) { mutableSetOf() }
            repositories.forEach {
                list.add(it)
            }
        }
    }

    fun addHongQueGitHubPackages() {
        addGitHubPackages("endink", "caching-kotlin")
        addGitHubPackages(
            "hongque-pro",
            "infra-bom",
            "infra-orm",
            "infra-commons",
            "infra-spring-cloud-stream",
            "infra-telemetry",
            "infra-mqts",
            "infra-oauth2"
        )
    }
}