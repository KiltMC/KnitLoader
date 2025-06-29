package xyz.bluspring.knit.loader.fabric

import com.llamalad7.mixinextras.utils.MixinInternals
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo
import xyz.bluspring.knit.loader.KnitLoader

class KnitMixinPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String?) {
        MixinInternals.registerExtension(KnitMixinExtension())

        KnitLoader.logger.info("Injecting loaded Knit mods into Fabric...")
        KnitLoader.instance.injectModsToLoader()
    }

    override fun getRefMapperConfig(): String? {
        return null
    }

    override fun shouldApplyMixin(targetClassName: String?, mixinClassName: String?): Boolean {
        return true
    }

    override fun acceptTargets(
        myTargets: Set<String?>?,
        otherTargets: Set<String?>?
    ) {
    }

    override fun getMixins(): List<String?>? {
        return null
    }

    override fun preApply(
        targetClassName: String?,
        targetClass: ClassNode?,
        mixinClassName: String?,
        mixinInfo: IMixinInfo?
    ) {
    }

    override fun postApply(
        targetClassName: String?,
        targetClass: ClassNode?,
        mixinClassName: String?,
        mixinInfo: IMixinInfo?
    ) {
    }
}