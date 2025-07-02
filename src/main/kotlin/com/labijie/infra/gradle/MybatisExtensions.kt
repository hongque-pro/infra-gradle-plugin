
import InfraDefaultVersions.DEFAULT_MYBATIS_GENERATOR_CORE_VERSION
import InfraDefaultVersions.DEFAULT_MYBATIS_GENERATOR_PLUGIN_VERSION
import InfraDefaultVersions.DEFAULT_MYSQL_CONNECTOR_VERSION
import com.labijie.infra.gradle.InfraPluginExtension
import com.labijie.infra.gradle.Utils.apply
import com.labijie.infra.gradle.Utils.configureFor
import com.thinkimi.gradle.MybatisGeneratorExtension


/**
 * 使用 mybatis 代码生成器
 *
 * 插件： com.thinkimi.gradle.MybatisGenerator
 *
 * 可以配合 itfsw 插件
 *
 * @see <a href="https://github.com/itfsw/mybatis-generator-plugin">https://github.com/itfsw/mybatis-generator-plugin</a>
 *
 * @param configFile XML 配置文件路径，相对路径表示相对项目根目录的路径，也可以设置据对路径
 * @param propertiesFile XML 配置文件中 properties 节使用的文件，相对路径表示相对项目根目录的路径，也可以设置据对路径。
 * 通常格式为:  &lt;properties url="file:///${propertiesFile}"></properties>
 *
 * @param isMysqlDataSource 是否使用 Mysql 数据源生成代码
 * @param enableItfswPlug 是否使用 itfsw 增强插件
 * @param generatorCoreVersion mybatis 官方 generator-core 包版本
 * @param itfswPluginVersion itfsw 插件版本
 * @param mysqlConnectorVersion mysql-connector-java 包版本
 * @param propertiesFileConfigKey XML 中 properties 文件使用的 key
 *
 */
fun InfraPluginExtension.useMybatis(
    configFile: String,
    propertiesFile: String,
    isMysqlDataSource: Boolean = true,
    enableItfswPlug: Boolean = false,
    generatorCoreVersion: String = DEFAULT_MYBATIS_GENERATOR_CORE_VERSION,
    itfswPluginVersion: String = DEFAULT_MYBATIS_GENERATOR_PLUGIN_VERSION,
    mysqlConnectorVersion: String = DEFAULT_MYSQL_CONNECTOR_VERSION,
    propertiesFileConfigKey: String = "propertiesFile"
) {
    val config = "mybatisGenerator"
    project.apply(plugin = "com.thinkimi.gradle.MybatisGenerator")
    project.dependencies.apply {
        add(config, "org.mybatis.generator:mybatis-generator-core:${generatorCoreVersion}")
        if (enableItfswPlug) {
            add(config, "com.itfsw:mybatis-generator-plugin:${itfswPluginVersion}")
        }
        if (isMysqlDataSource) {
            add(config, "com.mysql:mysql-connector-j:${mysqlConnectorVersion}")
        }
    }
    project.configureFor(MybatisGeneratorExtension::class.java) {
        this.configFile = project.getProjectFile(configFile)
        this.overwrite = true
        this.verbose = true
        this.mybatisProperties = objectFactory.mapProperty(String::class.java, String::class.java)

        if (propertiesFileConfigKey.isNotBlank()) {
            this.mybatisProperties.set(
                mapOf(
                    propertiesFileConfigKey to propertiesFile
                )
            )
        }
    }
}