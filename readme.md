# Infra-Gradle-Plugin

![gradle-plugin-portal](https://img.shields.io/gradle-plugin-portal/v/com.labijie.infra?logo=gradle)
![workflow status](https://img.shields.io/github/workflow/status/hongque-pro/infra-gradle-plugin/Gradle%20Build%20And%20Release/main?logo=github)
![license](https://img.shields.io/github/license/hongque-pro/infra-orm?style=flat-square)

该插件旨在简化 Gradle 项目配置，几行代码就可以迅速开始一个项目, 仅支持 Kotlin DSL, Groovy 未测试！

## 快速开始

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

## local.properties 支持

支持从 local.properties 中使用配置，从 git 排除 local.properties 以实现本地化配置：
>适用于本地保存密钥，无需担心被提交到 git 中.

```kotlin
val propertyName = XXXXXX
findPropertyAndLocal(propertyName)
```

## GithubPackages 支持

### 使用 GitHub Packages 中的包

在 gradle.build.kts 中添加以下内容:
```kotlin
allprojects {
    infra {
        useDefault {
            addHongQueGitHubPackages() //加入 hongque-pro 的基础包仓库
            addGitHubPackages("<owner>", "<repository>") //特定的 Repository 仓库
        }
    }
}
```

因为 **GitHub Packages** 需要配置授权访问，并不是公开访问，你需要配置 username 和 token. 可以通过 gradle.properties 或 local.properties 配置：
```properties
GPR_USER=<your github account>
GPR_TOKEN=<PTA OR GITHUB_TOKEN>
```
*持环境变量或者 java 的 -D 参数配置*

**GPR_TOKEN** 是消费仓库中的包必须的 PTA， 关于如何生成 PTA 参考这里:   
https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token   

---

### 发布包到 GitHubPackages   
在 gradle.build.kts 中添加以下内容:
```kotlin
infra {
    useGitHubPackages("hongque-pro", "application-framework")
}
```

发布包也需要授权，可以通过 gradle.properties 或 local.properties 配置：

```properties
GPR_USER=<your github account>
GPR_TOKEN=<PTA OR GITHUB_TOKEN>
```
*和消费包一样，也持环境变量或者 java 的 -D 参数配置*
