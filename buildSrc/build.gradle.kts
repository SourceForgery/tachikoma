
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.net.URI

val kotlinVersion = ""
dependencies {
    implementation("co.riiid:gradle-github-plugin:0.4.2")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.8")
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4") {
        exclude(group = "nekohtml", module = "xercesMinimal")
    }
    implementation("org.jlleitschuh.gradle:ktlint-gradle:9.1.1")
    implementation("io.ebean:ebean-gradle-plugin:12.1.1")
    implementation("net.researchgate:gradle-release:2.8.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.3.0.201903130848-r")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    implementation("se.transmode.gradle:gradle-docker:1.2-youcruit-9")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.10.0")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.17.0")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven { url = URI("https://plugins.gradle.org/m2/") }
    maven { url = URI("https://dl.bintray.com/youcruit/YouCruit") }
}

plugins {
    idea
    `kotlin-dsl`
    `embedded-kotlin`
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