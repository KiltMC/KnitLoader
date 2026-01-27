package xyz.bluspring.knit.loader.util

import xyz.bluspring.knit.loader.KnitLoader
import java.util.function.Consumer

object CrashReportHelper {
    @JvmStatic
    fun appendKnitMods(loaderConsumer: Consumer<Pair<String, String>>) {
        val loaders = KnitLoader.instance.loaders.sortedBy { it.id }

        for (loader in loaders) {
            var modString = ""
            val mods = loader.mods.sortedBy { it.definition.id }

            for (mod in mods) {
                modString += "\n"
                modString += "\t".repeat(2)
                modString += "${mod.definition.id}: ${mod.definition.displayName} ${mod.definition.version}"
            }

            loaderConsumer.accept("${loader.supportedLoader} Mods (${loader.id})" to modString)
        }
    }
}