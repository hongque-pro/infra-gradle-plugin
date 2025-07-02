package com.labijie.infra.gradle

import com.labijie.infra.gradle.Utils.TASK_NAME_NATIVE_COMPILE_DEV
import com.labijie.infra.gradle.Utils.TASK_NAME_NATIVE_RUN_DEV
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin
import org.springframework.boot.gradle.tasks.aot.ProcessAot


/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/7
 * @Description:
 */
class InfraPlugin : Plugin<Project> {
    companion object {
        fun Task.execute() {
            val t = this
            this.actions.forEach {
                it.execute(t)
            }
        }

        private fun Project.executeTask(task: Task, lock: Boolean) {
            if (lock) {
                this.project.dependencyLocking.lockAllConfigurations()
            }
            task.taskDependencies.getDependencies(task).forEach { subTask ->
                executeTask(subTask, lock)
            }
            task.execute()
            if (lock) {
                this.project.dependencyLocking.unlockAllConfigurations()
            }
        }

        fun Project.executeTask(task: Task) {
            executeTask(task, false)
        }
    }

    private fun Project.configureFastMode() {
        val pr = this

        if (pr.tasks.findByName("build") != null && pr.tasks.findByName(Utils.TASK_NAME_FAST_BUILD) == null) {
            pr.tasks.register(Utils.TASK_NAME_FAST_BUILD) { task ->
                task.group = "build"
                task.dependsOn("build")
            }

            // 全局提前标记 fast build 运行
            pr.gradle.taskGraph.whenReady { taskGraph ->
                val fastMode = taskGraph.allTasks.any { it.name == Utils.TASK_NAME_FAST_BUILD }

                if (fastMode) {
                    val skipTasks = pr.extensions.findByType(InfraPluginExtension::class.java)?.skipTasks
                    skipTasks?.forEach { taskName ->
                        pr.tasks.findByName(taskName)?.enabled = false
                    }
                }
            }
        }
    }

    private fun registerNativeTasks(project: Project) {

        project.afterEvaluate {
            project.plugins.withId("org.graalvm.buildtools.native") {

                if (project.tasks.findByName("nativeCompile") != null) {
                    // 插件存在时再注册任务
                    project.tasks.register(TASK_NAME_NATIVE_COMPILE_DEV) { task ->
                        task.group = "native"
                        task.description = "Inject spring.profiles.active into nativeCompile and run it"
                        task.dependsOn("nativeCompile")
                    }

                    project.tasks.register(TASK_NAME_NATIVE_RUN_DEV) { task ->
                        task.group = "native"
                        task.description = "Inject spring.profiles.active into nativeRun and run it"

                        task.dependsOn("nativeRun")
                    }


                    project.gradle.taskGraph.whenReady { taskGraph ->
                        val devProfile = taskGraph.allTasks.any { it.name == TASK_NAME_NATIVE_COMPILE_DEV || it.name == TASK_NAME_NATIVE_RUN_DEV }
                        if (devProfile) {
                            taskGraph.allTasks.filter { t ->
                                t.name.equals("processAot", ignoreCase = true) && t.javaClass.name.removeSuffix("_Decorated") == "org.springframework.boot.gradle.tasks.aot.ProcessAot"
                            }.let { tasks ->
                                tasks.forEach {
                                    task->
                                    if(task is JavaExec) {
                                        task.environment.putIfAbsent("SPRING_PROFILES_ACTIVE", "dev,local")
                                        println("Inject spring.profiles.active=dev,local to project : ${project.name}.")
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    override fun apply(target: Project) {
        target.extensions.create(InfraPluginExtension.Name, InfraPluginExtension::class.java, target)
        registerNativeTasks(target)
        target.afterEvaluate {
            p->
            target.configureFastMode()
        }
    }
}