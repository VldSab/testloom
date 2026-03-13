plugins {
    id("java-library")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))

    implementation(project(":testloom-core"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
