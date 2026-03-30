package xyz.bluspring.knit.loader.api

interface KnitNativeModCompatExtension {
    fun beforeModScan(api: KnitPreModScanApi) {}
    fun onBuiltinModDefinitions(api: KnitAddBuiltinModsApi) {}
    fun beforeFinishScanning(api: KnitApi) {}
}