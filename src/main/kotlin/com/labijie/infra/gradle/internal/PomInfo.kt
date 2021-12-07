package com.labijie.infra.gradle.internal

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

    fun license(name: String, url: String){
        this.licenseName = name
        this.licenseUrl = url
    }
}