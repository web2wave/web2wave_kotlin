plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "2.1.10"


}

group = "com.web2wave"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(9)
}