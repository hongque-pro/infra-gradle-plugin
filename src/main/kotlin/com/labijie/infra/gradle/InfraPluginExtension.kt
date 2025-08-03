package com.labijie.infra.gradle

import InfraDefaultVersions
import InfraPlugins.GitPropertiesPluginId
import com.google.devtools.ksp.gradle.KSP_VERSION
import com.google.devtools.ksp.gradle.KspExtension
import com.gorylenko.GitPropertiesPluginExtension
import com.labijie.infra.gradle.BuildConfig.useDefault
import com.labijie.infra.gradle.BuildConfig.useGithubAccount
import com.labijie.infra.gradle.BuildConfig.useNexusPublishPlugin
import com.labijie.infra.gradle.Utils.applyPluginIfNot
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.internal.ProjectProperties
import configureTask
import forceDependencyVersion
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
    public val project: Project,
    public val objectFactory: ObjectFactory
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
        "testClasses",
        "kaptGenerateStubsKotlin",
        "kaptGenerateStubsTestKotlin"
    )

    internal val libraryTasks = mutableSetOf(
        "kaptKotlin"
    )

    public val infraProperties: ProjectProperties = ProjectProperties()

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

    private fun usePublishPlugin() {
        if (project.parent == null) {
            this.project.useNexusPublishPlugin()
        }
    }

    fun useKotlinSerializationPlugin() {
        return project.applyPluginIfNot("org.jetbrains.kotlin.plugin.serialization")
    }

    /**
     * @param kaptDependencies dependency artifact.
     * example: "com.example:plugin:0.0.1"
     */
    fun useKaptPlugin(vararg kaptDependencies: String, kaptConfig: Action<KaptExtension>? = null) {
        project.applyPluginIfNot("org.jetbrains.kotlin.kapt")
        kaptDependencies.forEach { dp ->
            project.addDependencyIfAbsent("kapt", dp)
        }
        if (kaptConfig != null) {
            project.configureFor(KaptExtension::class.java) {
                kaptConfig.execute(this)
            }
        }
    }

    fun Project.addDependencyIfAbsent(configurationName: String, depNotation: String) {
        val kaptConfig = configurations.getByName(configurationName)

        val (group, name) = depNotation.split(":").let {
            it.getOrNull(0) to it.getOrNull(1)
        }

        val alreadyExists = kaptConfig.dependencies.any {
            it.group == group && it.name == name
        }

        if (!alreadyExists) {
            dependencies.add(configurationName, depNotation)
        }
    }

    fun useSpringConfigurationProcessor(springbootVersion: String? = null) {
        val version = springbootVersion ?: infraProperties.infraBomVersion
        useKaptPlugin("org.springframework.boot:spring-boot-configuration-processor:${version}")
    }


    /**
     * @param kspDependencies dependency artifact.
     * example: "com.example:plugin:0.0.1"
     */
    fun useKspPlugin(vararg kspDependencies: String, kspConfig: (KspExtension.() -> Unit)? = null) {
        project.applyPluginIfNot("com.google.devtools.ksp")
        kspDependencies.forEach { dp ->
            project.addDependencyIfAbsent("ksp", dp)
        }
        kspConfig?.let {
            project.configureFor(KspExtension::class.java, kspConfig)
        }
    }


    fun useKspApi(configurationName: Set<String> = setOf("implementation"), version: String = KSP_VERSION, ) {
        project.dependencies.apply {
            configurationName.forEach {
                project.dependencies.add(it, "com.google.devtools.ksp:symbol-processing-api:${version}")
            }
        }
    }

    fun useInfraOrmGenerator(
        generatorVersion: String = "2.0.+",
        pojoProjectDir: String? = null,
        pojoPackageName: String? = null,
        generateAot: Boolean = true,
        pojoSerializable: Boolean = false,

    ) {
        useKspPlugin("com.labijie.orm:exposed-generator:${generatorVersion}")
        if(pojoSerializable) {
            useKotlinSerializationPlugin()
        }
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
                this.arg("orm.pojo_kotlin_serializable", pojoSerializable.toString())
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
        action.execute(infraProperties)

        if (infraProperties.gitPropertiesPluginEnabled) {
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
            infraProperties
        )

        usePublishPlugin()
        this.project.forceDependencyVersion(InfraDefaultVersions.DEFAULT_KOTLIN_VERSION,
            "org.jetbrains.kotlin",
            "kotlin-stdlib",
            "kotlin-reflect",
            "kotlin-bom")

        this.project.forceDependencyVersion(InfraDefaultVersions.DEFAULT_KOTLIN_SERIALIZATION_VERSION) {
            g,m-> g == "org.jetbrains.kotlinx" && m.startsWith("kotlinx-serialization")
        }
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




}