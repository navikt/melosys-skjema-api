import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
}

group = "no.nav.melosys"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Enable automatic toolchain provisioning
gradle.beforeProject {
    project.plugins.withType<JavaPlugin> {
        project.extensions.configure<JavaToolchainService> {
            // Empty configuration to enable auto-provisioning
        }
    }
}

val tokenSupportVersion = "5.0.40"
val mockOAuth2ServerVersion = "3.0.1"
val kotlinLoggingVersion = "7.0.13"
val kotestVersion = "6.0.7"
val mockkVersion = "1.14.7"
val wiremockVersion = "3.13.2"
val springMockkVersion = "5.0.1"
val springdocVersion = "2.8.14"
val shedlockVersion = "7.4.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("no.nav.security:token-validation-spring:${tokenSupportVersion}")
    implementation("no.nav.security:token-client-spring:${tokenSupportVersion}")
    implementation("no.nav.security:token-validation-core:${tokenSupportVersion}")
    implementation("no.nav.tms.varsel:kotlin-builder:2.1.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:${kotlinLoggingVersion}")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion")

    runtimeOnly("org.postgresql:postgresql")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation("no.nav.security:token-validation-spring-test:${tokenSupportVersion}")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("io.kotest:kotest-runner-junit5:${kotestVersion}")
    testImplementation("io.kotest:kotest-assertions-core:${kotestVersion}")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}