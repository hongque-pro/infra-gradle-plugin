/**
 * THIS FILE IS PART OF HuanJing (huanjing.art) PROJECT
 * Copyright (c) 2023 huanjing.art
 * @author Huanjing Team
 */
package com.labijie.infra.gradle

import org.gradle.kotlin.dsl.PluginDependenciesSpecScope
import org.gradle.plugin.use.PluginDependencySpec


fun PluginDependenciesSpecScope.springBoot(version: String = DEFAULT_SPRINGBOOT_VERSION, apply: Boolean = true): PluginDependencySpec {
    return id("org.springframework.boot").version(version).apply(apply)
}

fun PluginDependenciesSpecScope.graalvmNativeBuild(version: String = DEFAULT_NATIVE_BUILD_VERSION, apply: Boolean = true): PluginDependencySpec {
    return id("org.graalvm.buildtools.native").version(version).apply(apply)
}