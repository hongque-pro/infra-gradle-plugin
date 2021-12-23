package com.labijie.infra.gradle.internal

import io.github.gradlenexus.publishplugin.NexusRepository
import java.time.Duration

/**
 *
 * @Author: Anders Xiao
 * @Date: 2021/12/8
 * @Description:
 */
class NexusSettings(var username: String, var password: String?) {
    var releaseUrl: String? = null
    var snapshotUrl: String? = null
    var allowInsecureProtocol: Boolean = true

    var connectTimeout = Duration.ofMinutes(5)
    var clientTimeout = Duration.ofMinutes(5)
    var checkRetry: Int = 300
    var checkInterval = Duration.ofSeconds(5)

    fun isValid(): Boolean {
        return this.username.isNotBlank() && password.isNullOrBlank() && !releaseUrl.isNullOrBlank() && !snapshotUrl.isNullOrBlank()
    }
}