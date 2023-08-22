import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask
import graphql.addGraphqlSchemaGenerator

plugins {
    `tachikoma-kotlin`
    id("com.expediagroup.graphql")
}

System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")

addGraphqlSchemaGenerator(":tachikoma-frontend-api-graphql:graphqlSchemaGenerator")

dependencies {
    val expediagroupGraphqlVersion by properties
    api("com.expediagroup:graphql-kotlin-client-jackson:$expediagroupGraphqlVersion")
    api("com.expediagroup:graphql-kotlin-client:$expediagroupGraphqlVersion")

    testImplementation("com.expediagroup:graphql-kotlin-ktor-client:$expediagroupGraphqlVersion") {
        exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
    }
    testImplementation("io.ktor:ktor-client-okhttp")
    testImplementation("io.ktor:ktor-client-logging-jvm")
    testImplementation("commons-lang:commons-lang:2.6")

    testImplementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))
    testImplementation(project(":tachikoma-database"))
    testImplementation(project(":tachikoma-grpc"))
    testImplementation(project(":tachikoma-rest"))
    testImplementation(project(":tachikoma-webserver"))
    testImplementation(project(":tachikoma-startup"))
    testImplementation(project(":tachikoma-frontend-api-graphql"))

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("com.google.guava:guava")
    testImplementation("com.google.protobuf:protobuf-java-util")
    testImplementation("com.google.protobuf:protobuf-java")
    testImplementation("com.h2database:h2")
    testImplementation("com.opentable.components:otj-pg-embedded")
    testImplementation("io.ebean:ebean")
    testImplementation("io.ebean:ebean-ddl-generator")
    testImplementation("io.grpc:grpc-stub")
    testImplementation("io.mockk:mockk")
    testImplementation("org.apache.logging.log4j:log4j-core")
    testImplementation("org.apache.logging.log4j:log4j-jul")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl")
    testImplementation("com.linecorp.armeria:armeria")
    testImplementation("com.linecorp.armeria:armeria-kotlin")
    testImplementation("com.squareup.okhttp3:okhttp")
    testImplementation("org.jsoup:jsoup")
    testImplementation("com.sun.mail:jakarta.mail")
}

graphql {
    client {
        endpoint = "http://localhost:8888/graphql"
        packageName = "com.youcruit.graphql.client.test.generated"
        queryFiles = File(projectDir, "src/main/resources")
            .walk()
            .filter { !it.isDirectory }
            .sorted()
            // .gql is deprecated for no other reason than it not being picked up
            // by the intellij plugin by default
            .filter { it.extension == "graphql" || it.extension == "gql" }
            .toList()
        serializer = GraphQLSerializer.JACKSON
    }
}

with(tasks) {
    withType(GraphQLIntrospectSchemaTask::class) {
        onlyIf { false }
    }

    graphqlGenerateClient {
        doFirst {
            delete("$buildDir/generated/source/graphql")
        }
    }
    getByName("classes").dependsOn(graphqlGenerateClient)
}

graphql {
    client {
        useOptionalInputWrapper = true
    }
}
