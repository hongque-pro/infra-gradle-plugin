package com.labijie.infra.gradle

import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.compareVersion
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.Utils.the
import com.labijie.infra.gradle.internal.NexusSettings
import com.labijie.infra.gradle.internal.PomInfo
import com.labijie.infra.gradle.internal.ProjectProperties
import findPropertyAndLocal
import getPropertyOrCmdArgs
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.signing.SigningExtension

internal object BuildConfig {
    private fun Any?.isNotNullOrBlank(): Boolean {
        return !(this == null || this.toString().isBlank())
    }

    private fun getProxyMavenRepository(): String {
        val proxy: String? = System.getenv("MAVEN_PROXY")?.ifBlank { null }
        return proxy ?: "https://maven.aliyun.com/nexus/content/groups/public/"
    }

    private fun Project.canBeSign(): Boolean {
        val project = this
        return project.findPropertyAndLocal("signing.password").isNotNullOrBlank() &&
                project.findPropertyAndLocal("signing.secretKeyRingFile").isNotNullOrBlank() &&
                project.findPropertyAndLocal("signing.keyId").isNotNullOrBlank()
    }

    private fun RepositoryHandler.useDefaultRepositories(
        project: Project,
        useMavenProxy: Boolean = true,
        githubPackages: Map<String, MutableSet<String>>
    ) {
        mavenLocal()
        if (useMavenProxy) {
            maven {
                it.setUrl(getProxyMavenRepository())
                it.isAllowInsecureProtocol = true
            }
        }


        mavenCentral()

        if(githubPackages.isNotEmpty()) {
            val username = project.getPropertyOrCmdArgs("GPR_USER", "GPR_USER")
            val password = project.getPropertyOrCmdArgs("GPR_TOKEN", "GPR_TOKEN")

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

    fun Project.usePublishingRepository(
        repositoryName: String = "Nexus",
        url: () -> String?,
        username: () -> String?,
        password: () -> String?
    ) {

        val p = this
        if (!this.the(PublishingExtension::class).repositories.any { it.name == repositoryName }) {

            p.configureFor(PublishingExtension::class.java) {
                this.repositories { handler ->
                    handler.maven {
                        it.name = repositoryName.ifBlank { "Nexus" }
                        it.setUrl(url().orEmpty())
                        it.credentials { c ->
                            c.username = username().orEmpty()
                            c.password = password().orEmpty()
                        }
                    }
                }
            }
        }

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


    fun Project.useDefault(
        isBom: Boolean,
        projectProperties: ProjectProperties
    ) {
        val proxy = this.getPropertyOrCmdArgs("USE_PROXY", "proxy").orEmpty()

        val useP = if (proxy.equals("true", ignoreCase = true)) {
            true
        } else if (proxy.equals("false", ignoreCase = true)) {
            false
        } else {
            null
        }

        this.repositories.useDefaultRepositories(this,useP ?: projectProperties.useMavenProxy, projectProperties.githubRepositories)

        if (isBom) {
            return
        }

        this.tasks.withType(JavaCompile::class.java) {
            it.sourceCompatibility = projectProperties.jvmVersion
            it.targetCompatibility = projectProperties.jvmVersion
        }

        this.configureFor(JavaPluginExtension::class.java) {
            if(projectProperties.includeDocument) {
                withJavadocJar()
            }
            if (projectProperties.includeSource) {
                withSourcesJar()
            }
        }

        this.tasks.withType(Javadoc::class.java) {
            it.isFailOnError = false
        }



        if (this.tasks.findByName("test") != null) {
            this.tasks.withType(Test::class.java) {
                it.useJUnitPlatform()
            }
        }

        this.dependencies.apply {
            this.add("api", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${projectProperties.kotlinVersion}")
            this.add("api", "org.jetbrains.kotlin:kotlin-reflect:${projectProperties.kotlinVersion}")



            if (projectProperties.infraBomVersion.isNotBlank()) {

                val mockitoVersionSuffix =
                    if (compareVersion(projectProperties.infraBomVersion, "2.6.4") < 0) ":4.1.0" else ""

                this.add("api", platform("com.labijie.bom:lib-dependencies:${projectProperties.infraBomVersion}"))
                this.add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-engine")
                this.add("testImplementation", "org.mockito:mockito-core${mockitoVersionSuffix}")
            } else {
                val junitVersion = "5.8.2"
                val mockitoVersion = "4.1.0"
                this.add(
                    "testImplementation",
                    "org.jetbrains.kotlin:kotlin-test-junit5:${projectProperties.kotlinVersion}"
                )
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-api:${junitVersion}")
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
                this.add("testImplementation", "org.mockito:mockito-core:${mockitoVersion}")
            }
            /**
            testImplementation "org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version"
            testImplementation "org.junit.jupiter:junit-jupiter-api"
            testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine"
            testImplementation "org.mockito:mockito-all"
             */
        }
    }


    /**
     * @param newHost Users registered in Sonatype after 24 February 2021 need to set this value to true
     */
    fun Project.useNexusPublishPlugin(newHost:Boolean, configure: ((repo: NexusSettings) -> Unit)? = null) {
        this.mustBeRoot("useNexusPublishPlugin")
        this.apply(plugin = "io.github.gradle-nexus.publish-plugin")
        if (this.extensions.findByName("nexusPublishing") != null) {
            this.extensions.configure(NexusPublishExtension::class.java) {
                val u = project.getPropertyOrCmdArgs("PUB_USER", "u")
                val p = project.getPropertyOrCmdArgs("PUB_PWD", "p")
                val s = project.getPropertyOrCmdArgs("PUB_URL", "s")
                val settings = NexusSettings(u ?: "", p ?: "")
                settings.snapshotUrl = s
                settings.releaseUrl = s

                it.repositories.apply {
                    sonatype { st ->
                        st.apply {
                            if(newHost){
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
                    if (configure != null || !s.isNullOrBlank()) {
                        configure?.invoke(settings)
                        if (settings.isValid()) {
                            create("nexus") { nexus ->
                                nexus.apply {
                                    nexusUrl.set(uri(settings.releaseUrl.orEmpty()))
                                    snapshotRepositoryUrl.set(uri(settings.snapshotUrl.orEmpty()))
                                    username.set(settings.username)
                                    password.set(settings.password)
                                    allowInsecureProtocol.set(settings.allowInsecureProtocol)
                                }
                            }
                        } else {
                            project.logger.warn("Private nexus settings invalid, make sure username, password, releaseUrl and snapshotUrl can not be null or empty.")
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


    fun Project.usePublishing(info: PomInfo, artifactName: ((p: Project) -> String)? = null) {

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
}