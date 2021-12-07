rootProject.name = "infra-gradle-plugin"
include("dummy-project")


pluginManagement {

    repositories {
        mavenLocal()

        gradlePluginPortal()
    }
}