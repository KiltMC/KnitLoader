package xyz.bluspring.knit.loader.api

import xyz.bluspring.knit.loader.KnitModLoader
import java.nio.file.Path

data class KnitModScanSetupApiImpl(
    override val loader: KnitModLoader<*>, val modDirectories: MutableList<Path> = mutableListOf()
): KnitModScanSetupApi {
    override fun addModDirectory(path: Path) {
        modDirectories.add(path)
    }
}
