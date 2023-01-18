import maven.unrollBom
import maven.unrollBomForce
import org.gradle.kotlin.dsl.provideDelegate

plugins {
    `java-library`
}

fun DependencyConstraintHandlerScope.unrollBom(vararg dependencies: String) {
    for (dependency in dependencies) {
        val (groupId, artifactId, version) = dependency.split(":")
        unrollBom(project, groupId, artifactId, version)
    }
}

fun ResolutionStrategy.unrollBomForce(vararg dependencies: String) {
    for (dependency in dependencies) {
        val (groupId, artifactId, version) = dependency.split(":")
        unrollBomForce(project, groupId, artifactId, version)
    }
}

dependencies {
    constraints {
        unrollBom(*forcedBoms().toTypedArray())
        for (forcedDependency in forcedDependencies()) {
            api(forcedDependency)
        }
    }
}

configurations.all {
    val guavaVersion: String by project
    val log4j2Version: String by project
    val jakartaAnnotationsVersion: String by project

    resolutionStrategy {
        failOnVersionConflict()
        unrollBomForce(
            *forcedBoms().toTypedArray()
        )

        force(forcedDependencies().toTypedArray())

        @Suppress("UnstableApiUsage")
        dependencySubstitution {
            substitute(module("org.slf4j:jcl-over-slf4j")).using(module("org.apache.logging.log4j:log4j-jcl:$log4j2Version"))

            substitute(module("org.slf4j:jul-to-slf4j")).using(module("org.apache.logging.log4j:log4j-jul:$log4j2Version"))
            substitute(module("org.slf4j:slf4j-simple")).using(module("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version"))
            substitute(module("com.google.guava:guava-jdk5")).using(module("com.google.guava:guava:$guavaVersion"))

            substitute(module("javax.annotation:javax.annotation-api")).using(module("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationsVersion"))
        }
    }
}
