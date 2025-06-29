package xyz.bluspring.knit.loader

import org.jetbrains.annotations.ApiStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.knit.loader.mod.*
import xyz.bluspring.knit.loader.util.IncompatibleModException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.walk
import kotlin.system.exitProcess

/**
 * The internal loader of Knit, abstracted away for providing support towards Fabric, Quilt, and any future loaders that wish to be supported.
 * Developers should not need to use this class.
 */
@ApiStatus.Internal
abstract class KnitLoader<C>(val nativeModLoaderName: String) {
    val loaders = sortedSetOf<KnitModLoader<*>>(Comparator.comparing { loader -> loader.loadingPriority })
    val containers = mutableMapOf<KnitMod, C>()

    init {
        for (loader in ServiceLoader.load(KnitModLoader::class.java)) {
            logger.info("Found mod loader ${loader.id} for loader ${loader.supportedLoader}.")
            loaders.add(loader)
        }

        logger.info("Knit Loader initialized under $nativeModLoaderName mod loader.")
    }

    abstract fun isValidEnvironment(env: ModEnvironment): Boolean

    suspend fun scanMods(path: Path) {
        val startTime = System.currentTimeMillis()
        val loadersToDefinitions = Collections.synchronizedMap(mutableMapOf<KnitModLoader<*>, MutableSet<ModDefinition>>())

        logger.debug("Scanning for mods in path {}...", path)
        // Scans all mods, retrieving their mod definitions.
        for (loader in loaders) {
            logger.debug("Scanning for mods for loader {} ({})...", loader.id, loader.supportedLoader)

            for (scanDir in loader.modDirs) {
                logger.debug("Scanning for mods in directory {}...", scanDir)

                path.resolve(scanDir).walk().filter { !it.isDirectory() }.forEach { modPath ->
                    for (loader in loaders) {
                        try {
                            val definitionsToAdd = loader.getModDefinitions(modPath)

                            synchronized(loadersToDefinitions) {
                                val definitions = loadersToDefinitions.computeIfAbsent(loader) { Collections.synchronizedSet(mutableSetOf()) }

                                synchronized(definitions) {
                                    logger.debug(
                                        "Discovered mod definitions {} under path {} for loader {} ({})",
                                        definitions.joinToString(",") { it.id },
                                        modPath,
                                        loader.id,
                                        loader.supportedLoader
                                    )

                                    definitions.addAll(definitionsToAdd)
                                }
                            }
                        } catch (e: IncompatibleModException) {
                            // If the file has not been loaded by Fabric, throw an exception.
                            if (!fileExistsNatively(modPath))
                                throw e
                        } catch (e: Throwable) {
                            logger.error("Failed to load file ${modPath.fileName}!")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        val definitionsToLoad = mutableMapOf<ModDefinition, KnitModLoader<*>>()
        val mappedModIds = mutableMapOf<String, String>()

        logger.debug("Found ${definitionsToLoad.size} mod definitions.")

        // First pass, get all mods that are to be loaded by Knit.
        definitionsLoad@for (modId in loadersToDefinitions.values.flatten().distinctBy { it.id }.map { it.id }) {
            // Skip mod if the mod already exists natively
            for (loader in loaders) {
                if (modExistsNatively(loader.getNativeModId(modId, nativeModLoaderName))) {
                    logger.debug("Ignoring mod ID $modId, because mod already exists natively.")
                    continue@definitionsLoad
                }
            }

            // Get all definitions from other loaders that match this definition
            val definitions = loadersToDefinitions
                .mapNotNull { it.key to (it.value.firstOrNull { d -> d.id == modId } ?: return@mapNotNull null) }
                .filter { isValidEnvironment(it.second.environment) } // Remove definitions that don't match the environment.
                .sortedByDescending { it.second.version }

            // Our definitions list is empty, skip.
            if (definitions.isEmpty()) {
                logger.debug("No definitions loaded in this environment for mod ID $modId, skipping.")
                continue
            }

            // Use the definition with the newest version.
            val highestDefinition = definitions.first().second

            // If the mod is able to load natively within the native mod loader, we need to ignore it.
            // For instance, YetAnotherConfigLib provides a mods.toml that has an invalid mod ID, and there are
            // Forge mods that provide a fabric.mod.json that doesn't actually have any entrypoints.
            if (fileExistsNatively(highestDefinition.originalPath) && canModLoadNatively(highestDefinition.originalPath)) {
                logger.debug("Ignoring mod definition with path {} because it can load natively.", highestDefinition.originalPath)
                continue
            }

            // Then, sort by loading priority of loaders.
            val prioritizedDefinition = definitions.filter { it.second.version == highestDefinition.version }
                .maxBy { it.first.loadingPriority }

            // The definitions are then added in for the loader to consider.
            definitionsToLoad[prioritizedDefinition.second] = prioritizedDefinition.first

            // Store the natively mapped mod IDs, as in some cases, the official Fabric mods have different IDs from the Forge ID.
            for (dependency in prioritizedDefinition.second.dependencies) {
                if (!mappedModIds.contains(dependency.id))
                    mappedModIds[dependency.id] = prioritizedDefinition.first.getNativeModId(dependency.id, nativeModLoaderName)
            }
        }

        // We should also load the built-in mod definitions. This occurs after the definition loading above, because some mods may "provide" the mod in their respective metadata files,
        // so the modExistsNatively check may end up thinking it is available.
        logger.debug("Loading all built-in mod definitions...")
        for (loader in loaders) {
            for (definition in loader.getBuiltinModDefinitions()) {
                // If the definition somehow already exists, we need to overwrite it with the built-in mod definition.
                val existingDefinition = definitionsToLoad.keys.firstOrNull { it.id == definition.id }
                if (existingDefinition != null) {
                    val otherLoader = definitionsToLoad[existingDefinition]!!
                    logger.warn("Mod definition for ID ${definition.id} already exists! Overwriting. (existing: ${otherLoader.id}/${otherLoader.supportedLoader}, new: ${loader.id}/${loader.supportedLoader})")
                    definitionsToLoad.remove(existingDefinition)
                }

                logger.debug("Found built-in mod definition ${definition.id} for loader ${loader.id} (${loader.supportedLoader})")
                definitionsToLoad[definition] = loader
            }
        }

        // Second pass, validate all dependencies
        // This is in a separate method to allow for the Quilt module to override and handle the broken dependencies by itself.
        validateDependencies(definitionsToLoad, mappedModIds)

        // Third pass, create containers for all mod definitions.
        // Kilt is also able to use this for mod remapping and sorting, and they will be injected into the native mod loader later.
        logger.debug("Creating mod containers for all mod definitions...")
        for (loader in loaders.sortedBy { it.id }) {
            val loaderDefinitions = definitionsToLoad.filterValues { it == loader }.keys
            val containers = loader.createModContainers(loaderDefinitions)

            for (mod in containers) {
                (loader.mutableMods as MutableList<KnitMod>).add(mod)
            }

            for (definition in loaderDefinitions.sortedBy { it.id }) {
                logger.info("Found ${loader.supportedLoader} mod ${definition.displayName} (${definition.id}) version ${definition.version} (loaded by ${loader.id})")
            }
        }

        // We've finished mod scanning now, so let's notify the mod loaders so they can do whatever they want.
        logger.debug("Finished mod scanning, notifying mod loaders...")
        for (loader in loaders) {
            loader.finishModScanning()
        }

        logger.info("Finished scanning for mods. (took ${System.currentTimeMillis() - startTime} ms)")
    }

    protected open fun validateDependencies(definitions: Map<ModDefinition, KnitModLoader<*>>, mappedIds: Map<String, String>) {
        logger.debug("Validating dependencies for ${definitions.size} mods.")
        val failedDependencies = mutableListOf<DependencyState>()

        for (definition in definitions.keys) {
            for (dependency in definition.dependencies) {
                // Check if we should actually validate this dependency
                if (isValidEnvironment(dependency.side))
                    continue

                // Check if Dependency ID actually exists
                if (dependency.type.checkIsMissing && !modExistsNatively(mappedIds.getOrElse(dependency.id) { dependency.id }) && definitions.keys.none { it.id == dependency.id }) {
                    failedDependencies.add(MissingDependencyState(definition, dependency, ModVersion.EMPTY))
                    continue
                }

                // If the mod is built-in, focus on using the built-in definition
                val dependencyVersion = if (modExistsNatively(mappedIds.getOrElse(dependency.id) { dependency.id }) && definitions.keys.none { it.id == dependency.id && it.isBuiltin })
                    getNativeModVersion(dependency.id)
                else
                    definitions.keys.first { it.id == dependency.id }.version

                // Check if dependency constraints match
                if (dependency.constraint.matches(dependencyVersion.toString())) {
                    // If it is discouraged/incompatible, add it to the "failed dependencies" list.
                    if (dependency.type == ModDependency.Type.DISCOURAGED || dependency.type == ModDependency.Type.INCOMPATIBLE) {
                        failedDependencies.add(DependencyExists(definition, dependency, dependencyVersion))
                    }
                } else {
                    // If the constraints do not match, add it in too.
                    failedDependencies.add(MismatchedDependencyVersionState(definition, dependency, dependencyVersion))
                }

                // If everything passes, we don't have to do anything.
            }
        }

        // If something failed, be sure to throw the error.
        if (failedDependencies.isNotEmpty()) {
            displayDependencyError(failedDependencies)
        }
    }

    protected abstract fun <T : KnitMod> createContainer(mod: T): C

    // Injects mods into the native mod loader. This is in a separate method, because Fabric can only have mods injected
    // at the mixin plugin level, whereas the language provider level iterates through the mod candidates, and as such a CME will occur.
    open fun injectModsToLoader() {
        for (loader in loaders) {
            for (mod in loader.mods) {
                val container = createContainer(mod)
                this.containers[mod] = container
            }
        }
    }

    // Injects all modded mixins into the native mod loader. This may be handled by the native mod loader,
    // so we can intentionally keep in empty in some cases.
    // We should also ensure that the loaders are pre-initialized, before mixins get loaded themselves.
    open fun injectModMixins() {
        for (loader in loaders) {
            try {
                loader.preInitialize()
            } catch (e: Throwable) {
                logger.error("Failed to pre-initialize loader ${loader.id}/${loader.supportedLoader}!")
                e.printStackTrace()
            }
        }
    }

    // Displays both a GUI and CLI error to the user.
    protected fun displayDependencyError(failedDependencies: List<DependencyState>) {
        // This is the default CLI error, and should typically also be called.
        if (failedDependencies.any { it.dependency.type.shouldExitOnFail })
            logger.error("Knit Loader has detected some incompatible dependencies!")
        else
            logger.warn("Knit Loader has detected some suggested dependency changes:")

        val sortedDependencies = failedDependencies
            .map { it.mod to it }
            .groupBy { it.first }
            .mapValues { it.value.map { b -> b.second } }

        for ((mod, states) in sortedDependencies) {
            if (states.any { it.dependency.type.shouldExitOnFail })
                logger.error("- ${mod.displayName} (${mod.id} - ${mod.originalPath.fileName})")
            else
                logger.warn("- ${mod.displayName} (${mod.id} - ${mod.originalPath.fileName})")

            for (state in states) {
                if (state.dependency.type.shouldExitOnFail)
                    logger.error("  !! - $state")
                else
                    logger.warn("   - (optional) $state")
            }
        }

        if (failedDependencies.any { it.dependency.type.shouldExitOnFail }) {
            displayDependencyErrorGUI(failedDependencies)
        }
    }

    protected open fun displayDependencyErrorGUI(failedDependencies: List<DependencyState>) {
        exitProcess(1)
    }

    fun displayError(message: String, exception: Exception) {
        exception.printStackTrace()
        displayErrorGUI(message, exception)

        throw exception
    }

    open fun displayErrorGUI(message: String, exception: Exception) {
    }

    open fun displayErrorTreeGUI(message: String, exceptions: Map<String, Exception>) {
    }

    /**
     * Checks to see if the mod already exists in the native mod loader. Note that after mods are loaded into the native mod loader, that this will become inaccurate.
     */
    abstract fun modExistsNatively(id: String): Boolean
    abstract fun getNativeModVersion(id: String): ModVersion
    abstract fun fileExistsNatively(path: Path): Boolean
    abstract fun canModLoadNatively(path: Path): Boolean

    fun getLoaderById(id: String): KnitModLoader<*>? {
        return this.loaders.firstOrNull { it.id == id }
    }

    protected class MissingDependencyState(mod: ModDefinition, dependency: ModDependency, version: ModVersion) : DependencyState(mod, dependency, version) {
        override fun toString(): String {
            return "Missing mod with ID \"${dependency.id}\"! (required version: ${dependency.constraint})"
        }
    }

    protected class MismatchedDependencyVersionState(mod: ModDefinition, dependency: ModDependency, version: ModVersion) : DependencyState(mod, dependency, version) {
        override fun toString(): String {
            return "Incompatible version of mod ID \"${dependency.id}\"! (expected: ${dependency.constraint}, got: $version)"
        }
    }
    protected class DependencyExists(mod: ModDefinition, dependency: ModDependency, version: ModVersion) : DependencyState(mod, dependency, version)
    protected abstract class DependencyState(val mod: ModDefinition, val dependency: ModDependency, val version: ModVersion)

    companion object {
        val logger: Logger = LoggerFactory.getLogger("Knit Loader")

        // The instance of Knit Loader that's being used, usually based on the native mod loader.
        lateinit var instance: KnitLoader<*>
    }
}