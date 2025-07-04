
/**
 *
 * @Author: Anders Xiao
 * @Date: 2023/11/21
 * @Description:
 */


object  InfraDefaultVersions {
    const val DEFAULT_MYBATIS_GENERATOR_CORE_VERSION = "1.4.2"
    const val DEFAULT_MYBATIS_GENERATOR_PLUGIN_VERSION = "1.4.6"
    const val DEFAULT_MYSQL_CONNECTOR_VERSION = "8.4.0"
    const val DEFAULT_KOTLIN_VERSION = "2.2.0"
    const val DEFAULT_JDK_VERSION = "21"
    const val DEFAULT_INFRA_BOM_VERSION = "3.5.3"
    const val DEFAULT_KOTLIN_SERIALIZATION_VERSION = "1.9.0"
}


internal object InfraPlugins {

    const val CheckUpdatePluginId = "com.github.ben-manes.versions"
    const val GitPropertiesPluginId = "com.gorylenko.gradle-git-properties"
}