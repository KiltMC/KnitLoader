plugins {
    id("org.quiltmc.loom") version "1.7.4"
}

base {
    archivesName.set("Knit-Loader-Quilt")
}

repositories {
    // normally, this should be included by default with Quilt Loom, not sure why it's not though..
    maven("https://maven.quiltmc.org/repository/release/")
}

loom {
    mods {
        // This should match your mod id.
        create("knit_loader") {
            sourceSet("main")
            dependency(dependencies.project(project.parent!!.path))
            // If you shade (directly include classes, not JiJ) a dependency into your mod, include it here using one of these methods:
            // dependency("com.example.shadowedmod:1.2.3")
            // configuration("exampleShadedConfigurationName")
        }
    }
}

val common by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:${rootProject.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation("org.quiltmc:quilt-loader:${property("quilt_loader_version")}")

    // TODO: use Quilt Kotlin Libraries when it's updated to Kotlin 2.1.21
    modImplementation ("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")

    api(project(project.parent!!.path))
    common(project(project.parent!!.path)) {
        isTransitive = false
    }
}

tasks {
    processResources {
        val properties = mutableMapOf(
            "version" to project.version,
            "fabric_kotlin_version" to project.property("fabric_kotlin_version")
        )

        for ((key, value) in properties) {
            inputs.property(key, value)
        }

        filteringCharset = "UTF-8"

        filesMatching("quilt.mod.json") {
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