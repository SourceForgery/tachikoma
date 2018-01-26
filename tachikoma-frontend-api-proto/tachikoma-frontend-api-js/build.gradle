apply plugin: 'com.google.protobuf'
apply plugin: 'java'

dependencies {
    protobuf project(':tachikoma-frontend-api-proto')
    protobuf "com.google.protobuf:protobuf-java:$protoc_version"
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:$protoc_version"
    }
    generateProtoTasks {
        ofSourceSet('main').each { task ->
            task.builtins {
                remove java
                js {}
            }
        }
    }
}

jar {
    exclude('**/*.class')
    exclude('**/*.proto')
    from('build/generated/source/proto/main/js/') {
        into 'protobuf'
        include '**/*.js'
    }
}
