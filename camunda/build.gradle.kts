import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.skynet4ever"
extra.apply { set("groovy.version", "3.0.3") }

buildscript {
    dependencies {
        classpath("org.owasp:dependency-check-gradle:6.5.3")
    }
}

apply(plugin = "org.owasp.dependencycheck")

plugins {
    val kotlinVersion = "1.4.20"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("jacoco")
    id("org.sonarqube") version "2.7.1"
}

repositories {
    maven("https://app.camunda.com/nexus/content/groups/public")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
    // actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.6.4")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.3")

    compileOnly("org.camunda.bpm:camunda-engine:7.17.0")
    compileOnly("org.springframework.boot:spring-boot:2.6.4")

    // Sentry
    implementation("io.sentry:sentry-spring-boot-starter:5.7.3")

    testImplementation("org.camunda.bpm:camunda-engine:7.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("io.mockk:mockk:1.10.5")

    // Property based testing
    testImplementation("net.jqwik:jqwik:1.5.6")

    // Test URL Encoding
    testImplementation("org.apache.httpcomponents:httpclient:4.5.13")

    testImplementation("org.camunda.bpm.assert:camunda-bpm-assert:8.0.0")
    testImplementation("org.camunda.bpm.extension:camunda-bpm-junit5:1.0.0")
    testImplementation("org.assertj:assertj-core:3.16.1")

    // Custom rest client
    testImplementation("org.camunda.bpm:camunda-engine-rest-core:7.14.0")
    testImplementation("jakarta.activation:jakarta.activation-api:1.2.1")

    testImplementation("com.h2database:h2:2.1.212")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml").get().asFile)
    }
}

tasks.sonarqube {
    dependsOn(tasks.jacocoTestReport)
}

tasks.getByName<Jar>("jar") {
    archiveFileName.set("httpDelegate.jar")
}

tasks.register<Copy>("copyDependencies") {
    group = "Dependencies"
    from(configurations.default)
    into("dependencies")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
