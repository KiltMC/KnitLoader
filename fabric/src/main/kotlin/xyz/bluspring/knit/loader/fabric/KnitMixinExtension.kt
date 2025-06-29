package xyz.bluspring.knit.loader.fabric

import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.ext.IExtension
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext
import xyz.bluspring.knit.loader.KnitLoader

class KnitMixinExtension : IExtension {
    override fun checkActive(environment: MixinEnvironment?): Boolean {
        KnitLoader.instance.injectModMixins()
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