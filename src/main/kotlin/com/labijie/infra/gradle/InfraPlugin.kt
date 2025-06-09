package com.labijie.infra.gradle

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle


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

    override fun apply(target: Project) {
        target.extensions.create(InfraPluginExtension.Name, InfraPluginExtension::class.java, target)

        target.afterEvaluate {
            p->
            target.configureFastMode()
        }
    }
}