package dev.ii8we.acquiredutils.client.mixin;

import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public final class LevelRendererMixin {

    @Inject(
        method = "doEntityOutline",
        at = @At("HEAD"),
        cancellable = true
    )
    private void acquiredutils$disableEntityOutline(CallbackInfo ci) {
        if (AcquiredUtilsConfig.get().disableGlowingEffects) {
            ci.cancel();
        }
    }
}
