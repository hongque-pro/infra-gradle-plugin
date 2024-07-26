package com.labijie.infra.gradle.internal

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