plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        kotlin.setSrcDirs(listOf("."))
        kotlin.exclude("*Test.kt")
    }
    test {
        kotlin.setSrcDirs(listOf("."))
        kotlin.include("*Test.kt")
    }
}

dependencies {
    implementation(project(":p4kt"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
    workingDir = rootProject.projectDir
}
