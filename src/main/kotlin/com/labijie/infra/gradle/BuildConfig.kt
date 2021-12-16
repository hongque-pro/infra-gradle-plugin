package com.labijie.infra.gradle

import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.compareVersion
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.Utils.the
import com.labijie.infra.gradle.internal.NexusSettings
import com.labijie.infra.gradle.internal.PomInfo
import getPropertyOrCmdArgs
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
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
        return project.findProperty("signing.password").isNotNullOrBlank() &&
                project.findProperty("signing.secretKeyRingFile").isNotNullOrBlank() &&
                project.findProperty("signing.keyId").isNotNullOrBlank()
    }

    private fun RepositoryHandler.useDefaultRepositories(useMavenProxy: Boolean = true) {
        mavenLocal()
        if (useMavenProxy) {
            maven {
                it.setUrl(getProxyMavenRepository())
                it.isAllowInsecureProtocol = true
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { it.setUrl("https://repo.spring.io/plugins-release") }
    }

    private fun Project.mustBeRoot(methodName: String) {
        if (this.parent != null) {
            throw IllegalArgumentException("$methodName method only support root project.")
        }
    }


    fun Project.useDefault(
        kotlinVersion: String = "1.6.0",
        jvmVersion: String = "1.8",
        includeSource: Boolean = true,
        useMavenProxy: Boolean = true,
        bomVersion: String? = null
    ) {
        if (this.parent == null) {
            this.buildscript.repositories.apply {
                useDefaultRepositories()
            }
        }

        this.tasks.withType(JavaCompile::class.java) {
            it.sourceCompatibility = jvmVersion
            it.targetCompatibility = jvmVersion
        }


//        this.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
//            kotlinOptions {
//                jvmTarget = jvmVersion
//            }
//        }


        this.configureFor(JavaPluginExtension::class.java) {
            withJavadocJar()
            if (includeSource) {
                withSourcesJar()
            }
        }

        this.tasks.withType(Javadoc::class.java) {
            it.isFailOnError = false
        }

        val proxy = this.getPropertyOrCmdArgs("USE_PROXY", "proxy").orEmpty()

        val useP = if (proxy.equals("true", ignoreCase = true)) {
            true
        } else if (proxy.equals("false", ignoreCase = true)) {
            false
        } else {
            null
        }

        this.repositories.useDefaultRepositories(useP ?: useMavenProxy)

        if (this.tasks.findByName("test") != null) {
            this.tasks.withType(Test::class.java) {
                it.useJUnitPlatform()
            }
        }

        this.dependencies.apply {
            this.add("api", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
            this.add("api", "org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")



            if (!bomVersion.isNullOrBlank()) {

                val mockitoVersionSuffix = if (compareVersion(bomVersion, "2.6.4") < 0) ":4.1.0" else ""

                this.add("api", platform("com.labijie.bom:lib-dependencies:${bomVersion}"))
                this.add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-engine")
                this.add("testImplementation", "org.mockito:mockito-core${mockitoVersionSuffix}")
            } else {
                val junitVersion = "5.8.2"
                val mockitoVersion = "4.1.0"
                this.add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5:${kotlinVersion}")
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

    fun Project.useNexusPublishPlugin(configure: ((repo: NexusSettings) -> Unit)? = null) {
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

    fun Project.usePublishing(publishToGitHub: Boolean, info: PomInfo, artifactName: ((p: Project) -> String)? = null) {

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
                        name.set(info.projectName ?: project.name)
                        description.set(info.description)
                        url.set(info.projectUrl)
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
                        scm {
                            s->
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

            //参考: https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-gradle
            val organ = info.gitHubOwner
            val repo = info.gitHubRepository
            if(publishToGitHub && !organ.isNullOrBlank() && !repo.isNullOrBlank()){
                this.repositories { handler->
                    handler.maven {
                        it.name = "GitHubPackages"
                        it.setUrl("https://maven.pkg.github.com/${organ}/${repo}")
                        it.credentials {credentials->
                            credentials.username  = System.getenv("GITHUB_ACTOR")
                            credentials.password = System.getenv("GITHUB_TOKEN")
                        }
                    }
                }
            }
        }

        this.configureFor(SigningExtension::class.java) {
            val publishing = project.the(PublishingExtension::class)

            if (project.canBeSign()) {
                this.sign(publishing.publications.findByName("maven"))
            } else {
                println("Signing information missing/incomplete for ${project.name}")
            }
        }
    }
}