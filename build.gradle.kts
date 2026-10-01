
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
