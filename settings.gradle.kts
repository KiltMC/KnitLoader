pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "FabricMC"
        }
        maven("https://maven.quiltmc.org/repository/release/") {
            name = "QuiltMC"
        }
        maven("https://maven.quiltmc.org/repository/snapshot/") {
            name = "QuiltMC"
        }
        maven("https://dl.bintray.com/brambolt/public")
        maven("https://mvn.devos.one/releases") {
            name = "devOS Releases"
        }
        maven("https://mvn.devos.one/snapshots") {
            name = "devOS Releases"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

include(":cichlid")
include(":fabric")
include(":quilt")
