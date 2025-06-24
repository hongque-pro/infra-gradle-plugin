package com.labijie.infra.gradle

import com.labijie.infra.gradle.BuildConfig.getGithubActor
import com.labijie.infra.gradle.BuildConfig.getGithubToken
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.applyPluginIfNot
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.Utils.the
import com.labijie.infra.gradle.internal.NexusSettings
import com.labijie.infra.gradle.internal.PomInfo
import com.labijie.infra.gradle.internal.ProjectProperties
import findPropertyAndLocal
import getPropertyOrCmdArgs
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.extensions.core.extra
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain

internal object BuildConfig {

    private const val githubUserExtra: String = "__GITHUB_ACCESS_USER"
    private const val githubTokenExtra: String = "__GITHUB_ACCESS_PASSWORD"

    private fun Any?.isNotNullOrBlank(): Boolean {
        return !(this == null || this.toString().isBlank())
    }

    private fun Project.getProxyMavenRepository(): String {

        val proxy: String? = this.getPropertyOrCmdArgs("MAVEN_PROXY", "maven.proxy")?.ifBlank { null }
        return proxy ?: "https://maven.aliyun.com/nexus/content/groups/public/"
    }

    private fun Project.canBeSign(): Boolean {
        val project = this
        return project.findPropertyAndLocal("signing.password").isNotNullOrBlank() &&
                project.findPropertyAndLocal("signing.secretKeyRingFile").isNotNullOrBlank() &&
                project.findPropertyAndLocal("signing.keyId").isNotNullOrBlank()
    }

    fun Project.useGithubAccount(
        actor: String,
        token: String
    ) {
        this.extraProperties.set(githubUserExtra, actor)
        this.extraProperties.set(githubTokenExtra, token)
    }

    private fun RepositoryHandler.useDefaultRepositories(
        project: Project,
        useMavenProxy: Boolean = true,
        githubPackages: Map<String, MutableSet<String>>
    ) {
        mavenLocal()
        if (useMavenProxy) {
            maven {
                it.setUrl(project.getProxyMavenRepository())
                it.isAllowInsecureProtocol = true
            }
        }
        mavenCentral()

        if (githubPackages.isNotEmpty()) {
            val username = project.getPropertyOrCmdArgs("GITHUB_ACTOR", "gpr.user")
            val password = project.getPropertyOrCmdArgs("GITHUB_TOKEN", "gpr.key")

            if (username.isNotNullOrBlank() && password.isNotNullOrBlank()) {
                var count = 0
                githubPackages.forEach { (key, values) ->
                    values.forEach { r ->
                        maven { m ->
                            count++
                            val url = "https://maven.pkg.github.com/${key}/${r}"
                            m.setUrl(url)
                            m.name = "${key}.${r}"
                            m.credentials {
                                it.username = username
                                it.password = password
                            }
                        }
                    }
                }

                project.logger.info("$count github package added.")
            } else {
                project.logger.warn("Github credentials not found, skip github packages.")
            }
        }

        gradlePluginPortal()
    }

    private fun Project.mustBeRoot(methodName: String) {
        if (this.parent != null) {
            throw IllegalArgumentException("$methodName method only support root project.")
        }

    }

    private fun getJvmTarget(version: String): JvmTarget {
        val javaVersion = JavaVersion.toVersion(version)
        val normalizedVersion = when (javaVersion) {
            JavaVersion.VERSION_1_8 -> "8"
            JavaVersion.VERSION_1_9 -> "9"
            else -> javaVersion.toString()
        }

        return try {
            JvmTarget.fromTarget(normalizedVersion)
        } catch (_: IllegalArgumentException) {
            // Kotlin 不支持的 jvmTarget（比如 "21"），回退到 JVM_17 或 JVM_20
            println("⚠️ Kotlin compiler does not support jvmTarget = $normalizedVersion. Falling back to JVM_17")
            JvmTarget.JVM_17
        }
    }

    fun Project.setupMockitoAgent() {
        val p = this

        this.afterEvaluate { self ->

            val hasVersion = p.rootProject.extra.has("mockitoVersion")
            if(!hasVersion) {
                val v = p.getTestResolvedVersion("org.mockito", "mockito-core")
                if(v == null) {
                    return@afterEvaluate
                }
                p.rootProject.extra.set("mockitoVersion", v)
            }
            val mockitoVersion = p.rootProject.extra.get("mockitoVersion")

            val mockitoAgent = p.configurations.findByName("mockitoAgent")
                ?: p.configurations.create("mockitoAgent") {
                    it.isCanBeResolved = true
                    it.isCanBeConsumed = false
                    it.isVisible = false
                }

            val alreadyAdded = mockitoAgent.dependencies.any {
                it.group == "org.mockito" && it.name == "mockito-core"
            }

            if (!alreadyAdded) {
                val dep = p.dependencies.create("org.mockito:mockito-core:$mockitoVersion") as ModuleDependency
                dep.isTransitive = false
                p.dependencies.add("mockitoAgent", dep)
            }

            p.tasks.withType(Test::class.java).configureEach { task ->
                task.doFirst {
                    val agentJar = mockitoAgent
                        .resolvedConfiguration
                        .resolvedArtifacts
                        .firstOrNull()
                        ?.file

                    if (agentJar != null) {
                        task.jvmArgs("-javaagent:$agentJar")
                        p.logger.lifecycle("Mockito agent injected: $agentJar")
                    } else {
                        p.logger.warn("Mockito agent JAR not found.")
                    }
                }
            }
        }
    }

    private fun Project.getTestResolvedVersion(group: String, name: String): String? {

        val testImplementation = this.configurations.findByName("testImplementation") ?: return null

        // 创建一个可解析的配置来继承 testImplementation
        val resolvableConfig = this.configurations.create("resolvableTestImplForMockito") {
            it.isCanBeResolved = true
            it.isCanBeConsumed = false
            it.extendsFrom(testImplementation)
        }

        // 解析依赖，确保 BOM 能解析出 mockito-core 的版本号
        this.logger.lifecycle("Resolving testImplementation configuration for $group:$name...")
        val resolved = try {
            resolvableConfig.resolvedConfiguration.firstLevelModuleDependencies
        } catch (e: Exception) {
            this.logger.warn("Failed to resolve testImplementation: ${e.message}")
            return null
        }

        val mockitoResolved = resolved.find {
            it.moduleGroup == group && it.moduleName == name
        }

        if (mockitoResolved == null) {
            this.logger.lifecycle("No $name found in testImplementation.")
            return null
        }

        val version = mockitoResolved.moduleVersion
        if (version == null) {
            this.logger.warn("$name dependency found, but version is unknown.")
            return null
        }
        return version
    }

    fun Project.useDefault(
        isBom: Boolean,
        projectProperties: ProjectProperties
    ) {
        this.repositories.useDefaultRepositories(
            this,
            projectProperties.useMavenProxy,
            projectProperties.githubRepositories
        )

        if (isBom) {
            return
        }

        project.applyPluginIfNot("org.jetbrains.kotlin.jvm")


        val jvm = getJvmTarget(projectProperties.jdkVersion)
        project.logger.info("Use jvm target: ${jvm.target}")

        this.tasks.withType(JavaCompile::class.java).all {
            it.sourceCompatibility = jvm.target
            it.targetCompatibility = jvm.target
            it.options.encoding = "UTF-8"
        }

        this.tasks.withType(KotlinJvmCompile::class.java).all {
            it.compilerOptions {
                jvmTarget.set(jvm)
            }
        }

        this.tasks.withType(KotlinCompile::class.java).all {
            it.compilerOptions {
                jvmTarget.set(jvm)// 改成已支持的目
            }
        }

        this.configureFor(JavaPluginExtension::class.java) {
            sourceCompatibility = JavaVersion.toVersion(jvm.target)
            targetCompatibility = JavaVersion.toVersion(jvm.target)
        }


        val service = project.extensions.getByType(JavaToolchainService::class.java)
        val customLauncher = service.launcherFor {
            it.languageVersion.set(JavaLanguageVersion.of(projectProperties.jdkVersion))
        }

        project.tasks.withType(UsesKotlinJavaToolchain::class.java).configureEach {
            it.kotlinJavaToolchain.toolchain.use(customLauncher)
        }


        this.configureFor(JavaPluginExtension::class.java) {
            if (projectProperties.includeDocument) {
                withJavadocJar()
            }
            if (projectProperties.includeSource) {
                withSourcesJar()
            }
        }

        this.tasks.withType(Javadoc::class.java) {
            it.isFailOnError = false
            it.options.encoding = "UTF-8"
        }


        if (this.tasks.findByName("test") != null) {
            this.tasks.withType(Test::class.java) {
                it.useJUnitPlatform()
            }
        }



        this.dependencies.apply {
            this.add("api", platform("org.jetbrains.kotlin:kotlin-bom:${InfraConstants.DEFAULT_KOTLIN_VERSION}"))
            this.add("api", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
            this.add("api", "org.jetbrains.kotlin:kotlin-reflect")

            if (projectProperties.infraBomVersion.isNotBlank()) {
                this.add("api", platform("com.labijie.bom:lib-dependencies:${projectProperties.infraBomVersion}"))
            } else {

                this.add("api", platform("org.junit:junit-bom:${Utils.DefaultJunitVersion}"))
                this.add("api", platform("org.mockito:mockito-bom:${Utils.DefaultMockitoVersion}"))
            }


            this.add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
            this.add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
            this.add("testImplementation", "org.junit.jupiter:junit-jupiter-params")
            this.add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
            this.add("testImplementation", "org.mockito:mockito-core")
            this.add("testImplementation", "org.mockito:mockito-junit-jupiter")
        }

        if(project.pluginManager.hasPlugin("org.graalvm.buildtools.native")) {
            project.plugins.withId("org.graalvm.buildtools.native") {
                project.extensions.configure(GraalVMExtension::class.java) { extension ->
                    extension.binaries.named("test") {
                        val contains = it.buildArgs.get().any { arg -> arg.startsWith("--initialize-at-build-time") }
                        if (!contains) {
                            it.buildArgs.add("--initialize-at-build-time")
                        }
                    }
                    extension.binaries.named("main"){ binary ->
                        binary.resources { resources ->
                            resources.includedPatterns.add("application.yml")
                            resources.includedPatterns.add("application-*.yml")
                            resources.includedPatterns.add("logback.xml")
                            resources.includedPatterns.add("git-info/git.properties")
                        }
                    }
                }
            }
        }

        val hasMockito = configurations.findByName("testImplementation")
            ?.dependencies
            ?.any { it.group == "org.mockito" && it.name == "mockito-core" }
            ?: false

        val hasTest = this.tasks.findByName("test") != null

        if(hasMockito && hasTest) {
            this.setupMockitoAgent()
        }

    }


    /**
     * use io.github.gradle-nexus.publish-plugin to publish package to maven repository.
     * @param newHost Users registered in Sonatype after 24 February 2021 need to set this value to true
     */
    fun Project.useNexusPublishPlugin(newHost: Boolean, configure: ((repo: NexusSettings) -> Unit)? = null) {
        if (!this.extraProperties.has("__hasNexusPublishPlugin")) {
            this.extraProperties["__hasNexusPublishPlugin"] = true
            this.mustBeRoot("useNexusPublishPlugin")
            this.apply(plugin = "io.github.gradle-nexus.publish-plugin")
            if (this.extensions.findByName("nexusPublishing") != null) {
                this.extensions.configure(NexusPublishExtension::class.java) {
                    val u = project.getPropertyOrCmdArgs("MAVEN_USER", "maven.user")
                    val p = project.getPropertyOrCmdArgs("MAVEN_KEY", "maven.password")
                    val settings = NexusSettings()
                    configure?.invoke(settings)
                    it.repositories.apply {
                        sonatype { st ->
                            st.apply {
                                if (newHost) {
                                    nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                                    snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                                }
                                if (u != null) {
                                    username.set(u)
                                    if (p != null) {
                                        password.set(p)
                                    }
                                }
                            }
                        }
                    }

                    it.connectTimeout.set(settings.connectTimeout)
                    it.clientTimeout.set(settings.clientTimeout)

                    it.transitionCheckOptions { options ->
                        // We have many artifacts so Maven Central takes a long time on its compliance checks. This sets
                        // the timeout for waiting for the repository to close to a comfortable 50 minutes.
                        options.maxRetries.set(settings.checkRetry)
                        options.delayBetween.set(settings.checkInterval)
                    }
                }
            }
        }
    }


    /**
     * Configure maven publish pom info
     */
    fun Project.configurePublishing(info: PomInfo, artifactName: ((p: Project) -> String)? = null) {

        this.apply(plugin = "maven-publish")
        this.apply(plugin = "signing")

        val project = this
        val artifact = artifactName?.invoke(project) ?: project.name



        this.configureFor(PublishingExtension::class.java) {
            publications { pub ->
                pub.create("maven", MavenPublication::class.java).apply {
                    artifactId = artifact
                    from((components.findByName("javaPlatform") ?: components.findByName("java")))
                    pom.apply {
                        name.set(info.projectName ?: artifact)
                        description.set(info.description)
                        url.set(info.projectUrl)
                        version = project.version.toString()
                        licenses { spec ->
                            spec.license { l ->
                                l.name.set(info.licenseName)
                                l.url.set(info.licenseUrl)
                            }
                        }
                        developers { spec ->
                            spec.developer { d ->
                                d.id.set(info.developerName)
                                d.name.set(info.developerName)
                                d.email.set(info.developerMail)
                            }

                        }
                        scm { s ->
                            s.url.set(info.projectUrl)
                            s.connection.set(info.githubScmUrl)
                            s.developerConnection.set(info.gitUrl)
                        }

                        versionMapping { strategy ->
                            strategy.allVariants { v ->
                                v.fromResolutionResult()
                            }
                        }
                    }
                }
            }
        }

        this.configureFor(SigningExtension::class.java) {
            val publishing = project.the(PublishingExtension::class)

            if (project.canBeSign()) {
                this.sign(publishing.publications.findByName("maven"))
            }
        }
    }

    fun Project.usePublishRepository(
        repositoryName: String = "Nexus",
        url: () -> String?,
        username: () -> String?,
        password: () -> String?,
        allowInsecureProtocol: (() -> Boolean)? = null,
    ) {

        val p = this
        if (!p.the(PublishingExtension::class).repositories.any { it.name == repositoryName }) {

            p.configureFor(PublishingExtension::class.java) {
                this.repositories { handler ->
                    handler.maven {
                        it.name = repositoryName.ifBlank { "Nexus" }
                        it.setUrl(url().orEmpty())
                        it.credentials { c ->
                            c.username = username().orEmpty()
                            c.password = password().orEmpty()
                        }
                        it.isAllowInsecureProtocol = allowInsecureProtocol?.invoke() ?: true
                    }
                }
            }
        }

        /**
         *
         *                         if (settings.isValid()) {
         *                             create("nexus") { nexus ->
         *                                 nexus.apply {
         *                                     nexusUrl.set(uri(settings.releaseUrl.orEmpty()))
         *                                     snapshotRepositoryUrl.set(uri(settings.snapshotUrl.orEmpty()))
         *                                     username.set(settings.username)
         *                                     password.set(settings.password)
         *                                     allowInsecureProtocol.set(settings.allowInsecureProtocol)
         *                                 }
         *                             }
         *                         } else {
         *                             project.logger.warn("Private nexus settings invalid, make sure username, password, releaseUrl and snapshotUrl can not be null or empty.")
         *                         }
         */

        val tt = p.tasks.register("publishTo${repositoryName}") { task ->
            task.description = "Publishes maven publications  to the GitHub Packages."
            task.group = PublishingPlugin.PUBLISH_TASK_GROUP
        }
        val mavenPublications =
            p.the(PublishingExtension::class).publications.withType(MavenPublication::class.java)
        mavenPublications.configureEach { _ ->
            val publishTask = p.tasks.named(
                "publishMavenPublicationTo${repositoryName}Repository"
            )
            tt.configure {
                it.dependsOn(publishTask)
            }
        }

    }

    fun Project.useNexusPub(
        url: String? = null,
        username: String? = null,
        password: String? = null,
    ) {
        this.usePublishRepository(
            "Nexus",
            {
                project.getPropertyOrCmdArgs("NEXUS_URL", "nexus.url") ?: url
            },
            {
                project.getPropertyOrCmdArgs("NEXUS_USER", "nexus.user") ?: username
            },
            {
                project.getPropertyOrCmdArgs("NEXUS_KEY", "nexus.key") ?: password
            })
    }

    /**
     * Publish maven package to github packages.
     *
     * refer:
     * https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-gradle
     *
     * @param getGithubActor github actor, can be get in github action auto.
     * @param getGithubToken github token, can be get in github action auto.
     *
     * Abort githubActor and githubToken more details:
     * https://docs.github.com/en/actions/security-guides/automatic-token-authentication
     */
    fun Project.useGitHubPackagesPub(
        owner: String,
        repository: String
    ) {
        val repoName = "GitHubPackages"
        val url = "https://maven.pkg.github.com/${owner}/${repository}"

        this.usePublishRepository(
            repoName,
            { url },
            { getGithubActor() },
            { getGithubToken() },
            { false }
        )
    }

    private fun Project.getGithubActor(): String {
        val defaultUser = if (project.extraProperties.has(githubUserExtra)) project.extraProperties.get(githubUserExtra)
            .toString() else ""
        return project.getPropertyOrCmdArgs("GITHUB_ACTOR", "gpr.user") ?: defaultUser
    }

    private fun Project.getGithubToken(): String {
        val defaultToken =
            if (project.extraProperties.has(githubTokenExtra)) project.extraProperties.get(githubTokenExtra)
                .toString() else ""
        return project.getPropertyOrCmdArgs("GITHUB_TOKEN", "gpr.key") ?: defaultToken;
    }
}