buildscript {
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-frontend-plugin:0.0.23"
    }
}

apply plugin: 'kotlin-platform-js'
apply plugin: 'kotlin2js'
apply plugin: 'org.jetbrains.kotlin.frontend'
apply plugin: 'kotlin-dce-js'

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version"
    testImplementation "org.jetbrains.kotlin:kotlin-test-js:$kotlin_version"
    runtime project(':tachikoma-frontend-api-proto:tachikoma-frontend-api-js')
}

kotlinFrontend {
    downloadNodeJsVersion = 'latest'

    npm {
        dependency("kotlin", kotlin_version)
        dependency("protobufjs", "6.8.3")
        dependency "style-loader" // production dependency

        devDependency "karma"     // development dependency
    }

    sourceMaps = true

    webpackBundle {
        bundleName = "main"
        publicPath = "/static/"
        sourceMapEnabled = true
        contentPath = file('src/main/web')
        port = 9090
        proxyUrl = "http://localhost:9091"
    }
}


compileKotlin2Js {
    kotlinOptions.metaInfo = true
    kotlinOptions.outputFile = "$project.buildDir.path/js/${project.name}.js"
    kotlinOptions.sourceMap = true
    kotlinOptions.moduleKind = 'commonjs'
    kotlinOptions.main = "call"
}

compileTestKotlin2Js {
    kotlinOptions.sourceMap = true
}


task assembleWeb(type: Sync) {
    configurations.compileClasspath.each { File file ->
        from(zipTree(file.absolutePath), {
            includeEmptyDirs = false
            include { fileTreeElement ->
                def path = fileTreeElement.path
                path.endsWith(".js") && (path.startsWith("META-INF/resources/") ||
                        !path.startsWith("META-INF/"))
            }
        })
    }
    from compileKotlin2Js.destinationDir
    into "${projectDir}/web"

    dependsOn classes
}

assemble.dependsOn assembleWeb