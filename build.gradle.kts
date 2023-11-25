group = "com.labijie.infra"
version = Constants.projectVersion

plugins {

    kotlin("jvm") version Constants.kotlinVersion
    id("com.gradle.plugin-publish") version Constants.gradlePublishPluginVersion
    if(Constants.testPlugin) {
        id("com.labijie.infra") version Constants.projectVersion apply false
    }
    id("maven-publish")

    //for debug comment this line to disable sign
    //id("signing")
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
            tags = listOf("infra", "labijie", "template")
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    // Or shorter:
    jvmToolchain(17)
    // For example:
    jvmToolchain(17)
}



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
}

tasks.withType(JavaCompile::class.java) {
    sourceCompatibility = "17"
    targetCompatibility = "17"
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
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Constants.kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-reflect:${Constants.kotlinVersion}")
    implementation(gradleApi())
    api("io.github.gradle-nexus:publish-plugin:${Constants.publishingPluginVersion}")
    api("org.jetbrains.kotlin:kotlin-allopen:${Constants.kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:${Constants.kotlinVersion}")
    api("${Constants.mybatisPluginLib}:${Constants.mybatisPluginVersion}")
    //use for task gradle dependencyUpdates
    api("com.github.ben-manes:gradle-versions-plugin:${Constants.checkUpdatePlugin}")

    api("com.google.devtools.ksp:symbol-processing-gradle-plugin:${Constants.kspPluginVersion}")
}

registerPubTasks()

