import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("groovy")
    id("java-library")
    id("maven-publish")
    id("signing")
    id("fr.brouillard.oss.gradle.jgitver").version("0.10.0-rc03")
    id("net.ltgt.errorprone").version("4.1.0")
}

group = "org.gradle.buildtool.internal"

jgitver {
    nonQualifierBranches("main")
    mavenLike(true)
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("com.google.guava:guava:32.1.2-jre") {
        isTransitive = false
    }

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    errorprone("com.google.errorprone:error_prone_core:2.36.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }

    // Consumers require Java 8 compatibility
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
    // withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.check("MissingSummary", CheckSeverity.OFF)
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Spock test framework
            useSpock("2.2-groovy-3.0")
        }
    }
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
}

@Suppress("DEPRECATION")
fun toMavenVersion(gitVersion: String): Pair<String, Boolean> {
    val matcher = Regex("(.*?)(-g[0-9a-f]+)?(-dirty)?").matchEntire(gitVersion) ?: error("Invalid version: $gitVersion")
    val version = VersionNumber.parse(matcher.groupValues[1])
    // TODO Prevent publishing dirty stuff?
    // If it's not a tagged version, or if there are local changes, this is a snapshot
    val snapshot = matcher.groupValues[2].isNotEmpty() || matcher.groupValues[3].isNotEmpty()
    val mavenVersion = if (snapshot) {
        VersionNumber(version.major, version.minor + 1, 0, "SNAPSHOT").toString()
    } else {
        version.toString()
    }
    return Pair(mavenVersion, snapshot)
}

val (mavenVersion, snapshot) = toMavenVersion(project.version.toString())

println("Building version $mavenVersion to ${if (snapshot) "snapshot" else "release"} repository (Git version: ${project.version})")

publishing {
    repositories {
        maven {
            val artifactoryUrl = providers.environmentVariable("GRADLE_INTERNAL_REPO_URL").orNull
            val artifactoryUsername = providers.environmentVariable("ORG_GRADLE_PROJECT_publishUserName").orNull
            val artifactoryPassword = providers.environmentVariable("ORG_GRADLE_PROJECT_publishApiKey").orNull

            name = "remote"
            val libsType = if (snapshot) "snapshots" else "releases"
            url = uri("${artifactoryUrl}/libs-${libsType}-local")
            credentials {
                username = artifactoryUsername
                password = artifactoryPassword
            }
        }
    }

    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = mavenVersion
            description = project.description

            pom {
                packaging = "jar"
                url = "https://github.com/gradle/java-abi-extractor"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "The Gradle team"
                        organization = "Gradle Inc."
                        organizationUrl = "https://gradle.org"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/gradle/java-abi-extractor.git"
                    developerConnection = "scm:git:ssh://github.com:gradle/java-abi-extractor.git"
                    url = "https://github.com/gradle/java-abi-extractor"
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("PGP_SIGNING_KEY"),
        System.getenv("PGP_SIGNING_KEY_PASSPHRASE")
    )
    if (!System.getenv("PGP_SIGNING_KEY_PASSPHRASE").isNullOrBlank()) {
        publishing.publications.configureEach {
            signing.sign(this)
        }
    }
}
