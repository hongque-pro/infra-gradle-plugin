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
            this.actions.forEach {
                it.execute(this)
            }
        }

        fun executeTask(task: Task) {
            task.taskDependencies.getDependencies(task).forEach {
                    subTask -> executeTask(subTask)
            }
            task.execute()
        }
    }

    override fun apply(target: Project) {
        target.extensions.create(InfraPluginExtension.Name, InfraPluginExtension::class.java, target)
    }
}