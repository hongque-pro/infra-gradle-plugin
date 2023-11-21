package com.labijie.infra.gradle.internal

import com.labijie.infra.gradle.Utils.configureFor
import com.labijie.infra.gradle.Utils.the
import getPropertyOrCmdArgs
import io.github.gradlenexus.publishplugin.NexusRepository
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import java.time.Duration

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/8
 * @Description:
 */
class NexusSettings() {

    var connectTimeout = Duration.ofMinutes(5)
    var clientTimeout = Duration.ofMinutes(5)
    var checkRetry: Int = 300
    var checkInterval = Duration.ofSeconds(5)

}