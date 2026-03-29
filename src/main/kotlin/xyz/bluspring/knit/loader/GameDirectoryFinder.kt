package xyz.bluspring.knit.loader

import java.nio.file.Path

interface GameDirectoryFinder {
    fun findGameDirectories(): Collection<Path>
}