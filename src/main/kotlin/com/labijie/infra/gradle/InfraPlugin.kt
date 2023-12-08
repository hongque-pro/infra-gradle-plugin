package com.labijie.infra.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

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
            if(lock) {
                this.project.dependencyLocking.lockAllConfigurations()
            }
            task.taskDependencies.getDependencies(task).forEach {
                    subTask->
                executeTask(subTask, lock)
            }
            task.execute()
            if(lock) {
                this.project.dependencyLocking.unlockAllConfigurations()
            }
        }

        fun Project.executeTask(task: Task) {
            executeTask(task, false)
        }
    }

    override fun apply(target: Project) {
       target.extensions.create(InfraPluginExtension.Name, InfraPluginExtension::class.java, target)
    }
}