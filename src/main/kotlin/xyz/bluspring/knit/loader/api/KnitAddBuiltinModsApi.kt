package xyz.bluspring.knit.loader.api

import xyz.bluspring.knit.loader.mod.ModDefinition

interface KnitAddBuiltinModsApi: KnitApi {
    fun addModDefinition(mod: ModDefinition)
}