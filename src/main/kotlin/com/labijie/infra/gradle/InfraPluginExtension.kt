package com.labijie.infra.gradle

import InfraConstants.DEFAULT_KSP_API_VERSION
import InfraConstants.DEFAULT_MYBATIS_GENERATOR_CORE_VERSION
import InfraConstants.DEFAULT_MYBATIS_GENERATOR_PLUGIN_VERSION
import InfraConstants.DEFAULT_MYSQL_CONNECTOR_VERSION
import InfraConstants.GitPropertiesPluginId
import com.google.devtools.ksp.gradle.KspExtension
import com.gorylenko.GitPropertiesPluginExtension
import com.labijie.infra.gradle.BuildConfig.useDefault
import com.labijie.infra.gradle.BuildConfig.useGithubAccount
import com.labijie.infra.gradle.BuildConfig.useNexusPublishPlugin
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.applyPluginIfNot
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.internal.ProjectProperties
import com.thinkimi.gradle.MybatisGeneratorExtension
import configureTask
import getProjectFile
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import java.io.File
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
open class InfraPluginExtension @Inject constructor(
    private val project: Project,
    private val objectFactory: ObjectFactory
) {
    companion object {
        const val Name = "infra"
    }

    internal val skipTasks = mutableSetOf(
        "test",
        "kspKotlin",
        "kaptKotlin",
        "kspTestKotlin",
        "javadoc",
        "javadocJar",
        "compileTestJava",
        "compileTestKotlin",
        "processTestResources",
        "testClasses"
    )


    private fun Project.processResources(configure: ProcessResources.() -> Unit) {
        this.configureTask(name = "processResources", configuration = configure)
    }

    fun skipTaskForFastBuild(vararg tasks: String) {
        tasks.forEach {
            this.skipTasks.add(it)
        }
    }

    private fun isBom(): Boolean {
        return project.plugins.findPlugin("java-platform") != null
    }

    /**
     *  @param newMavenHost Users registered in Sonatype after 24 February 2021 need to set this value to true
     */
    private fun usePublishPlugin(newMavenHost: Boolean = true) {
        if (project.parent == null) {
            this.project.useNexusPublishPlugin(newMavenHost)
        } else {
            this.project.rootProject.useNexusPublishPlugin(newMavenHost)
        }
    }

    fun useKaptPlugin(vararg kaptDependencies: Any, kaptConfig: Action<KaptExtension>? = null) {
        project.applyPluginIfNot("org.jetbrains.kotlin.kapt")
        kaptDependencies.forEach { dp ->
            project.dependencies.add("kapt", dp)
        }
        if (kaptConfig != null) {
            project.configureFor(KaptExtension::class.java) {
                kaptConfig.execute(this)
            }
        }
    }


    fun useSpringConfigurationProcessor(springbootVersion: String) {
        useKaptPlugin("org.springframework.boot:spring-boot-configuration-processor:${springbootVersion}")
    }


    fun useKspPlugin(vararg kspDependencies: Any, kspConfig: (KspExtension.() -> Unit)? = null) {
        project.applyPluginIfNot("com.google.devtools.ksp")
        kspDependencies.forEach { dp ->
            project.dependencies.add("ksp", dp)
        }
        kspConfig?.let {
            project.configureFor(KspExtension::class.java, kspConfig)
        }
    }


    fun useKspApi(version: String = DEFAULT_KSP_API_VERSION, configurationName: String = "implementation") {
        project.dependencies.apply {
            project.dependencies.add(configurationName, "com.google.devtools.ksp:symbol-processing-api:${version}")
        }
    }

    fun useInfraOrmGenerator(
        generatorVersion: String = "2.0.+",
        pojoProjectDir: String? = null,
        pojoPackageName: String? = null,
        generateAot: Boolean = true,
    ) {
        useKspPlugin("com.labijie.orm:exposed-generator:${generatorVersion}")
        project.afterEvaluate {
            project.configureFor(KspExtension::class.java) {
                if (!pojoProjectDir.isNullOrBlank()) {
                    var dir: String = pojoProjectDir
                    if (!File(dir).isAbsolute) {
                        dir = Path(project.projectDir.absolutePath, pojoProjectDir).absolutePathString()
                    }
                    this.arg("orm.pojo_project_dir", dir)
                }
                pojoPackageName?.let {
                    this.arg("orm.pojo_package", pojoPackageName)
                }
                this.arg("orm.springboot_aot", generateAot.toString())
                this.arg("orm.table_artifact_id", project.name)
            }
        }

    }

    fun forceVersion(version: String, groupPrefix: String, vararg packageNamePrefix: String) {
        project.configurations.all { conf ->
            conf.resolutionStrategy.eachDependency { details ->
                if ((details.requested.group == groupPrefix) &&
                    (packageNamePrefix.isEmpty() || packageNamePrefix.any { details.requested.name.startsWith(it) })
                ) {
                    details.useVersion(version)
                }
            }
        }
    }

    fun gitProperties(action: Action<GitPropertiesPluginExtension>) {
        this.project.configureFor(GitPropertiesPluginExtension::class.java) {
            action.execute(this)
        }
    }

    fun useDefault(action: Action<in ProjectProperties>) {
        val self = this
        val properties = ProjectProperties()
        action.execute(properties)
        if (properties.gitPropertiesPluginEnabled) {
            project.applyPluginIfNot(GitPropertiesPluginId)
            project.configureFor(GitPropertiesPluginExtension::class.java) {
                this.customProperties.putIfAbsent("project.version", project.version)
                this.customProperties.putIfAbsent("project.group", project.group)
                this.customProperties.putIfAbsent("project.name", project.name)
                this.gitPropertiesName = "git-info/git.properties"
                this.failOnNoGitDirectory = false
                this.dotGitDirectory.set(project.rootProject.layout.projectDirectory.dir(".git"))
            }
        }


        this.project.useDefault(
            self.isBom(),
            properties
        )

        usePublishPlugin(!properties.mavenPublishingOldHost)
        forceVersion(InfraConstants.DEFAULT_KOTLIN_VERSION, "org.jetbrains.kotlin", "kotlin-stdlib", "kotlin-reflect", "kotlin-bom")
    }

    /**
     * @param targetClassPath target bundle path (package path), example: com/labijie/application
     * @param resourceName resource file name, default is "messages"
     */
    fun processSpringMessageSource(
        targetClassPath: String,
        resourceName: String = "messages",
        showLog: Boolean = false
    ) {
        project.processResources {
            from("src/main/resources") { cp ->
                cp.include("${resourceName}*.properties")
                cp.into(targetClassPath)
            }
            filesMatching("${resourceName}*.properties") { cp ->
                val include = cp.path.startsWith(targetClassPath)
                if (showLog) {
                    println("find message resource: ${cp.path} (${if (include) "included" else "excluded"}")
                }
                if (!include) {
                    cp.exclude()
                }
            }
        }
    }

    fun useGithubAccount(user: String, key: String) {
        this.project.useGithubAccount(user, key)
    }

    fun publishing(action: Action<in PublishingBuilder>) {
        val builder = PublishingBuilder(this.project)
        action.execute(builder)
    }

    /**
     * 使用 mybatis 代码生成器
     *
     * 插件： com.thinkimi.gradle.MybatisGenerator
     *
     * 可以配合 itfsw 插件
     *
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
    @Deprecated("Use infra-orm instead Mybatis: https://github.com/hongque-pro/infra-orm")
    fun useMybatis(
        configFile: String,
        propertiesFile: String,
        isMysqlDataSource: Boolean = true,
        enableItfswPlug: Boolean = false,
        generatorCoreVersion: String = DEFAULT_MYBATIS_GENERATOR_CORE_VERSION,
        itfswPluginVersion: String = DEFAULT_MYBATIS_GENERATOR_PLUGIN_VERSION,
        mysqlConnectorVersion: String = DEFAULT_MYSQL_CONNECTOR_VERSION,
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
                add(config, "com.mysql:mysql-connector-j:${mysqlConnectorVersion}")
            }
        }
        project.configureFor(MybatisGeneratorExtension::class.java) {
            this.configFile = project.getProjectFile(configFile)
            this.overwrite = true
            this.verbose = true
            this.mybatisProperties = objectFactory.mapProperty(String::class.java, String::class.java)

            if (propertiesFileConfigKey.isNotBlank()) {
                this.mybatisProperties.set(
                    mapOf(
                        propertiesFileConfigKey to propertiesFile
                    )
                )
            }
        }
    }

}