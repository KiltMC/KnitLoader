plugins {
    alias(libs.plugins.fabric.loom)
}

val common by configurations.creating

base {
    archivesName.set("knit-loader-fabric")
}

loom {
    mixin {
        useLegacyMixinAp = false
    }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)

    // Just because I like Kotlin more than Java
    modImplementation(libs.fabric.kotlin)

    // Cursed Fabric/Mixin stuff
    include(libs.asmfabricloader)
    modApi(libs.asmfabricloader)

    include(libs.mixinsquared)
    implementation(libs.mixinsquared)
    annotationProcessor(libs.mixinsquared)

    api(project(project.parent!!.path))
    common(project(project.parent!!.path)) {
        isTransitive = false
    }
}

tasks {
    processResources {
        val properties = mutableMapOf(
            "version" to project.version,
            "loader_version" to libs.versions.fabric.loader.get(),
            "minecraft_version" to libs.versions.minecraft.get(),
            "fabric_kotlin_version" to libs.versions.fabric.kotlin.get(),
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
