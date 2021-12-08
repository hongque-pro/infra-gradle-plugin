package com.labijie.infra.gradle.internal

import org.gradle.api.Project

class PomInfo {
    var description: String = ""
    var projectUrl: String = ""
    var gitUrl: String = ""
    var githubScmUrl: String = ""

    var licenseName: String = "The Apache License, Version 2.0"
    private set
    var licenseUrl: String = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    private set
    var developerName: String = "labijie team"
    var developerMail: String = "tech@labijie.com"

    var projectName: String? = null

    internal var idGeneration: ((p: Project) -> String)? = null

    fun artifactId(idGen: (p: Project) -> String){
        this.idGeneration = idGen
    }

    fun artifactId(id: String){
        this.idGeneration = { id }
    }


    fun license(name: String, url: String){
        this.licenseName = name
        this.licenseUrl = url
    }

    /**
     * set github project follow properties:
     * @see projectUrl
     * @see gitUrl
     * @see githubScmUrl
     */
    fun githubUrl(organizationOrUser: String, repository: String){
        this.projectUrl = "https://github.com/${organizationOrUser}/${repository}"
        this.githubScmUrl = "scm:git@github.com:${organizationOrUser}/${repository}.git"
        this.gitUrl = "https://github.com/${organizationOrUser}/${repository}.git"
    }

    /**
     * set developer info follow properties:
     * @see developerName
     * @see developerMail
     */
    fun developer(name: String, email: String) {
        this.developerName = name
        this.developerMail = email
    }
}