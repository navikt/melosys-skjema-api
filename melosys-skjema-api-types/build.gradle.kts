plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "no.nav.melosys"

val javaVersion = (project.findProperty("javaVersion") as String?)?.toInt() ?: 21
val jacksonVersion = "2.21"
val jakartaValidationVersion = "3.1.1"
val swaggerVersion = "2.2.42"
val junitVersion = "6.0.2"
val kotestVersion = "6.1.2"

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
    withSourcesJar()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    compileOnly("jakarta.validation:jakarta.validation-api:$jakartaValidationVersion")
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:$swaggerVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("Melosys Skjema API Types")
                description.set("Shared types for Melosys Skjema API")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        organization.set("Nav (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration")
                        organizationUrl.set("https://www.nav.no")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/melosys-skjema-api")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
