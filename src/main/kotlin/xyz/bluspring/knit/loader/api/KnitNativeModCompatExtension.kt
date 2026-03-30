package xyz.bluspring.knit.loader.api

interface KnitNativeModCompatExtension {
    fun setupModScanning(api: KnitModScanSetupApi) {}
    fun onCreateBuiltinModDefinitions(api: KnitAddBuiltinModsApi) {}
    fun beforeFinishScanning(api: KnitApi) {}
    fun afterFinishScanning(api: KnitApi) {}
}