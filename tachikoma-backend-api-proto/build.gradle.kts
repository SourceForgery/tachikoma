plugins {
    id("java")
}

sourceSets {
    main {
        resources {
            srcDir("src/main/proto")
        }
    }
}
