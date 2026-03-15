plugins {
    kotlin("jvm")
}

group = "io.github.qobilidop"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
    workingDir = rootProject.projectDir
}
