plugins {
    id("java-library")
}

val lombokVersion = "1.18.32"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))

    implementation(project(":testloom-core"))

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("com.google.truth:truth:1.4.4")
}
