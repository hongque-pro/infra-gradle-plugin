# Infra-Gradle-Plugin

![gradle-plugin-portal](https://img.shields.io/gradle-plugin-portal/v/com.labijie.infra?logo=gradle)
![workflow status](https://img.shields.io/github/actions/workflow/status/hongque-pro/infra-gradle-plugin/build.yml)
![license](https://img.shields.io/github/license/hongque-pro/infra-orm?style=flat-square)

该插件旨在简化 Gradle 项目配置，几行代码就可以迅速开始一个项目。   
<mark>注意</mark>: 仅支持 Kotlin DSL 的 gradle 项目。

## 内置引入的插件

infra gradle 会自动引入其他的插件，并且版本会被绑定且版本不可修改（版本不可修改受限于 gradle 的插件机制），如果内置插件的版本不适合您的项目，你可能无法使用 infra gradle plugin。
   
`Infra gradle **2.1.0**` 内置插件，这些插件将自动带入，无需手动引入：

> 如果你想使用 infra-gradle 又不想带入某个插件，你需要手动编译 infra-gradle 插件的源码，在 `build.gradle.kts` 中将 `api` 依赖改为 `compileOnly`，然后项目中手动引入这些插件。
> 
> `2.1.0`开始，infra-gradle 移除了 mybatis 插件引入，你需要手动引入它，可以通过 id("com.thinkimi.gradle.MybatisGenerator") 来引入，推荐你尝试 [Infra-Orm](https://github.com/hongque-pro/infra-orm) 代替 mybatis 获得更好的开发体验。

| 插件 id                                     | DSL 手动引入方式                     | 版本       |
|-------------------------------------------|--------------------------------|----------|
| org.jetbrains.kotlin.jvm                  | kotlin("jvm")                  | 2.2.0    |
| org.jetbrains.kotlin.plugin.allopen       | id                             | 2.2.0   |
| org.jetbrains.kotlin.plugin.serialization | kotlin("plugin.serialization") | 2.2.0   |
| com.google.devtools.ksp                   | id                             | 2.2.0-+ |
| io.github.gradle-nexus.publish-plugin     | id                             | 2.0.0    |
| com.gorylenko.gradle-git-properties       | id                             | 2.5.0    |
| com.github.ben-manes.versions             | id                             | 0.52.0   |

`2.1.0` 开始，不再支持可变 kotlin 依赖版本，因为我们发现当 kotlin 依赖包版本和 kotlin gradle 插件版本不一致时可能出现潜在的 BUG。

## 快速开始
通过简单几行 DSL 代码，快速生成一个可编译、可发布到 maven 的 project: 
```kotlin
plugins {
    id("com.labijie.infra") version "your version"
}

infra {
    useDefault {
        infraBomVersion = "3.2.0" //不使用 infra-bom， 请设置为空串("")
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
}

dependencies {
    //...和原生 DSL 用法一致
}

```

## 快速构建任务 

(Since 2.0.4)   

随着项目的规模扩大， gradle 完整构建速度缓慢，其中一些任务（例如 `test`, `ksp`, `kapt` 等）可能不是每次都需要执行, 
你可以使用命令行 -x <task name> 跳过这些任务，但是每次输入较长的命令非常不方便，本插件自动创建一个 `fastBuild` 任务，
用来实现仅 "Compile"， 该任务会将 test, symbol process 等相关任务完全排除，得到一个瘦身后的 build 任务。

```shell
gralde fastBuild
```

实测下来，这可以节省 60% 的构建时间（测试项目包含 ksp/kapt），同时避免 ksp/kapt 等任务造成的内存溢出，在很多场景下非常适用。

你还可以配置额外要跳过的任务，例如：

```kotlin
infra {
    skipTaskForFastBuild("myBatisGenerate", "gitPropertiesGenerate")
}
```

## local.properties 支持

支持从 local.properties 文件中使用配置，从 git 排除 local.properties 以实现本地化配置：
>适用于本地保存密钥，无需担心被提交到 git 中.

```groovy
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

```groovy
infra {
    useGithubAccount("<your github account>", "<PTA OR GITHUB_TOKEN>")
}
```

### 发布到 Github Packages 

`gradle.build` 文件中加入:

```kotlin
 infra {
    publishing {
        toGithubPackages(<git-repo-owner>, <repo-name>)
    }
}
```

> 其中 `toGithubPackages` 参数：    
> `git-repo-owner` : github 用户/组织名称   
> `git-repo-owner` : github 仓库名称 
>
> 例如 [application-framework](https://github.com/hongque-pro/application-framework) 可以写作：   
> toGithubPackages("hongque-pro", "application-framework")


> **关于 Github Token**   
> 
> `GITHUB_TOKEN`是消费仓库中的包必须的 PTA， 关于如何生成 PTA 参考这里:   
> https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token

### 消费 GitHub Packages 中的包

在 gradle.build.kts 中添加以下内容:
```groovy
allprojects {
    infra {
        useDefault {
            addGitHubRepository("<owner>", "<repository>") //特定的 Github Repository 仓库
        }
    }
}
```



## 发布 Maven 包
默认插件已经自动配置了将包发布到 **maven** 中央仓库，你可以将包发布到 **github pages** 或私有 **nexus** 仓库
在 `gradle.build.kts` 中添加以下内容:   

```groovy
subprojects {
    infra(isBom = true) {
        publishing {
            pom {
                description = "maven bom project"
                githubUrl("hongque-pro", "infra-test")
                artifactId { "mylib" }
            }

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


## Maven 使用仓库镜像

默认情况下，插件会自动使用  maven 仓库加速：
- 使用阿里云镜像加速: https://maven.aliyun.com/nexus/content/groups/public/
- 你可以通过环境变量 `MAVEN_PROXY` 或在 gradle.properties / local.properties 中添加 `maven.proxy` 配置，指定 nexus 仓库地址，来使用你的私有 maven 仓库。

> 插件在检测一些环境变量自动关闭 maven 仓库加速：
> 
> - GITHUB_JOB
> - NO_MAVEN_PROXY
> 
> 注意，这些环境变量只要存在就会关闭镜像加速，无论它们是什么值。

你可以强制禁止 maven 仓库加速行为：
```groovy
infra {
    useDefault {
        useMavenProxy = false
    }
}
```

## 配置 GitProperties 插件

默认集成了 git properties 插件
> 插件地址:    
> https://github.com/n0mer/gradle-git-properties
> 
> 需要注意的是，为了避免重名 git.properties 资源，插件默认使用 git-info/git.properties 打包文件。
> 
> 配合 [application-framework](https://github.com/hongque-pro/application-framework) 项目 `getGitProperties` 可以方便的读取 git 信息

可以通过如下方式配置插件

```kotlin
infra {
    gitProperties {
        failOnNoGitDirectory = false
    }
}
```