package io.github.foundationgames.perihelion.client.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.foundationgames.perihelion.entity.RadiationBurningEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class RadiationBurnHudEffect implements ClientEntityEvents.Load, ClientTickEvents.EndWorldTick {
    public static final int FADE_TIME_TICKS = 35;

    private static final Identifier VIGNETTE = new Identifier("textures/misc/vignette.png");
    private static final Identifier NAUSEA = new Identifier("textures/misc/nausea.png");

    private boolean burning = false;
    private int burnTicks = 0;

    @Override
    public void onEndTick(ClientWorld world) {
        var player = MinecraftClient.getInstance().player;

        if (player instanceof RadiationBurningEntity rad) {
            burning = rad.isRadiationBurning();
        } else {
            burning = false;
        }

        burnTicks = MathHelper.clamp(burnTicks + (burning ? 1 : -1), 0, FADE_TIME_TICKS);
    }

    @Override
    public void onLoad(Entity entity, ClientWorld world) {
        if (entity == MinecraftClient.getInstance().player) {
            burning = false;
            burnTicks = 0;
        }
    }

    public void render(DrawContext context, float tickDelta, int w, int h) {
        float fade = MathHelper.clamp((burnTicks + (burning ? tickDelta : -tickDelta)) / FADE_TIME_TICKS, 0, 1);

        if (fade > 0) {
            float redFade = MathHelper.clamp(MathHelper.sqrt(fade * 4), 0, 1);

            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);

            fade *= fade;

            this.overlay(context, NAUSEA, w, h, 0.7f * fade, 0.7f * fade, 0.7f * fade, 1);
            this.overlay(context, VIGNETTE, w, h, 0.45f * redFade, 0, 0, 1);

            RenderSystem.defaultBlendFunc();
        }
    }

    private void overlay(DrawContext context, Identifier texture, int w, int h, float r, float g, float b, float a) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        context.setShaderColor(r, g, b, a);
        context.drawTexture(texture, 0, 0, -90, 0, 0, w, h, w, h);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        context.setShaderColor(1, 1, 1, 1);
    }
}
