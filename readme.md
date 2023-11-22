# Infra-Gradle-Plugin

![gradle-plugin-portal](https://img.shields.io/gradle-plugin-portal/v/com.labijie.infra?logo=gradle)
![workflow status](https://img.shields.io/github/actions/workflow/status/hongque-pro/infra-gradle-plugin/build.yml?branch=main)
![license](https://img.shields.io/github/license/hongque-pro/infra-orm?style=flat-square)

该插件旨在简化 Gradle 项目配置，几行代码就可以迅速开始一个项目。   
<mark>注意</mark>: 仅支持 Kotlin DSL 的 gradle 项目。

## 快速开始

```kotlin-dsl
plugins {
    id("com.labijie.infra") version "your version"
}

infra {
    useDefault {
        infraBomVersion = "3.1.5" //如果不配置，表示不使用 Infra-Bom, 同时单元测试的依赖也不会被配置
        jvmVersion = "17"
    }

    //配置发布包的信息，如果不需要发布包到 maven 或 nexus 无需配置
    publishing {
        pom {
            description = "dxxxx"
            projectUrl = "https://localhost"
            githubScmUrl = "https://localhost"
            githubUrl("hongque-pro", "infra-test")
        }
    }

    //加入 nexus publish 插件, 如果不需要发布包到 maven 或 nexus 无需配置
    usePublishPlugin()
}

dependencies {
    //...和原生 DSL 用法一致
}



```

## local.properties 支持

支持从 local.properties 文件中使用配置，从 git 排除 local.properties 以实现本地化配置：
>适用于本地保存密钥，无需担心被提交到 git 中.

```kotlin
val propertyName = XXXXXX
findPropertyAndLocal(propertyName)
```

## Github Packages 支持

### 配置 Gihub账号
因为 **GitHub Packages** 需要配置授权访问，并不是公开访问，你需要配置 username 和 token，支持如下的方式配置

#### 1. 通过文件 `gradle.properties` 或 `local.properties` 配置
```properties
gpr.user=<your github account>
gpr.key=<PTA OR GITHUB_TOKEN>
```

#### 2. 通过环境变量配置
`gpr.user` 也可以通过环境变量 `GITHUB_ACTOR` 配置   
`gpr.key` 也可以通过环境变量 `GITHUB_TOKEN` 配置

#### 2. 通过命令行参数配置
Gradle 参数 
```shell
gradle build -Pgpr.user=<your github account> -Pgpr.key=<PTA OR GITHUB_TOKEN>
```   

JAVA 参数
```shell
gradle build -Dgpr.user=<your github account> -Dgpr.key=<PTA OR GITHUB_TOKEN>
```

#### 4. 直接在 `build.gradle.kts` 中指定 (不推荐)

```kotlin
infra {
    useGithubAccount("<your github account>", "<PTA OR GITHUB_TOKEN>")
}
```


> **关于 Github Token**   
> 
> `GITHUB_TOKEN`是消费仓库中的包必须的 PTA， 关于如何生成 PTA 参考这里:   
> https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token

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



## 发布 Maven 包
在 gradle.build.kts 中添加以下内容:   

```kotlin
subprojects {
    infra(isBom = true) {
        publishing {
            pom {
                description = "maven bom project"
                githubUrl("hongque-pro", "infra-test")
                artifactId { "mylib" }
            }

            toGithubPackages("hongque-pro", "infra-bom")
            toNexus("http://xxxx", "userA", "password")
        }
    }
}
```
说明：   
`publishing` 将会自动配置好发布到 maven 官方仓库发布所需的配置，除了你的 maven 账号的签名用密钥 。    
`toGithubPackages` 将会支持将你的包发布到 github 仓库。   
`toNexus` 支持将你的包发布到你自己的私有仓库 (Nexus)。   
> 发布包到 maven 仓库需要签名，具体请参考 maven 官方文档。

发布包也需要授权，可以通过 gradle.properties 或 local.properties 配置：

#### 发布授权配置

发布到 Maven 官方仓库和 Nexus 都需要授权。和 github 账号配置一样，支持环境变量，命令行或 `gradle.properties` 文件中配置这些账号信息。   
下表列出了授权配置的参数:   

| 项目          |    环境变量    | gradle.properties |
|:------------|:----------:|:-----------------:|
| Mavan 账号    | MAVEN_USER |    maven.user     |
| Mavan 密码    | MAVEN_KEY  |     maven.key     |
| Nexus 服务器地址 | NEXUS_URL  |     nexus.url     |
| Nexus 账号    | NEXUS_USER |    nexus.user     |
| Nexus 密码    | NEXUS_KEY  |     nexus.key     |

> `gradle.properties` 配置都可以通过命令行 `-P[参数名]` 来配置。

#### 发布包签名配置

签名配置遵循 [Gradle Sign](https://docs.gradle.org/current/userguide/signing_plugin.html) 插件配置。

```properties
signing.keyId=
signing.password=
signing.secretKeyRingFile=
```
具体请参考 Sign 插件文档：   
https://docs.gradle.org/current/userguide/signing_plugin.html