package xyz.bluspring.knit.loader.api

interface KnitNativeModCompatExtension {
    fun beforeModScan(api: KnitPreModScanApi) {}
}