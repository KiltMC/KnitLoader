package xyz.bluspring.knit.loader.fabric

import de.florianreuth.asmfabricloader.api.event.PrePrePreLaunchEntrypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import xyz.bluspring.knit.loader.KnitLoader
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream

class KnitEarlierInitializer : PrePrePreLaunchEntrypoint {
    override fun onLanguageAdapterLaunch() {
        val loader = KnitLoaderFabric()

        runBlocking(Dispatchers.IO) {
            // Scans the mods from the game directory.
            try {
                val modsDirectories = Stream.concat(
                    Stream.of(FabricLoader.getInstance().gameDir),
                    FabricLoader.getInstance().allMods.stream().filter {
                            mod -> mod.containingMod.isEmpty && mod.origin.kind == ModOrigin.Kind.PATH
                    }.flatMap { path -> path.origin.paths.stream().map { path -> path.parent.parent } }
                ).collect(Collectors.toSet())
                loader.scanMods(modsDirectories)
            } catch (e: Throwable) {
                KnitLoader.instance.displayError("Errors occurred during mod scanning!", RuntimeException(e))
            }
        }
    }
}