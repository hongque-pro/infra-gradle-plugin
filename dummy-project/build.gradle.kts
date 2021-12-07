
plugins {
    id("com.labijie.infra")
}

infra {
    useDefault {
        infraBomVersion = "2.6.0"
    }

    usePublish {
        description = "dxxxx"
        projectUrl = "https://localhost"
        gitUrl = "https://localhost"
        githubScmUrl = "https://localhost"
    }

    useNexusPublish()
}

dependencies {
    //...和原生 DSL 用法一致
}


