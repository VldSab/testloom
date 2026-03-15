plugins {
    id("org.springframework.boot") version "3.3.0"
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))

    implementation(project(":testloom-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
