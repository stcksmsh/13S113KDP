import org.gradle.api.tasks.JavaExec
import java.util.concurrent.Executors
import java.util.concurrent.Future

plugins {
    kotlin("jvm") version "1.8.10"
    application
}

group = "io.github.stcksmsh"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("io.github.stcksmsh.App")
}

tasks.withType<Jar>().configureEach {
    // Set the duplicatesStrategy globally for all Jar tasks
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks {
    // Create JAR task for WorkerNode
    val createWorkerJar by registering(Jar::class) {
        group = "Build"
        description = "Assembles a runnable JAR for the WorkerNode."

        from(sourceSets["main"].output)
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
        })

        manifest {
            attributes["Main-Class"] = "io.github.stcksmsh.kdp.worker.WorkerNode"
        }

        archiveFileName.set("worker-node.jar")
    }

    // Create JAR task for ServerNode
    val createServerJar by registering(Jar::class) {
        group = "Build"
        description = "Assembles a runnable JAR for the ServerNode."

        from(sourceSets["main"].output)
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
        })

        manifest {
            attributes["Main-Class"] = "io.github.stcksmsh.kdp.server.ServerNode"
        }

        archiveFileName.set("server-node.jar")
    }

    val createJars by registering() {
        dependsOn(createWorkerJar, createServerJar)
    }

    // Define Docker build for WorkerNode
    val buildWorkerDocker by registering(Exec::class) {
        group = "Docker"
        description = "Builds the Docker image for WorkerNode."

        dependsOn(createWorkerJar)

        commandLine("docker", "build", "-t", "worker-node:latest", "-f", "docker/Dockerfile.worker", ".")
    }

    // Define Docker build for ServerNode
    val buildServerDocker by registering(Exec::class) {
        group = "Docker"
        description = "Builds the Docker image for ServerNode."

        dependsOn(createServerJar)

        commandLine("docker", "build", "-t", "server-node:latest", "-f", "docker/Dockerfile.server", ".")
    }

    val buildDockers by registering() {
        dependsOn(buildWorkerDocker, buildServerDocker)
    }

    val composeUp by registering(Exec::class) {
        group = "Docker"
        description = "Starts the Docker containers."

        dependsOn(buildDockers)

        commandLine("docker", "compose", "up")
    }
}
tasks.test {
    useJUnitPlatform()
}