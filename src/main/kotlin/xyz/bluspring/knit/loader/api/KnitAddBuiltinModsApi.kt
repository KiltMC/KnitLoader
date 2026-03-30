package xyz.bluspring.knit.loader.api

import xyz.bluspring.knit.loader.KnitModLoader
import xyz.bluspring.knit.loader.mod.ModDefinition

class KnitAddBuiltinModsApi(
    loader: KnitModLoader<*>, val modDefinitions: MutableList<ModDefinition> = mutableListOf()
): KnitApi(loader) {
    fun addModDefinition(mod: ModDefinition) {
        modDefinitions.add(mod)
    }
}