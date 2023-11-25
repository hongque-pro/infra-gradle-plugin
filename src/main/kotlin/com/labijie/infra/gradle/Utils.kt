package com.labijie.infra.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
object Utils {
    val initedProjects = ConcurrentHashMap<Project, Properties>()
    val configuredProjects = ConcurrentHashMap<Project, Boolean>()

    fun isGithubAction(): Boolean
    {
        return !System.getenv("GITHUB_JOB").isNullOrBlank()
    }

    fun isDisableMavenPRoxy(): Boolean
    {
        return !System.getenv("NO_MAVEN_PROXY").isNullOrBlank()
    }

    fun Project.setIsInfraBom(isBom: Boolean) {
        project.extraProperties["infraIsBom"] = isBom
    }

    fun Project.isInfraBomProject(): Boolean {
        if(project.extraProperties.has("infraIsBom"))
        {
            return project.extraProperties.get("infraIsBom") as Boolean
        }
        return false;
    }

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

    fun compareVersion(version1: String, version2: String): Int {
        val versionArray1 = version1.split("\\.").toTypedArray() //注意此处为正则匹配，不能用"."；
        val versionArray2 = version2.split("\\.").toTypedArray()
        var idx = 0
        val minLength = versionArray1.size.coerceAtMost(versionArray2.size) //取最小长度值
        var diff = 0
        while (idx < minLength && versionArray1[idx].length - versionArray2[idx].length.also { diff = it } == 0 //先比较长度
            && versionArray1[idx].compareTo(versionArray2[idx]).also { diff = it } == 0) { //再比较字符
            ++idx
        }
        //如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
        diff = if (diff != 0) diff else versionArray1.size - versionArray2.size
        return diff
    }
}