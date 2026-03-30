package xyz.bluspring.knit.loader.api

import java.nio.file.Path

interface KnitModScanSetupApi: KnitApi {
    fun addModDirectory(path: Path)
}