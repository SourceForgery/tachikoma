plugins {
    kotlin("js")
}

dependencies {
//     compile "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version"
//     testImplementation "org.jetbrains.kotlin:kotlin-test-js:$kotlin_version"
//     implementation(project(":tachikoma-frontend-api-proto:tachikoma-frontend-api-js"))
    testImplementation(kotlin("test-js"))
}

kotlin {
    js {
        browser {
        }
        binaries.executable()
    }
}

// kotlinFrontend {
//     downloadNodeJsVersion = 'latest'
//
//     npm {
//         dependency("kotlin", kotlin_version)
//         dependency("protobufjs", "6.8.3")
//         dependency("style-loader") // production dependency
//
//         devDependency("karma")     // development dependency
//     }
//
//     sourceMaps = true
//
//     webpackBundle {
//         bundleName = "main"
//         publicPath = "/static/"
//         sourceMapEnabled = true
//         contentPath = file('src/main/web')
//         port = 9090
//         proxyUrl = "http://localhost:9091"
//     }
// }


// compileKotlin2Js {
//     kotlinOptions.metaInfo = true
//     kotlinOptions.outputFile = "$project.buildDir.path/js/${project.name}.js"
//     kotlinOptions.sourceMap = true
//     kotlinOptions.moduleKind = 'commonjs'
//     kotlinOptions.main = "call"
// }

// compileTestKotlin2Js {
//     kotlinOptions.sourceMap = true
// }


val assembleWeb by tasks.registering(Copy::class) {
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absolutePath)) {
            includeEmptyDirs = false
            include { fileTreeElement ->
                val path = fileTreeElement.path
                path.endsWith(".js") && (path.startsWith("META-INF/resources/") ||
                    !path.startsWith("META-INF/"))
            }
        }
    }
    // from(compileKotlin2Js.destinationDir)
    into("${projectDir}/web")

    dependsOn("mainClasses")
}

tasks["assemble"].dependsOn(assembleWeb)
