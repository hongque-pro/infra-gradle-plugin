package com.labijie.infra.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.labijie.infra.gradle.BuildConfig.useDefault
import com.labijie.infra.gradle.BuildConfig.useNexusPublishPlugin
import com.labijie.infra.gradle.BuildConfig.usePublishing
import com.labijie.infra.gradle.BuildConfig.usePublishingRepository
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.Utils.getProjectFile
import com.labijie.infra.gradle.internal.PomInfo
import com.labijie.infra.gradle.internal.ProjectProperties
import com.thinkimi.gradle.MybatisGeneratorExtension
import getPropertyOrCmdArgs
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


    private fun isBom(): Boolean {
        return project.plugins.findPlugin("java-platform") != null
    }

    fun useNexusPublish(newMavenHost: Boolean = true) {
        if (project.parent == null) {
            this.project.useNexusPublishPlugin(newMavenHost)
        } else {
            project.logger.debug("${project.name} is not a root project, useNexusPublish skipped.")
        }
    }

    fun Project.useNexus(
        url: String? = null,
        username: String? = null,
        password: String? = null,
    ) {
        val p = this
        this.usePublishingRepository("Nexus",
            {
                p.getPropertyOrCmdArgs("PUB_URL", "s") ?: url
            },
            {
                p.getPropertyOrCmdArgs("PUB_USER", "u") ?: username
            },
            {
                p.getPropertyOrCmdArgs("PUB_PWD", "p") ?: password
            })
    }

    fun Project.useGitHubPackages(owner: String, repository: String) {
        val repoName = "GitHubPackages"
        val url = "https://maven.pkg.github.com/${owner}/${repository}"

        this.usePublishingRepository(
            repoName,
            { url },
            { System.getenv("GITHUB_ACTOR") },
            { System.getenv("GITHUB_TOKEN") })
    }


    fun useInfraOrmGenerator(version: String = "1.0.0", outputDir: String? = null, packageName: String? = null) {
        if (!project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
            project.apply(plugin = "com.google.devtools.ksp")
        }
        project.dependencies.apply {
            this.add("ksp", "com.labijie.orm:exposed-generator:${version}")
        }
        if (!outputDir.isNullOrBlank() || !packageName.isNullOrBlank()) {
            project.configureFor(KspExtension::class.java) {
                if (!outputDir.isNullOrBlank()) {
                    var dir: String = outputDir
                    if (!File(dir).isAbsolute) {
                        dir = Path(project.projectDir.absolutePath, outputDir).toString()
                    }
                    this.arg("exg_out_dir", dir)
                }
                if (!packageName.isNullOrBlank()) {
                    this.arg("exg_package", packageName)
                }
            }
        }
    }

    fun useDefault(action: Action<in ProjectProperties>) {
        val self = this
        val properties = ProjectProperties()
        action.execute(properties)
        this.project.useDefault(
            self.isBom(),
            properties
        )
    }

    fun usePublish(action: Action<in PomInfo>) {
        val pom = PomInfo()
        action.execute(pom)

        if (pom.description.isBlank()) throw MissingValueException("${pom::description.name} is missing, set in labijie publish block")
        if (pom.projectUrl.isBlank()) throw MissingValueException("${pom::projectUrl.name} is missing, set in labijie publish block")
        if (pom.gitUrl.isBlank()) throw MissingValueException("${pom::gitUrl.name} is missing, set in labijie publish block")
        if (pom.githubScmUrl.isBlank()) throw MissingValueException("${pom::githubScmUrl.name} is missing, set in labijie publish block")

        this.project.usePublishing(pom, pom.idGeneration)
    }

    /**
     * ?????? mybatis ?????????????????? ???????????? itfsw ??????
     * @see <a href="https://github.com/itfsw/mybatis-generator-plugin">https://github.com/itfsw/mybatis-generator-plugin</a>
     *
     * @param configFile XML ???????????????????????????????????????????????????????????????????????????????????????????????????
     * @param propertiesFile XML ??????????????? properties ??????????????????????????????????????????????????????????????????????????????????????????????????????
     * ???????????????:  &lt;properties url="file:///${propertiesFile}"></properties>
     *
     * @param isMysqlDataSource ???????????? Mysql ?????????????????????
     * @param enableItfswPlug ???????????? itfsw ????????????
     * @param generatorCoreVersion mybatis ?????? generator-core ?????????
     * @param itfswPluginVersion itfsw ????????????
     * @param mysqlConnectorVersion mysql-connector-java ?????????
     * @param propertiesFileConfigKey XML ??? properties ??????????????? key
     *
     */
    fun useMybatis(
        configFile: String,
        propertiesFile: String,
        isMysqlDataSource: Boolean = true,
        enableItfswPlug: Boolean = false,
        generatorCoreVersion: String = "1.4.0",
        itfswPluginVersion: String = "1.3.10",
        mysqlConnectorVersion: String = "8.0.27",
        propertiesFileConfigKey: String = "propertiesFile"
    ) {
        val config = "mybatisGenerator"
        project.apply(plugin = "com.thinkimi.gradle.MybatisGenerator")
        project.dependencies.apply {
            add(config, "org.mybatis.generator:mybatis-generator-core:${generatorCoreVersion}")
            if (enableItfswPlug) {
                add(config, "com.itfsw:mybatis-generator-plugin:${itfswPluginVersion}")
            }
            if (isMysqlDataSource) {
                add(config, "mysql:mysql-connector-java:${mysqlConnectorVersion}")
            }
        }
        project.configureFor(MybatisGeneratorExtension::class.java) {
            this.configFile = project.getProjectFile(configFile)
            this.overwrite = true
            this.verbose = true
            if (propertiesFileConfigKey.isNotBlank()) {
                this.mybatisProperties = mapOf(
                    propertiesFileConfigKey to propertiesFile
                )
            }
        }
    }

}