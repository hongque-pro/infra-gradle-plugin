package com.labijie.infra.gradle.internal

import io.github.gradlenexus.publishplugin.NexusRepository

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

    fun isValid(): Boolean {
        return this.username.isNotBlank() && password.isNullOrBlank() && !releaseUrl.isNullOrBlank() && !snapshotUrl.isNullOrBlank()
    }
}