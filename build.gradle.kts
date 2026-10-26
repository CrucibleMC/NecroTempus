
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

dependencies {
    add("java17Dependencies", "com.github.GTNewHorizons:lwjgl3ify:3.0.33")
    add("java17PatchDependencies", "com.github.GTNewHorizons:lwjgl3ify:3.0.33:forgePatches") {
        isTransitive = false
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude("io/github/cruciblemc/necrotempus/dev/WikiScreenshotHarness.class")
}
