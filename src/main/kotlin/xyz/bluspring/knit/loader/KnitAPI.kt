package xyz.bluspring.knit.loader

import java.nio.file.Path

interface KnitAPI {
    fun beforeModScan()

    fun addGameDirectory(path: Path) {
        KnitLoader.instance.addGameDirectory(path)
    }
}