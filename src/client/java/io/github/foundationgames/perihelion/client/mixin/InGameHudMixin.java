package io.github.foundationgames.perihelion.client.mixin;

import io.github.foundationgames.perihelion.client.PerihelionClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow private int scaledWidth;
    @Shadow private int scaledHeight;

    @Inject(method = "render", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/MinecraftClient;getLastFrameDuration()F", ordinal = 0))
    private void perihelion$renderBurnHudEffect(DrawContext context, float tickDelta, CallbackInfo ci) {
        PerihelionClient.RADIATION_BURN_HUD.render(context, tickDelta, scaledWidth, scaledHeight);
    }
}
