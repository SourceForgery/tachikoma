import org.gradle.plugins.ide.idea.model.IdeaModel
import java.net.URI

val kotlinVersion = embeddedKotlinVersion
dependencies {
    implementation("co.riiid:gradle-github-plugin:0.4.2")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4") {
        exclude(group = "nekohtml", module = "xercesMinimal")
    }
    implementation("org.jlleitschuh.gradle:ktlint-gradle:9.4.1")
    implementation("io.ebean:ebean-gradle-plugin:12.1.12")
    implementation("net.researchgate:gradle-release:2.8.1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.6.0.201912101111-r")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    implementation("se.transmode.gradle:gradle-docker:1.2-youcruit-9")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.10.1")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.27.0")
}

configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
        force(
            "org.apache.httpcomponents:httpclient:4.5.11",
            "com.google.guava:guava:28.2-jre",
            "org.slf4j:slf4j-api:1.7.30",
            "commons-logging:commons-logging:1.2",
            "commons-lang:commons-lang:2.6",
            "com.sun.jersey:jersey-client:1.18",
            "org.apache.maven:maven-artifact:3.6.3",
            "org.apache.maven:maven-model:3.6.3",
            "org.codehaus.plexus:plexus-utils:3.3.0",
            "com.google.gradle:osdetector-gradle-plugin:1.6.2",
            "org.codehaus.groovy.modules.http-builder:http-builder:0.7.2",
            "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2"
        )
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven { url = URI("https://plugins.gradle.org/m2/") }
    maven { url = URI("https://dl.bintray.com/youcruit/YouCruit") }
}

@Suppress("UnstableApiUsage")
plugins {
    idea
    `kotlin-dsl`
    `embedded-kotlin`
    id("com.github.ben-manes.versions") version "0.27.0"
}

kotlinDslPluginOptions {
    @Suppress("UnstableApiUsage")
    experimentalWarning.set(false)
}

group = "com.tachikoma"

configure<IdeaModel> {
    module {
        outputDir = file("build/idea-out")
        testOutputDir = file("build/idea-testout")
    }
}

dependencies {
    implementation("com.google.guava:guava:28.2-jre")
}
