package xyz.bluspring.knit.loader.fabric.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.fabricmc.loader.api.ModContainer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import xyz.bluspring.knit.loader.KnitLoader;
import xyz.bluspring.knit.loader.fabric.KnitLoaderFabric;

import java.util.ArrayList;

@Pseudo
@Mixin(targets = {
    "net/minecraft/SystemReport",
    "net/minecraft/class_6396",
    "net/minecraft/util/SystemDetails"
}, priority = 1050)
public abstract class SystemReportMixin {
    // Knit: Filter our mods from Fabric Loader's list
    @Dynamic
    @TargetHandler(mixin = "net.fabricmc.fabric.mixin.crash.report.info.SystemDetailsMixin", name = "appendMods")
    @ModifyReceiver(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;iterator()Ljava/util/Iterator;"), require = 0)
    private static ArrayList<ModContainer> knit_loader$removeKnitModsFromList(ArrayList<ModContainer> instance) {
        return new ArrayList<>(instance.stream().filter(e -> !((KnitLoaderFabric) KnitLoader.Companion.getInstance()).getContainers().containsValue(e))
            .toList());
    }
}
