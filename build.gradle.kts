plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.6"
}

base {
    archivesName.set("Knit-Loader")
}

val knitVersion = property("mod_version") as String
version = knitVersion

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.bawnorton.com/releases")
    }
}

subprojects {
    apply(plugin = "com.gradleup.shadow")
    version = knitVersion
}

dependencies {
    api("org.jetbrains:annotations:26.0.2")
    api("org.slf4j:slf4j-api:2.0.12")

    // Right off the bat, we'll get every Kotlin standard library we'd ever need from here.
    compileOnly("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")
}