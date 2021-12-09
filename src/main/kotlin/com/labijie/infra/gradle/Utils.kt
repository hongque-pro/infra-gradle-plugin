package com.labijie.infra.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
object Utils {
    inline fun <reified T : Any> Project.configureFor(clazz: Class<T>, noinline configuration: T.() -> Unit): Unit =
        @Suppress("deprecation")
        clazz.let { type ->
            convention.findByType(type)?.let(configuration)
                ?: convention.findPlugin(clazz)?.let(configuration)
                ?: convention.configure(type, configuration)
        }

    fun PluginAware.apply(from: Any? = null, plugin: String? = null, to: Any? = null) {
        require(from != null || plugin != null) { "At least one of 'from' or 'plugin' must be given." }
        apply {
            if (plugin != null) it.plugin(plugin)
            if (from != null) it.from(from)
            if (to != null) it.to(to)
        }
    }

    fun <T : Any> Project.the(extensionType: KClass<T>): T =
        @Suppress("deprecation") convention.findByType(extensionType.java)
            ?: @Suppress("deprecation") convention.findPlugin(extensionType.java)
            ?: @Suppress("deprecation") convention.getByType(extensionType.java)


    fun Project.getProjectFile(file: String): String {
        val f = File(file)
        return if(f.isAbsolute){
            f.absolutePath
        }else{
            File(this.projectDir, file).absolutePath
        }
    }
}