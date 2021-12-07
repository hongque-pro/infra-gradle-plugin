# Infra-Gradle-Plugin

![gradle-plugin-portal](https://img.shields.io/gradle-plugin-portal/v/com.labijie.infra?logo=gradle)
![workflow status](https://img.shields.io/github/workflow/status/hongque-pro/infra-gradle-plugin/Gradle%20Build%20And%20Release/main?logo=github)
![license](https://img.shields.io/github/license/hongque-pro/infra-orm?style=flat-square)

该插件旨在简化 Gradle 项目配置，几行代码就可以迅速开始一个项目, 仅支持 Kotlin DSL, Groovy 未测试！

引入插件

```kotlin

plugins {
    id("com.labijie.infra") version "your version"
}

infra {
    useDefault {
        infraBomVersion = "2.6.0" //如果不配置，表示不使用 Infra-Bom, 同时单元测试的依赖也不会被配置
    }

    //发布到 maven 和私有 nexus, 非类库创作者不需要这个方法
    usePublish {
        description = "dxxxx"
        projectUrl = "https://localhost"
        gitUrl = "https://localhost"
        githubScmUrl = "https://localhost"
    }

    //加入 nexus publish 插件做 CI，只适合在跟项目使用这个方法
    useNexusPublish()
}

dependencies {
    //...和原生 DSL 用法一致
}



```