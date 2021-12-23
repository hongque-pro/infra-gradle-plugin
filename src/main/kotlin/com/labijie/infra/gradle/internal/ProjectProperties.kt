package com.labijie.infra.gradle.internal

import jdk.nashorn.internal.runtime.ListAdapter

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
class ProjectProperties {
    var infraBomVersion: String = "2.6.5"
    var kotlinVersion: String = "1.6.0"
    var jvmVersion: String = "1.8"
    var includeSource: Boolean = false
    var useMavenProxy: Boolean = true

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