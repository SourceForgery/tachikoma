import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    idea
    `kotlin-dsl`
    `embedded-kotlin`
    id("com.github.ben-manes.versions") version "0.42.0"
}

val kotlinVersion: String by project
dependencies {
    api("com.github.breadmoirai:github-release:2.4.1")

    val guavaVersion: String by project
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
    implementation("io.ebean:ebean-gradle-plugin:13.11.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.6.0.201912101111-r")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.42.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    val expediagroupGraphqlVersion by properties
    implementation("com.expediagroup:graphql-kotlin-gradle-plugin:$expediagroupGraphqlVersion") {
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
        exclude("org.jetbrains.kotlin", "kotlin-gradle-plugin-api")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm")
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
}

val jacksonVersion: String by project
val guavaVersion: String by project
val slf4jVersion: String by project
val commonsLoggingVersion: String by project
val commonsLangVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
        force(
            "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion",
            "com.fasterxml.jackson.core:jackson-core:$jacksonVersion",
            "com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion",
            "com.google.guava:guava:$guavaVersion",
            "org.slf4j:slf4j-api:$slf4jVersion",
            "commons-logging:commons-logging:$commonsLoggingVersion",
            "commons-lang:commons-lang:$commonsLangVersion",
            "com.sun.jersey:jersey-client:1.18",
            "org.jetbrains.kotlin:kotlin-stdlib:$embeddedKotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib-common:$embeddedKotlinVersion",
        )
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}


group = "com.tachikoma"

configure<IdeaModel> {
    module {
        outputDir = file("build/idea-out")
        testOutputDir = file("build/idea-testout")
    }
}
