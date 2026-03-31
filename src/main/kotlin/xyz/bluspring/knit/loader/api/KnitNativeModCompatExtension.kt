package xyz.bluspring.knit.loader.api

/**
 * Service for Knit initialization extensions.
 * @see java.util.ServiceLoader
 */
interface KnitNativeModCompatExtension {
    /**
     * Called before mod scan begins.
     * Primarily intended to add additional directories to scan for mods.
     */
    fun setupModScanning(api: KnitModScanSetupApi) {}

    /**
     * Called after mods have been collected from the mod directories but before built-in mod definitions are added.
     * Primarily intended to add additional built-in mod definitions.
     */
    fun onCreateBuiltinModDefinitions(api: KnitAddBuiltinModsApi) {}

    /**
     * Called after mods have been collected but before the loader performs the final scan step (such as sorting and loading the mod classes).
     */
    fun beforeFinishScanning(api: KnitApi) {}

    /**
     * Called for the given loader when the mod scan has been completed.
     */
    fun afterFinishScanning(api: KnitApi) {}
}