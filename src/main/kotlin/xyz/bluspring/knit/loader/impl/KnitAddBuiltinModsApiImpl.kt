package xyz.bluspring.knit.loader.impl

import xyz.bluspring.knit.loader.KnitModLoader
import xyz.bluspring.knit.loader.api.KnitAddBuiltinModsApi
import xyz.bluspring.knit.loader.mod.ModDefinition

data class KnitAddBuiltinModsApiImpl(
    override val loader: KnitModLoader<*>, val modDefinitions: MutableList<ModDefinition> = mutableListOf()
): KnitAddBuiltinModsApi {
    override fun addModDefinition(mod: ModDefinition) {
        modDefinitions.add(mod)
    }
}
