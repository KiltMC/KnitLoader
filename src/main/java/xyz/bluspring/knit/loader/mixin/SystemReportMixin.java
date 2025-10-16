package xyz.bluspring.knit.loader.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bluspring.knit.loader.KnitLoader;
import xyz.bluspring.knit.loader.KnitModLoader;
import xyz.bluspring.knit.loader.mod.KnitMod;

import java.util.Comparator;
import java.util.function.Supplier;

@Pseudo
@Mixin(targets = {
    "net/minecraft/SystemReport", // Mojmap
    "net/minecraft/class_6396", // Intermediary
    "net/minecraft/util/SystemDetails" // Yarn
}, priority = 1050)
public abstract class SystemReportMixin {
    @Shadow(aliases = {"method_37122", "addSection"})
    public abstract void setDetail(String identifier, Supplier<String> valueSupplier);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void knit_loader$appendKnitMods(CallbackInfo ci) {
        var loaders = KnitLoader.Companion.getInstance().getLoaders()
            .stream()
            .sorted(Comparator.comparing(KnitModLoader::getId))
            .toList();

        for (KnitModLoader<?> loader : loaders) {
            this.setDetail(loader.getSupportedLoader() + " Mods (" + loader.getId() + ")", () -> {
                var modString = new StringBuilder();
                var mods = loader.getMods().stream().sorted(Comparator.comparing(e -> e.getDefinition().getId())).toList();

                for (KnitMod mod : mods) {
                    modString.append('\n');
                    modString.append("\t".repeat(2));
                    modString.append(mod.getDefinition().getId());
                    modString.append(": ");
                    modString.append(mod.getDefinition().getDisplayName());
                    modString.append(' ');
                    modString.append(mod.getDefinition().getVersion());
                }

                return modString.toString();
            });
        }
    }
}
