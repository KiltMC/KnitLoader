plugins {
    id("fabric-loom")// version "1.11-SNAPSHOT"
}

val common by configurations.creating

base {
    archivesName.set("Knit-Loader-Fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.parent?.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("loader_version")}")

    modImplementation ("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_version")}")

    // Just because I like Kotlin more than Java
    modImplementation ("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")

    // Cursed Fabric/Mixin stuff
    include(modApi("xyz.bluspring:AsmFabricLoader:${project.parent?.property("asmfabricloader_version")}")!!)
    include(implementation(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:${rootProject.property("mixin_squared_version")}")!!)!!)

    api(project(project.parent!!.path))
    common(project(project.parent!!.path)) {
        isTransitive = false
    }
}

tasks {
    processResources {
        val properties = mutableMapOf(
            "version" to project.version,
            "loader_version" to rootProject.property("loader_version"),
            "fabric_version" to rootProject.property("fabric_version"),
            "minecraft_version" to rootProject.property("minecraft_version"),
            "fabric_kotlin_version" to project.property("fabric_kotlin_version")
        )

        for ((key, value) in properties) {
            inputs.property(key, value)
        }

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            // Use this instead of expand, as otherwise Gradle hard-errors when finding unknown $ names, and treats them as properties.
            this.filter {
                if (it.contains("\${")) {
                    var newString = it

                    for ((name, property) in properties) {
                        newString = newString.replace("\${$name}", property.toString())
                    }

                    return@filter newString
                }

                it
            }
        }
    }

    shadowJar {
        configurations = listOf(common)
        archiveClassifier = "dev-shadow"
    }

    remapJar {
        inputFile.set(project.tasks.shadowJar.get().archiveFile)
        archiveClassifier = null
        dependsOn(project.tasks.shadowJar)
    }
}