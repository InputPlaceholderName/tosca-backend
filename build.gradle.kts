import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent

val logback_version: String by project
val ktor_version: String by project
val kotlin_version: String by project
val exposed_version: String by project
val junit_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.10"
}

group = "me.klasa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.jetbrains.exposed", "exposed-core", exposed_version)
    implementation("org.jetbrains.exposed", "exposed-dao", exposed_version)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposed_version)
    implementation("org.postgresql:postgresql:42.2.20")
    implementation("org.flywaydb:flyway-core:7.9.1")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
    implementation("com.zaxxer:HikariCP:4.0.3")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation("org.testcontainers:postgresql:1.15.3")
    testImplementation("org.testcontainers:junit-jupiter:1.15.3")
}

tasks.withType<Test>() {
    this.testLogging {
        this.events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}


tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}