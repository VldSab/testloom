import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.math.BigDecimal

plugins {
    id("java")
}

allprojects {
    group = "dev.testloom"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    val coverageModules = setOf(
        "testloom-core",
        "testloom-spring-boot-starter",
        "testloom-cli"
    )

    if (name in coverageModules) {
        apply(plugin = "jacoco")

        tasks.withType<Test> {
            finalizedBy(tasks.named("jacocoTestReport"))
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            if (project.name == "testloom-cli") {
                classDirectories.setFrom(
                    classDirectories.files.map { fileTree(it) { exclude("**/TestloomMain.class") } }
                )
            }
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("test"))
            if (project.name == "testloom-cli") {
                classDirectories.setFrom(
                    classDirectories.files.map { fileTree(it) { exclude("**/TestloomMain.class") } }
                )
            }
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum = BigDecimal("0.80")
                    }
                }
            }
        }

        tasks.named("check") {
            dependsOn(tasks.named("jacocoTestCoverageVerification"))
        }
    }
}
