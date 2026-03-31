package xyz.bluspring.knit.loader.api

import xyz.bluspring.knit.loader.mod.ModDefinition

/**
 * Simple API interface with the ability to add built-in mod definitions.
 */
interface KnitAddBuiltinModsApi: KnitApi {
    /**
     * Adds the given mod definition alongside the built-in mod definitions for the current loader.
     */
    fun addModDefinition(mod: ModDefinition)
}