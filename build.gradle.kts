/// build.gradle.kts (ant-wrapper):
/// ==============================
///
/// Access gradle.properties:
///     yes -> "val prop_name = project.extra['prop.name']"
///     no  -> "val prop_name = property('prop.name')"

import org.gradle.api.file.DuplicatesStrategy


/** 1) Plugins used globally */
plugins {
    jacoco

    id("org.jetbrains.kotlin.jvm") version "1.4.20"
}


/** 2) General information regarding the software */
group   = project.extra["software.group"]!! as String
version = project.extra["software.version"]!! as String


/** 3) Dependency source configuration */
repositories {
    mavenCentral()
}


/** 4) Plugin dependencies */
dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-bom")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.ant:ant:1.10.12")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
    testImplementation(gradleTestKit())
}


/** 5) Configure JAR */
tasks.jar {
    manifest {
        attributes["Main-Class"] = project.extra["software.class"]!! as String
    }

    from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}


/** 6) JaCoCo configuration */
jacoco {
    toolVersion = "0.8.7"
}

tasks.jacocoTestReport {
    reports {
        csv.isEnabled = true
    }
}
