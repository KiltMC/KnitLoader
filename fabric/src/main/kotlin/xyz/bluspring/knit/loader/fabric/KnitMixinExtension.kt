package xyz.bluspring.knit.loader.fabric

import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.ext.IExtension
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext
import xyz.bluspring.knit.loader.KnitLoader

class KnitMixinExtension : IExtension {
    companion object {
        private var wasInitialized = false
    }

    override fun checkActive(environment: MixinEnvironment?): Boolean {
        if (!wasInitialized) {
            KnitLoader.instance.injectModMixins()
            wasInitialized = true
        }
        return false
    }

    override fun preApply(context: ITargetClassContext?) {
    }

    override fun postApply(context: ITargetClassContext?) {
    }

    override fun export(
        env: MixinEnvironment?,
        name: String?,
        force: Boolean,
        classNode: ClassNode?
    ) {
    }
}