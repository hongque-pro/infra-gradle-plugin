package com.labijie.infra.gradle

import com.labijie.infra.gradle.BuildConfig.configurePublishing
import com.labijie.infra.gradle.BuildConfig.useGitHubPackagesPub
import com.labijie.infra.gradle.BuildConfig.useNexusPub
import com.labijie.infra.gradle.BuildConfig.usePublishRepository
import com.labijie.infra.gradle.internal.PomInfo
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException

/**
 *
 * @Author: Anders Xiao
 * @Date: 2023/11/21
 * @Description:
 */
class PublishingBuilder internal constructor(private val project: Project) {
    fun pom(action: Action<in PomInfo>) {
        val pom = PomInfo()
        action.execute(pom)

        if (pom.description.isBlank()) throw MissingValueException("${pom::description.name} is missing, set in labijie publish block")
        if (pom.projectUrl.isBlank()) throw MissingValueException("${pom::projectUrl.name} is missing, set in labijie publish block")
        if (pom.gitUrl.isBlank()) throw MissingValueException("${pom::gitUrl.name} is missing, set in labijie publish block")
        if (pom.githubScmUrl.isBlank()) throw MissingValueException("${pom::githubScmUrl.name} is missing, set in labijie publish block")

        this.project.configurePublishing(pom, pom.idGeneration)
    }

    fun toGithubPackages(owner: String,
                         repository: String) {
        project.useGitHubPackagesPub(owner, repository)
    }

    fun toNexus(url: String,
             user: String? = null,
             password: String? = null) {
        project.useNexusPub(url, user, password)
    }

    fun toCustomRepository(
        repositoryName: String,
        url: String,
        user: String? = null,
        password: String? = null,
        allowInsecureProtocol: Boolean = true
    ) {
        project.usePublishRepository(repositoryName, { url }, { user }, { password }, { allowInsecureProtocol })
    }
}