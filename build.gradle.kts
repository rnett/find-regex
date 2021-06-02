import com.rnett.action.githubAction
import com.rnett.action.useAutoBuildWorkflow

plugins {
    kotlin("js") version "1.4.32"
    id("com.github.rnett.ktjs-github-action") version "1.1.1"
}

group = "com.github.rnett.github-actions"
version = "0.1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    testImplementation(kotlin("test-js"))


    implementation("com.github.rnett.ktjs-github-action:kotlin-js-action:1.1.1")
}

useAutoBuildWorkflow()

kotlin {
    js(IR) {
        githubAction()
    }
}