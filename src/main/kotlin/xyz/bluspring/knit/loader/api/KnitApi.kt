package xyz.bluspring.knit.loader.api

import xyz.bluspring.knit.loader.KnitModLoader

/**
 * Simple API interface with access to the loader.
 */
interface KnitApi {
    /**
     * Access to the loader
     */
    val loader: KnitModLoader<*>
}