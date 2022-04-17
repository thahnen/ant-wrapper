/// build.gradle.kts (ant-wrapper):
/// ==============================
///
/// Access gradle.properties:
///     yes -> "val prop_name = project.extra['prop.name']"
///     no  -> "val prop_name = property('prop.name')"

import java.lang.Thread.sleep
import org.gradle.api.file.DuplicatesStrategy


/** 1) Plugins used globally */
plugins {
    jacoco

    id("org.jetbrains.kotlin.jvm") version "1.5.31"
    id("io.gitlab.arturbosch.detekt") version "1.19.0-RC2"
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

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
    testImplementation(gradleTestKit())
}


/** 5) Configure JAR */
tasks.jar {
    archiveFileName.set("${project.name}.jar")

    manifest {
        attributes["Main-Class"] = project.extra["software.class"]!! as String
    }

    from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}


/** 6) JaCoCo configuration */
jacoco {
    toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
    reports {
        csv.isEnabled = true
    }
}


/** 7) detekt configuration */
detekt {
    ignoreFailures = true
    basePath = projectDir.toString()
}


/** 8) Gradle test configuration */
tasks.withType<Test> {
    testLogging.showStandardStreams = true
}


/** 8) Create usable Ant installations (with Ant installed) */
tasks.register("installWithAntInstalled") {
    dependsOn(tasks.jar)

    doLast {
        // i) check if property provided as command line argument exists or not
        when {
            !project.hasProperty("out") -> throw InvalidUserDataException(
                "[${project.name} -> installWithAntInstalled] No output Ant project directory provided as command " +
                "line argument! Run task as follows: 'gradlew installWithAntInstalled -Pout=/home/user/antProject'!"
            )
        }
        val out = file(project.properties["out"] as String)

        // ii) copy Jar / Ant script to output project directory
        listOf(tasks.jar, "$projectDir/src/test/resources/install-Ant-Wrapper.xml").forEach {
            copy {
                from(it)
                into(out)
            }
        }
        sleep(5000)

        // iii) run Ant script in specific directory
        exec {
            workingDir = file(properties["out"] as String)
            commandLine("ant", "-buildfile", "install-Ant-Wrapper.xml")
        }
        sleep(5000)

        // iv) remove Jar / Ant script
        delete(file("${out.absolutePath}/install-Ant-Wrapper.xml"))
        delete(file("${out.absolutePath}/ant-wrapper.jar"))
    }
}


/** 9) Create usable Ant installation (without Ant installed) */
