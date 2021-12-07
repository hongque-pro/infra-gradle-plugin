package com.labijie.infra.gradle

import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.Utils.the
import com.labijie.infra.gradle.internal.PomInfo
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.buildinit.plugins.internal.KotlinGradlePluginProjectInitDescriptor
import org.gradle.plugins.signing.SigningExtension

internal object BuildConfig {
    private fun Any?.isNotNullOrBlank(): Boolean {
        return !(this == null || this.toString().isBlank())
    }

    private fun getProxyMavenRepository(): String {
        val proxy: String? = System.getenv("MAVEN_PROXY")?.ifBlank { null }
        return proxy ?: "https://maven.aliyun.com/nexus/content/groups/public/"
    }

    fun Project.canBeSign(): Boolean {
        val project = this
        return project.findProperty("signing.password").isNotNullOrBlank() &&
                project.findProperty("signing.secretKeyRingFile").isNotNullOrBlank() &&
                project.findProperty("signing.keyId").isNotNullOrBlank()
    }

    fun Project.getStringProperty(propertyName: String, defaultValue: String? = null): String? {
        val v = this.findProperty(propertyName)?.toString()
        return v ?: defaultValue
    }

    fun Project.getPropertyOrCmdArgs(propertyName: String, cmdArgName: String): String? {
        val project = this
        val propertyValue = project.getStringProperty(propertyName)
        return (System.getProperty(cmdArgName) ?: propertyValue) ?: System.getenv(propertyName)?.ifEmpty { null }
    }

    fun RepositoryHandler.useDefaultRepositories(useMavenProxy: Boolean = true) {
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
        bomVersion: String? = null,
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

        this.repositories.useDefaultRepositories(useMavenProxy)

        if (this.tasks.findByName("test") != null) {
            this.tasks.withType(Test::class.java) {
                it.useJUnitPlatform()
            }
        }

        this.dependencies.apply {
            if (!bomVersion.isNullOrBlank()) {
                this.add("implementation", platform("com.labijie.bom:lib-dependencies:${bomVersion}"))
                this.add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
                this.add("testImplementation", "org.junit.jupiter:junit-jupiter-engine")
                this.add("testImplementation", "org.mockito:mockito-all")
            }
            this.add("api", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
            this.add("api", "org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
            /**
            testImplementation "org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version"
            testImplementation "org.junit.jupiter:junit-jupiter-api"
            testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine"
            testImplementation "org.mockito:mockito-all"
             */
        }
    }

    fun Project.useNexusPublishPlugin() {
        this.mustBeRoot("useNexusPublishPlugin")

        this.apply(plugin = "io.github.gradle-nexus.publish-plugin")
        if (this.extensions.findByName("nexusPublishing") != null) {
            this.extensions.configure(NexusPublishExtension::class.java) {
                val u = project.getPropertyOrCmdArgs("PUB_USER", "u")
                val p = project.getPropertyOrCmdArgs("PUB_PWD", "p")
                val s = project.getPropertyOrCmdArgs("PUB_URL", "s") ?: "https://your-nexus-server.com/publish"
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
                    create("nexus") { nexus ->
                        nexus.apply {
                            nexusUrl.set(uri(s))
                            //snapshotRepositoryUrl.set(uri("https://your-server.com/snapshots"))
                            if (u != null) {
                                username.set(u) // defaults to project.properties["nexusUsername"]
                                if (p != null) {
                                    password.set(p) // defaults to project.properties["nexusPassword"]
                                }
                            }
                        }
                    }
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
                        name.set(info.projectName ?: project.name)
                        description.set(info.description)
                        url.set(info.projectUrl)
                        licenses { l ->
                            l.license {
                                name.set(info.licenseName)
                                url.set(info.licenseUrl)
                            }
                        }
                        developers { d ->
                            d.developer {
                                it.id.set(info.developerName)
                                it.name.set(info.developerName)
                                it.email.set(info.developerMail)
                            }

                        }
                        scm {
                            it.url.set(info.projectUrl)
                            it.connection.set(info.githubScmUrl)
                            it.developerConnection.set(info.gitUrl)
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