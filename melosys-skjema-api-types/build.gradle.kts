plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "no.nav.melosys"

val javaVersion = (project.findProperty("javaVersion") as String?)?.toInt() ?: 21

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
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.21")
    compileOnly("jakarta.validation:jakarta.validation-api:3.1.0")
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.28")
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
