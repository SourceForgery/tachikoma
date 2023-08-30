rootProject.name = "com.sourceforgery.tachikoma"

include(
    "jersey-uri-builder",
    "tachikoma-protobuf-annotations",

    "tachikoma-backend-api-proto",
    "tachikoma-backend-api-proto:tachikoma-backend-api-jvm",

    "tachikoma-frontend-api-graphql",
    "tachikoma-frontend-api-proto",
    "tachikoma-frontend-api-proto:tachikoma-frontend-api-jvm",
// "tachikoma-frontend-api-proto:tachikoma-frontend-api-js",

    "tachikoma-internal-proto-jvm",
    "tachikoma-internal-api",

    "tachikoma-common",

    "tachikoma-database-api",
    "tachikoma-database",
    "tachikoma-mq",
    "tachikoma-grpc",
    "tachikoma-rest",
// "tachikoma-frontend",
    "tachikoma-startup",
    "tachikoma-webserver",
    "tachikoma-webserver-docker",
    "tachikoma-postfix-utils",
    "tachikoma-postfix-docker",
    "tachikoma-test-client",
    "tachikoma-it"
)
