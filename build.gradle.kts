import java.time.Clock

apply(plugin = "com.github.ben-manes.versions")

applyRelease()

val replaceVersion by tasks.registering(Copy::class) {
    // Always regenerate yaml
    outputs.upToDateWhen { false }
    from("kubernetes") {
        include("**/*.yaml")
        val snapshotDockerRepo: String? by project
        val snapshotDockerVersion: String? by project

        val replacements = mutableMapOf(
            "version" to (snapshotDockerVersion ?: project.version),
            "dockerRepository" to (snapshotDockerRepo?.trimEnd('/') ?: "sourceforgery"),
            "currentTime" to Clock.systemUTC().instant().toString()
        )
        System.err.println(replacements)
        expand(replacements)
    }
    into("$buildDir/kubernetes/")
    includeEmptyDirs = false
}

tasks["assemble"].dependsOn(replaceVersion)
val publishSnapshot by tasks.registering {
    dependsOn(replaceVersion)
}
rootProject.tasks["githubRelease"].dependsOn(replaceVersion)

extensions.getByType<co.riiid.gradle.GithubExtension>().apply {
    addAssets("$buildDir/kubernetes/deployment-webserver.yaml")
}

@Suppress("UnstableApiUsage")
allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
            dependencySubstitution {
                substitute(module("org.slf4j:jcl-over-slf4j")).with(module("org.apache.logging.log4j:log4j-jcl:$log4j2Version"))

                substitute(module("org.slf4j:jul-to-slf4j")).with(module("org.apache.logging.log4j:log4j-jul:$log4j2Version"))
                substitute(module("org.slf4j:slf4j-simple")).with(module("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version"))
                substitute(module("com.google.guava:guava-jdk5")).with(module("com.google.guava:guava:$guavaVersion"))

                all {
                    when (val requested = requested) {
                        is org.gradle.internal.component.external.model.DefaultModuleComponentSelector ->
                            when (requested.group) {
                                "io.grpc" -> if ("kotlin" !in requested.module) {
                                    useTarget("${requested.group}:${requested.module}:$grpcVersion")
                                }
                                "com.google.protobuf" -> useTarget("${requested.group}:${requested.module}:$protocVersion")
                                "org.apache.logging.log4j" -> if (requested.module != "log4j-api-kotlin") {
                                    useTarget("${requested.group}:${requested.module}:$log4j2Version")
                                }
                                "org.jetbrains.kotlin" -> useTarget("${requested.group}:${requested.module}:$kotlinVersion")
                                "com.fasterxml.jackson.core" -> useTarget("${requested.group}:${requested.module}:$jacksonVersion")
                            }
                    }
                }
            }
            force(
                "com.google.guava:guava:$guavaVersion",
                "commons-io:commons-io:2.6",
                "commons-logging:commons-logging:1.2",
                "javax.annotation:javax.annotation-api:1.3.2",
                "org.postgresql:postgresql:$postgresqlDriverVersion",
                "org.slf4j:slf4j-api:1.7.29"
            )
        }
    }

    apply(plugin = "idea")

    extensions.getByType<org.gradle.plugins.ide.idea.model.IdeaModel>().apply {
        module {
            outputDir = file("build/idea-out")
            testOutputDir = file("build/idea-testout")
        }
    }

    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        options.apply {
            isWarnings = true
        }
    }
}

group = "com.sourceforgery.tachikoma"
