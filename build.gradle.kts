plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

val knitVersion = property("mod_version") as String
version = knitVersion

base {
    archivesName.set("knit-loader-common")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.bawnorton.com/releases")
        maven("https://maven.florianreuth.de/snapshots")
    }

    val javaVersion = 17

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    kotlin {
        jvmToolchain(javaVersion)
    }

    group = "xyz.bluspring.knit-loader"
    version = knitVersion
}

subprojects {
    apply(plugin = "com.gradleup.shadow")
}

dependencies {
    api("org.jetbrains:annotations:26.0.2")
    api("org.slf4j:slf4j-api:2.0.12")

    // Right off the bat, we'll get every Kotlin standard library we'd ever need from here.
    compileOnly(libs.fabric.kotlin)

    // Loader-independent mixins :D
//    compileOnly("net.fabricmc:sponge-mixin:${property("fabric_mixin_version")}")
}
