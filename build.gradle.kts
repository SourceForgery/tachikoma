apply(plugin = "com.github.ben-manes.versions")

applyRelease()

val replaceVersion by tasks.registering(Copy::class) {
    from("kubernetes") {
        include("**/*.yaml")
        expand(mutableMapOf("version" to project.version))
    }
    into("$buildDir/kubernetes/")
    includeEmptyDirs = false
}

tasks.getByPath(":githubRelease").apply {
    dependsOn(replaceVersion)
    doFirst {
        project.extensions.getByType<co.riiid.gradle.GithubExtension>().apply {
            for (asset in assets) {
                logger.info("\"$asset\": ${File(asset).length()}")
            }
        }
    }
}

extensions.getByType<co.riiid.gradle.GithubExtension>().apply {
    addAssets(listOf("${project.buildDir}/kubernetes/deployment-webserver.yaml"))
}

@Suppress("UnstableApiUsage")
allprojects {
    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
            dependencySubstitution {
                substitute(module("javax.annotation:javax.annotation-api")).with(module("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationsVersion"))
                substitute(module("org.slf4j:jcl-over-slf4j")).with(module("org.apache.logging.log4j:log4j-jcl:$log4j2Version"))
                substitute(module("org.slf4j:jul-to-slf4j")).with(module("org.apache.logging.log4j:log4j-jul:$log4j2Version"))
                substitute(module("org.slf4j:slf4j-simple")).with(module("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version"))
                substitute(module("javax.inject:javax.inject:1")).with(module("org.glassfish.hk2.external:javax.inject:$hk2Version"))
                substitute(module("com.google.guava:guava-jdk5")).with(module("com.google.guava:guava:$guavaVersion"))
                substitute(module("com.google.code.findbugs:annotations")).with(module("com.google.code.findbugs:jsr305:$jsr305Version"))
                substitute(module("net.jcip:jcip-annotations")).with(module("com.google.code.findbugs:jsr305:$jsr305Version"))
                substitute(module("javax.annotation:javax.annotation-api")).with(module("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationsVersion"))
                substitute(module("javax.activation:activation")).with(module("com.sun.activation:javax.activation:1.2.0"))

                all {
                    when (val requested = requested) {
                        is org.gradle.internal.component.external.model.DefaultModuleComponentSelector ->
                            when (requested.group) {
                                "io.grpc" -> useTarget("${requested.group}:${requested.module}:$grpcVersion")
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
                "com.google.code.gson:gson:$gsonVersion",
                "com.google.guava:guava:$guavaVersion",
                "com.google.errorprone:error_prone_annotations:2.3.3",
                "org.codehaus.mojo:animal-sniffer-annotations:1.18",
                "org.postgresql:postgresql:$postgresqlDriverVersion",
                "org.slf4j:slf4j-api:1.7.29"
            )
        }
    }
}

tasks["assemble"].dependsOn(replaceVersion)

subprojects {
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
