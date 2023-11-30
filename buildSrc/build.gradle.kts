
plugins {
    `kotlin-dsl`
}

fun getProxyMavenRepository(): String {
    val proxy: String? = System.getenv("MAVEN_PROXY")?.ifBlank { null }
    if(proxy != null)
    {
        System.out.printf("Find environment variable 'MAVEN_PROXY' for maven mirror.")
    }
    return proxy ?: "https://maven.aliyun.com/nexus/content/groups/public/"
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    maven {
        this.setUrl(getProxyMavenRepository())
        this.isAllowInsecureProtocol = true
    }
    mavenCentral()
}

