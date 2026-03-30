package xyz.bluspring.knit.loader.api

import xyz.bluspring.knit.loader.KnitModLoader
import java.nio.file.Path

class KnitPreModScanApi(
    loader: KnitModLoader<*>, val modDirectories: MutableList<Path> = mutableListOf()
): KnitApi(loader) {
    fun addModDirectory(path: Path) {
        modDirectories.add(path)
    }
}