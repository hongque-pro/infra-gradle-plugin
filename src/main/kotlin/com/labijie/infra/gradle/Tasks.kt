/**
 * @author Anders Xiao
 * @date 2024-07-26
 */
package com.labijie.infra.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


open class BuildOnlyTask : DefaultTask() {
    @TaskAction
    fun runEnter() {
        Utils.setFastMode(this.project, true)
    }
}

abstract class ExitFastBuild : DefaultTask() {
    @TaskAction
    fun runEnter() {
        Utils.setFastMode(this.project, false)
    }
}