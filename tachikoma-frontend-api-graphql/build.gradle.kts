import graphql.GraphqlSchemaGeneratorTask

plugins {
    `tachikoma-kotlin`
}

val generatorConfiguration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val graphqls by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

dependencies {
    val graphqlKotlinVersion: String by properties
    implementation(project(":tachikoma-internal-api"))

    implementation("com.expediagroup:graphql-kotlin-schema-generator:$graphqlKotlinVersion")
    implementation("com.expediagroup:graphql-kotlin-server:$graphqlKotlinVersion")
    api("org.kodein.di:kodein-di")

    generatorConfiguration("org.slf4j:slf4j-jdk14:2.0.6")
}

val graphqlsFile = File(buildDir, "tachikoma-frontend-api-graphql-${project.version}.graphqls")

val graphqlSchemaGenerator by tasks.registering(GraphqlSchemaGeneratorTask::class) {
    dependsOn("classes")
    mainClass.set("com.sourceforgery.tachikoma.graphql.GraphqlSchemaGeneratorKt")
    classpath(generatorConfiguration)
    schemaOutput.set(graphqlsFile)
}

tasks.assemble {
    dependsOn(graphqlSchemaGenerator)
}

rootProject.extensions.configure<com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension> {
    releaseAssets.from(graphqlsFile)
}

artifacts {
    add(graphqls.name, graphqlsFile) {
        builtBy(graphqlSchemaGenerator)
    }
}
