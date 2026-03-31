package xyz.bluspring.knit.loader.impl

import xyz.bluspring.knit.loader.KnitModLoader
import xyz.bluspring.knit.loader.api.KnitApi

data class KnitApiImpl(override val loader: KnitModLoader<*>): KnitApi {
}