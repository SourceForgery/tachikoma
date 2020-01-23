apply(plugin = "java")

sourceSets {
    "main" {
        resources {
            srcDir("src/main/proto")
        }
    }
}