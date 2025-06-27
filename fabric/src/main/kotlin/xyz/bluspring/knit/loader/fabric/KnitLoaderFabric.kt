package xyz.bluspring.knit.loader.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.ModContainerImpl
import net.fabricmc.loader.impl.discovery.ModCandidateImpl
import net.fabricmc.loader.impl.gui.FabricGuiEntry
import net.fabricmc.loader.impl.gui.FabricStatusTree
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.metadata.LoaderModMetadata
import net.fabricmc.loader.impl.util.FileSystemUtil
import org.spongepowered.asm.mixin.FabricUtil
import org.spongepowered.asm.mixin.Mixins
import xyz.bluspring.knit.loader.KnitLoader
import xyz.bluspring.knit.loader.KnitModLoader
import xyz.bluspring.knit.loader.fabric.mod.FabricModMetadata
import xyz.bluspring.knit.loader.mod.KnitMod
import xyz.bluspring.knit.loader.mod.ModDefinition
import xyz.bluspring.knit.loader.mod.ModEnvironment
import xyz.bluspring.knit.loader.mod.ModVersion
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.extension

class KnitLoaderFabric : KnitLoader<ModContainerImpl>("Fabric") {
    private val candidates = mutableMapOf<ModDefinition, ModCandidateImpl>()
    private val loaderMetadatas = mutableMapOf<ModDefinition, LoaderModMetadata>()

    fun getModCandidate(mod: ModDefinition): ModCandidateImpl {
        return candidates[mod]!!
    }

    fun getLoaderMetadata(mod: ModDefinition): LoaderModMetadata {
        return loaderMetadatas[mod]!!
    }

    fun createLoaderMetadata(mod: ModDefinition, loader: KnitModLoader<*>): LoaderModMetadata {
        if (loaderMetadatas.containsKey(mod))
            return getLoaderMetadata(mod)

        return FabricModMetadata(mod, loader).apply {
            loaderMetadatas[mod] = this
        }
    }

    private fun createModCandidate(mod: ModDefinition, loader: KnitModLoader<*>): ModCandidateImpl {
        if (candidates.containsKey(mod))
            return getModCandidate(mod)

        //createPlain(List<Path> paths, LoaderModMetadata metadata, boolean requiresRemap, Collection<ModCandidate> nestedMods)
        val createPlainMethod = ModCandidateImpl::class.java.getDeclaredMethod("createPlain", List::class.java, LoaderModMetadata::class.java, Boolean::class.java, Collection::class.java)
        createPlainMethod.isAccessible = true

        val metadata = createLoaderMetadata(mod, loader)

        return createPlainMethod.invoke(this, listOf(mod.path), metadata, false, mutableListOf<ModCandidateImpl>()) as ModCandidateImpl
    }

    override fun isValidEnvironment(env: ModEnvironment): Boolean {
        return when (env) {
            ModEnvironment.BOTH -> true
            ModEnvironment.CLIENT -> FabricLoader.getInstance().environmentType == EnvType.CLIENT
            ModEnvironment.SERVER -> FabricLoader.getInstance().environmentType == EnvType.SERVER
        }
    }

    override fun <T : KnitMod> createContainer(mod: T): ModContainerImpl {
        val container = ModContainerImpl(createModCandidate(mod.definition, loaders.first { it.mods.contains(mod) } as KnitModLoader<*>))

        FabricLoaderImpl.INSTANCE.modsInternal.add(container)

        // Define the container into the internal mod map, even though the internal mods list knows,
        // the map doesn't.
        val modMapField = FabricLoaderImpl::class.java.getDeclaredField("modMap")
        modMapField.isAccessible = true
        val modMap = modMapField.get(FabricLoaderImpl.INSTANCE) as MutableMap<String, ModContainerImpl>

        modMap[mod.definition.id] = container

        // Add the mod to the class path.
        FabricLauncherBase.getLauncher().addToClassPath(mod.definition.path)

        // Force Java to be aware of the mod in the file system.
        // This is so any mods that try to open for resources from itself won't fail.
        if (mod.definition.path != mod.definition.originalPath) // *Some* libraries don't work correctly, somehow.
            FileSystemUtil.getJarFileSystem(mod.definition.path, true)

        return container
    }

    override fun injectModMixins() {
        // We should ensure that the loaders are pre-initialized, before the mixins get loaded themselves.
        super.injectModMixins()

        // Currently praying that having a similarly named mixin config isn't actually a problem
        val configToModMap = mutableMapOf<String, ModContainerImpl>()

        for (loader in loaders) {
            for (mod in loader.mods) {
                for (config in mod.definition.mixinConfigs) {
                   try {
                       // If this environment isn't valid for the mixin, just ignore.
                       if (!isValidEnvironment(config.environment))
                           continue

                       // Map the mixin config to the following container. This is specifically so we don't
                       // end up causing a CME later.
                       configToModMap[config.config] = containers[mod]!!

                       // Add the configuration into mixin directly.
                       Mixins.addConfiguration(config.config)
                   } catch (e: Throwable) {
                       logger.error("Failed to load mixins for ${mod.definition.displayName} (${mod.definition.id}), loaded by ${loader.id}/${loader.supportedLoader}!")
                       e.printStackTrace()
                   }
                }
            }
        }

        // Iterate through the mixin configs so we can add identifying data to them.
        for (rawConfig in Mixins.getConfigs()) {
            // We can safely ignore the config if we didn't handle it.
            val mod = configToModMap[rawConfig.name] ?: continue

            val config = rawConfig.config
            config.decorate(FabricUtil.KEY_MOD_ID, mod.metadata.id)
            // Set the compatibility key to latest, and kinda just hope for the best.
            config.decorate(FabricUtil.KEY_COMPATIBILITY, FabricUtil.COMPATIBILITY_LATEST)
        }
    }

    override fun modExistsNatively(id: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(id)
    }

    override fun fileExistsNatively(path: Path): Boolean {
        if (path.extension == "jar") {
            val jar = JarFile(path.toFile())

            // It's easier to just check for this, honestly.... >:(
            return jar.getEntry("fabric.mod.json") != null
        }

        return false
    }

    override fun displayDependencyErrorGUI(failedDependencies: List<DependencyState>) {
        FabricGuiEntry.displayError("Knit Loader has detected some incompatible dependencies!", null, { tree ->
            val errorTab = tree.addTab("Knit Error")

            val sortedDependencies = failedDependencies
                .map { it.mod to it }
                .groupBy { it.first }
                .mapValues { it.value.map { b -> b.second } }

            for ((mod, states) in sortedDependencies) {
                if (states.none { it.dependency.type.shouldExitOnFail })
                    continue

                val modBranch = errorTab.node.addMessage("${mod.displayName} (${mod.id}):", FabricStatusTree.FabricTreeWarningLevel.ERROR)

                for (state in states) {
                    if (!state.dependency.type.shouldExitOnFail)
                        continue

                    modBranch.addMessage(state.toString(), FabricStatusTree.FabricTreeWarningLevel.ERROR)
                }
            }

            tree.tabs.removeIf { tab -> tab != errorTab }
        }, true)

        super.displayDependencyErrorGUI(failedDependencies)
    }

    override fun displayErrorGUI(message: String, exception: Exception) {
        FabricGuiEntry.displayError(message, exception, true)

        super.displayErrorGUI(message, exception)
    }

    override fun displayErrorTreeGUI(message: String, exceptions: Map<String, Exception>) {
        FabricGuiEntry.displayError(message, null, { tree ->
            val errorTab = tree.addTab("Knit Error")

            for ((msg, exception) in exceptions) {
                errorTab.node.addMessage(msg, FabricStatusTree.FabricTreeWarningLevel.ERROR)
                    .addCleanedException(exception)
            }

            tree.tabs.removeIf { tab -> tab != errorTab }
        }, true)

        super.displayErrorTreeGUI(message, exceptions)
    }

    private val modCandidatesField = FabricLoaderImpl::class.java.getDeclaredField("modCandidates")

    init {
        modCandidatesField.isAccessible = true
    }

    override fun canModLoadNatively(path: Path): Boolean {
        val candidates = (modCandidatesField.get(FabricLoaderImpl.INSTANCE) as List<ModCandidateImpl>?)

        if (candidates == null)
            return false

        // Try to find a mod candidate that matches the provided path
        val matchingCandidate = candidates.firstOrNull { it.paths.any { p -> p == path } || it.originPaths.any { p -> p == path } } ?: return false

        // If the entrypoint keys are empty, that likely means it does not run natively at all, same with the mixin configs and language adapters too.
        return matchingCandidate.metadata.entrypointKeys.isNotEmpty() || matchingCandidate.metadata.getMixinConfigs(FabricLoader.getInstance().environmentType).isNotEmpty() || matchingCandidate.metadata.languageAdapterDefinitions.isNotEmpty()
    }

    override fun getNativeModVersion(id: String): ModVersion {
        return FabricModVersion(FabricLoader.getInstance().getModContainer(id).orElseThrow().metadata.version)
    }
}