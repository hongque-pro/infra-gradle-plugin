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

    const val TASK_NAME_FAST_BUILD = "fastBuild"

    private val fastModeList = ConcurrentHashMap<String, Boolean>()
    internal fun isInFastMode(project: Project): Boolean {
        return fastModeList.containsKey(project.path)
    }

    internal fun Project.printFastModelDebug() {
        println("[${this.path}] Fast model: ${isInFastMode(this)}")
        println("fast list:")
        println(fastModeList.keys.joinToString(System.lineSeparator()))
    }

    internal fun setFastMode(project: Project, enabled: Boolean) {
        if(enabled) {
            fastModeList[project.path] = true
        }else {
            fastModeList.remove(project.path)
        }
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
        clazz.let { type ->
            extensions.findByType(type)?.let(configuration)
                ?: extensions.configure(type, configuration)
        }

    fun PluginAware.apply(from: Any? = null, plugin: String? = null, to: Any? = null) {
        require(from != null || plugin != null) { "At least one of 'from' or 'plugin' must be given." }
        apply {
            if (plugin != null) it.plugin(plugin)
            if (from != null) it.from(from)
            if (to != null) it.to(to)
        }
    }

    fun <T : Any> Project.the(extensionType: KClass<T>): T = extensions.getByType(extensionType.java)


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