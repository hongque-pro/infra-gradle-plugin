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
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject
import kotlin.io.path.Path

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
open class InfraPluginExtension @Inject constructor(private val project: Project, private val objectFactory: ObjectFactory) {
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
     * 使用 mybatis 代码生成器， 可以配合 itfsw 插件
     * @see <a href="https://github.com/itfsw/mybatis-generator-plugin">https://github.com/itfsw/mybatis-generator-plugin</a>
     *
     * @param configFile XML 配置文件路径，相对路径表示相对项目根目录的路径，也可以设置据对路径
     * @param propertiesFile XML 配置文件中 properties 节使用的文件，相对路径表示相对项目根目录的路径，也可以设置据对路径。
     * 通常格式为:  &lt;properties url="file:///${propertiesFile}"></properties>
     *
     * @param isMysqlDataSource 是否使用 Mysql 数据源生成代码
     * @param enableItfswPlug 是否使用 itfsw 增强插件
     * @param generatorCoreVersion mybatis 官方 generator-core 包版本
     * @param itfswPluginVersion itfsw 插件版本
     * @param mysqlConnectorVersion mysql-connector-java 包版本
     * @param propertiesFileConfigKey XML 中 properties 文件使用的 key
     *
     */
    fun useMybatis(
        configFile: String,
        propertiesFile: String,
        isMysqlDataSource: Boolean = true,
        enableItfswPlug: Boolean = false,
        generatorCoreVersion: String = "1.4.2",
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
            this.mybatisProperties = objectFactory.mapProperty(String::class.java, String::class.java)

            if (propertiesFileConfigKey.isNotBlank()) {
                this.mybatisProperties.set(mapOf(
                    propertiesFileConfigKey to propertiesFile
                ))
            }
        }
    }

}