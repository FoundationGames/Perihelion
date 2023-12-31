package io.github.foundationgames.perihelion.client.mixin;

import io.github.foundationgames.perihelion.client.world.WorldRendererDuck;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements WorldRendererDuck {
    public Runnable perihelion$fogCallback = null;

    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"))
    private void perihelion$cacheFogCallbackBecauseFabricApiDoesNot(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        perihelion$fogCallback = fogCallback;
    }

    @Override
    public Runnable perihelion$consumeFogCallback() {
        Runnable callback = perihelion$fogCallback == null ? () -> {} : perihelion$fogCallback;
        this.perihelion$fogCallback = null;

        return callback;
    }
}
