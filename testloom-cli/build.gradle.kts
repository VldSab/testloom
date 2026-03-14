plugins {
    id("application")
}

dependencies {
    implementation(project(":testloom-core"))

    implementation("info.picocli:picocli:4.7.5")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.google.truth:truth:1.4.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("dev.testloom.cli.TestloomMain")
    applicationName = "testloom"
}
