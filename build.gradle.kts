
plugins {
    kotlin("jvm") version Constants.kotlinVersion
    id("com.gradle.plugin-publish") version "0.18.0"
    if(Constants.testPlugin) {
        id("com.labijie.infra") version (Constants.projectVersion) apply false
    }
    id("maven-publish")
    id("signing")
    id("java-gradle-plugin")
    id("java-library")
}


group = "com.labijie.infra"
version = Constants.projectVersion

fun getProxyMavenRepository(): String {
    val proxy: String? = System.getenv("MAVEN_PROXY")?.ifBlank { null }
    return proxy ?: "https://maven.aliyun.com/nexus/content/groups/public/"
}

repositories {
    mavenLocal()
    maven {
        setUrl(getProxyMavenRepository())
        isAllowInsecureProtocol = true
    }
    mavenCentral()
    gradlePluginPortal()
    maven { setUrl("https://repo.spring.io/plugins-release") }
}

tasks.withType(JavaCompile::class.java) {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType(Javadoc::class.java) {
    this.isFailOnError = false
}

if (tasks.findByName("test") != null) {
    tasks.withType(Test::class.java) {
        useJUnitPlatform()
    }
}


dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Constants.kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Constants.kotlinVersion}")
    implementation(gradleApi())
    api("io.github.gradle-nexus:publish-plugin:${Constants.publishingPluginVersion}")
    api("org.jetbrains.kotlin:kotlin-allopen:${Constants.kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:${Constants.kotlinVersion}")
    api("${Constants.mybatisPluginLib}:${Constants.mybatisPluginVersion}")
    api("com.github.ben-manes:gradle-versions-plugin:${Constants.checkUpdatePlugin}")

    compileOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${Constants.kspPluginVersion}")
}


configure<com.gradle.publish.PluginBundleExtension> {
    website = "https://github.com/hongque-pro/infra-gradle-plugin"
    vcsUrl = "https://github.com/hongque-pro/infra-gradle-plugin.git"
    tags = listOf("infra", "labijie", "template")
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("InfraPlugin") {
            id = "com.labijie.infra"
            displayName = "infra gradle plugin"
            description = "Build toolkit to help you configure gradle project"
            implementationClass = "com.labijie.infra.gradle.InfraPlugin"
        }
    }
}

configure<PublishingExtension> {
    repositories {
        maven {
            name = "localPluginRepository"
            url = uri("../local-plugin-repository")
        }
    }
}