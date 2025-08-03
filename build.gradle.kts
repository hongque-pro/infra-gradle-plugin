val pluginVersion = "2.1.1"

group = "com.labijie.infra"
version = pluginVersion

val jdk_version = 21

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly("org.graalvm.buildtools:native-gradle-plugin:${Constants.nativeBuildPlugin}")
    compileOnly("org.springframework.boot:spring-boot-gradle-plugin:${Constants.springbootPlugin}")
    compileOnly("${Constants.mybatisPluginLib}:${Constants.mybatisPluginVersion}")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Constants.kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-reflect:${Constants.kotlinVersion}")
    implementation(gradleApi())
    api("io.github.gradle-nexus:publish-plugin:${Constants.publishingPluginVersion}")
    api("org.jetbrains.kotlin:kotlin-allopen:${Constants.kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:${Constants.kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-serialization:${Constants.kotlinVersion}")
    api("com.google.devtools.ksp:symbol-processing-gradle-plugin:${Constants.kspPluginVersion}")

    api(Constants.gitPropertiesPluginArtifact)
    api(Constants.checkUpdatePluginArtifact)
}

plugins {

    kotlin("jvm") version Constants.kotlinVersion
    id("com.gradle.plugin-publish") version Constants.gradlePublishPluginVersion
    if(Constants.testPlugin) {
        id("com.labijie.infra") version pluginVersion apply false
    }
    id("maven-publish")
    id("java-gradle-plugin")
    id("java-library")
}

gradlePlugin {
    plugins {
        create("InfraPlugin") {
            id = "com.labijie.infra"
            displayName = "infra gradle plugin"
            description = "Build toolkit to help you configure gradle project"
            implementationClass = "com.labijie.infra.gradle.InfraPlugin"
            tags = listOf("infra", "labijie", "template", "spring")
        }
    }

    website = "https://github.com/hongque-pro/infra-gradle-plugin"
    vcsUrl = "https://github.com/hongque-pro/infra-gradle-plugin.git"
}

//for com.gradle.plugin-publish
//refer: https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html
publishing {
    repositories {
        maven {
            name = "Folder"
            url = uri("../local-plugin-repository")
        }
    }
}


//refer: https://docs.gradle.org/current/userguide/signing_plugin.html
//signing {
//    publishing.publications.forEach {
//        System.out.println(it.name)
//    }
//    isRequired =  false
//}

java {
    sourceCompatibility =  JavaVersion.toVersion(jdk_version)
    targetCompatibility = JavaVersion.toVersion(jdk_version)
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(jdk_version)
    }
}

kotlin {
    // Or shorter:
    jvmToolchain(jdk_version)
}



fun getProxyMavenRepository(): String? {
    return System.getenv("MAVEN_PROXY")?.ifBlank { null }
}

repositories {
    mavenLocal()
    getProxyMavenRepository()?.let {
        maven {
            setUrl(it)
            isAllowInsecureProtocol = true
        }
    }

    mavenCentral()
    gradlePluginPortal()
}

tasks.withType(JavaCompile::class.java) {
    sourceCompatibility = "$jdk_version"
    targetCompatibility = "$jdk_version"
}

tasks.withType(Javadoc::class.java) {
    this.isFailOnError = false
}

if (tasks.findByName("test") != null) {
    tasks.withType(Test::class.java) {
        useJUnitPlatform()
    }
}


registerPubTasks()

