package io.github.foundationgames.perihelion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.foundationgames.perihelion.Perihelion;
import io.github.foundationgames.perihelion.entity.SolarSailBoatEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class WarningTextHud implements ClientTickEvents.EndWorldTick, HudRenderCallback, ClientEntityEvents.Load {
    public static final Text GRAVITY_WARNING = Text.translatable("message.perihelion.sun_gravity").formatted(Formatting.RED);

    private Text toRender = Text.empty();
    private int timer = 0;

    private boolean solarSailed = false;

    public static Text getSolarBoatControlHint() {
        return Text.translatable("message.perihelion.boat_control_hint", Text.translatable(MinecraftClient.getInstance().options.jumpKey.getBoundKeyTranslationKey())).formatted(Formatting.YELLOW);
    }

    public void setWarning(Text text) {
        toRender = text;
        timer = 120;
    }

    @Override
    public void onEndTick(ClientWorld world) {
        if (timer > 0) {
            timer--;
        }

        if (MinecraftClient.getInstance().player.getVehicle() instanceof SolarSailBoatEntity boat) {
            if (boat.getPanelGlow(0) > 0) {
                if (!solarSailed) {
                    setWarning(getSolarBoatControlHint());
                }

                solarSailed = true;
            }
        } else {
            solarSailed = false;
        }
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        float a = 0;

        if (timer > 115) {
            a = MathHelper.clamp(120 - (timer - tickDelta), 0, 5) * 0.2f;
        } else {
            a = MathHelper.clamp(timer - tickDelta, 0, 20) * 0.05f;
        }

        if (a > 0) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, a);
            drawContext.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, toRender, drawContext.getScaledWindowWidth() / 2, 35, 0xFFFFFF);

            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    @Override
    public void onLoad(Entity entity, ClientWorld world) {
        if (entity == MinecraftClient.getInstance().player && world.getRegistryKey() == Perihelion.THE_SUN_DIMENSION) {
            this.setWarning(GRAVITY_WARNING);
        }
    }
}
