package xyz.bluspring.knit.loader.fabric

import de.florianreuth.asmfabricloader.api.event.PrePrePreLaunchEntrypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import xyz.bluspring.knit.loader.KnitLoader
import java.nio.file.Path

class KnitEarlierInitializer : PrePrePreLaunchEntrypoint {
    fun getDirectoryContainingTheModsDirectory(): Path {
        if (FabricLoader.getInstance().isDevelopmentEnvironment) return FabricLoader.getInstance().gameDir
        // Use the mod container to find the mods directory if possible, to account for mods like AutoModpack which use a different directory.
        var container = FabricLoader.getInstance().getModContainer("kilt")
        // Just in case someone decides to bundle Kilt for some reason.
        while (container.isPresent && container.get().containingMod.isPresent) {
            container = container.get().containingMod
        }
        return if (
            container.isPresent && container.get().origin.kind != ModOrigin.Kind.PATH ||
            container.get().origin.paths.isEmpty()
        ) {
            FabricLoader.getInstance().gameDir
        } else {
            container.get().origin.paths[0].parent.parent
        }
    }

    override fun onLanguageAdapterLaunch() {
        val loader = KnitLoaderFabric()

        runBlocking(Dispatchers.IO) {
            // Scans the mods from the game directory.
            try {
                loader.scanMods(getDirectoryContainingTheModsDirectory())
            } catch (e: Throwable) {
                KnitLoader.instance.displayError("Errors occurred during mod scanning!", RuntimeException(e))
            }
        }
    }
}