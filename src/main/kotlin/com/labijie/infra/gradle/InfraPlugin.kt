package com.labijie.infra.gradle

import com.labijie.infra.gradle.InfraPlugin.Companion.execute
import com.labijie.infra.gradle.Utils.apply
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.GradleBuild

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

        target.apply(plugin = "kotlin")
        target.apply(plugin = "kotlin-spring")
        target.apply(plugin = "java-library")
        target.extensions.create(InfraExtension.Name, InfraExtension::class.java, target)
//        val build = target.tasks.findByName("build")
//        if(target.parent == null && build != null) {
//            target.tasks.create("fastBuild", Exec::class.java) {
//                it.group = "build"
//                val args = mutableListOf<String>()
//                args.add("gradle")
//                args.add("build")
//                args.add("--parallel")
//                args.add("-x test")
//                if(target.childProjects.any {t->
//                    t.value.tasks.findByName("kspKotlin") != null
//                }){
//                    args.add("-x kspKotlin")
//                }
//                target.gradle.gradleHomeDir
//                GradleBuild().apply {
//                    setTasks(tasks)
//                }
//                it.commandLine(*args.toTypedArray())
//            }
//        }
    }
}