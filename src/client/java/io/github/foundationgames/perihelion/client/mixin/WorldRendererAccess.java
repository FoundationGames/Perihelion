package io.github.foundationgames.perihelion.client.mixin;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccess {
    @Accessor("starsBuffer")
    VertexBuffer perihelion$getStars();

    @Accessor("lightSkyBuffer")
    VertexBuffer perihelion$getSky();
}
