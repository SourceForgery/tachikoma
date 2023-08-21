import graphql.GraphqlSchemaGeneratorTask

plugins {
    `tachikoma-kotlin`
}

val generatorConfiguration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    val graphqlKotlinVersion: String by properties
    implementation(project(":tachikoma-internal-api"))

    implementation("com.expediagroup:graphql-kotlin-schema-generator:$graphqlKotlinVersion")
    implementation("com.expediagroup:graphql-kotlin-server:$graphqlKotlinVersion")
    api("org.kodein.di:kodein-di")

    generatorConfiguration("org.slf4j:slf4j-jdk14:2.0.6")
}

val graphqlSchemaGenerator by tasks.registering(GraphqlSchemaGeneratorTask::class) {
    dependsOn("classes")
    mainClass.set("com.sourceforgery.tachikoma.graphql.GraphqlSchemaGeneratorKt")
    classpath(generatorConfiguration)
}

tasks.assemble {
    dependsOn(graphqlSchemaGenerator)
}
