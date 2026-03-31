package xyz.bluspring.knit.loader.api

import java.nio.file.Path

/**
 * Simple API interface with the ability to add additional mod directories.
 */
interface KnitModScanSetupApi: KnitApi {
    /**
     * Adds a directory to scan for mods.
     * This means the loader will attempt to load any mods in the given directory.
     */
    fun addModDirectory(path: Path)
}