package xyz.bluspring.knit.loader.api

import xyz.bluspring.knit.loader.KnitModLoader

data class KnitApiImpl(override val loader: KnitModLoader<*>): KnitApi {
}