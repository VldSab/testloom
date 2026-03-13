plugins {
    id("java-library")
}

dependencies {
    implementation(project(":testloom-core"))

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.3.0")
    implementation("org.springframework:spring-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
