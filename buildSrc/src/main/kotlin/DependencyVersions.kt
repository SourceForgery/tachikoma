import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

fun Project.forcedBoms(): List<String> {
    val armeriaVersion: String by project
    val ebeanVersion: String by project
    val grpcVersion: String by project
    val jacksonVersion: String by project
    val junitVersion: String by project
    val kotlinCoroutineVersion: String by project
    val kotlinVersion: String by project
    val log4j2Version: String by project
    val okHttpVersion: String by project
    val protocVersion: String by project

    return listOf(
        "com.fasterxml.jackson:jackson-bom:$jacksonVersion",
        "com.google.protobuf:protobuf-bom:$protocVersion",
        "com.linecorp.armeria:armeria-bom:$armeriaVersion",
        "com.squareup.okhttp3:okhttp-bom:$okHttpVersion",
        "io.ebean:ebean-bom:$ebeanVersion",
        "io.grpc:grpc-bom:$grpcVersion",
        "io.ktor:ktor-bom:2.1.3",
        "org.apache.logging.log4j:log4j-bom:$log4j2Version",
        "org.jetbrains.kotlin:kotlin-bom:$kotlinVersion",
        "org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinCoroutineVersion",
        "org.junit:junit-bom:$junitVersion",
    )
}

fun Project.forcedDependencies(): Set<String> {
    val amqpClientVersion: String by project
    val armeriaVersion: String by project
    val bytebuddyVersion: String by project
    val commonsIOVersion: String by project
    val commonsLangVersion: String by project
    val commonsLoggingVersion: String by project
    val errorProneAnnotationsVersion: String by project
    val expectItVersion: String by project
    val grpcKotlinVersion: String by project
    val grpcVersion: String by project
    val gsonVersion: String by project
    val guavaVersion: String by project
    val h2Version: String by project
    val jakartaAnnotationsVersion: String by project
    val jakartaMailVersion: String by project
    val javaxAnnotationApiVersion: String by project
    val jnrUnixsocketVersion: String by project
    val jsoupVersion: String by project
    val jsr305Version: String by project
    val kodeinVersion: String by project
    val kotlinVersion: String by project
    val kotlinxHtmlVersion: String by project
    val log4jApiKotlin: String by project
    val mockkVersion: String by project
    val pgEmbeddedVersion: String by project
    val postgresqlDriverVersion: String by project
    val slf4jVersion: String by project
    val tapeVersion: String by project
    val jmustacheVersion: String by project

    return setOf(
        "com.github.jnr:jnr-unixsocket:$jnrUnixsocketVersion",
        "com.google.code.findbugs:jsr305:$jsr305Version",
        "com.google.code.gson:gson:$gsonVersion",
        "com.google.errorprone:error_prone_annotations:$errorProneAnnotationsVersion",
        "com.google.guava:guava:$guavaVersion",
        "com.graphql-java:graphql-java:20.4",
        "com.h2database:h2:$h2Version",
        "com.linecorp.armeria:armeria-grpc:$armeriaVersion",
        "com.linecorp.armeria:armeria-grpc:$armeriaVersion",
        "com.opentable.components:otj-pg-embedded:$pgEmbeddedVersion",
        "com.rabbitmq:amqp-client:$amqpClientVersion",
        "com.samskivert:jmustache:$jmustacheVersion",
        "com.squareup:tape:$tapeVersion",
        "com.sun.mail:jakarta.mail:$jakartaMailVersion",
        "commons-io:commons-io:$commonsIOVersion",
        "commons-lang:commons-lang:$commonsLangVersion",
        "commons-logging:commons-logging:$commonsLoggingVersion",
        "io.grpc:grpc-kotlin-stub:$grpcKotlinVersion",
        "io.grpc:grpc-kotlin-stub:$grpcKotlinVersion",
        "io.grpc:grpc-stub:$grpcVersion",
        "io.mockk:mockk:$mockkVersion",
        "jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationsVersion",
        "javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion",
        "net.bytebuddy:byte-buddy:$bytebuddyVersion",
        "net.sf.expectit:expectit-core:$expectItVersion",
        "org.apache.httpcomponents:httpcore:4.4.15",
        "org.apache.logging.log4j:log4j-api-kotlin:$log4jApiKotlin",
        "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion",
        "org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion",
        "org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion",
        "org.jsoup:jsoup:$jsoupVersion",
        "org.kodein.di:kodein-di:$kodeinVersion",
        "org.postgresql:postgresql:$postgresqlDriverVersion",
        "org.reactivestreams:reactive-streams:1.0.4",
        "org.slf4j:slf4j-api:$slf4jVersion",
    )
}
