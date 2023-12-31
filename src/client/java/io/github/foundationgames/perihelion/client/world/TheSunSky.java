package io.github.foundationgames.perihelion.client.world;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.foundationgames.perihelion.Perihelion;
import io.github.foundationgames.perihelion.client.mixin.WorldRendererAccess;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

public class TheSunSky implements DimensionRenderingRegistry.SkyRenderer, ClientTickEvents.StartWorldTick {
    public static final Identifier TEXTURE = Perihelion.id("textures/environment/sun_surface.png");

    // Texture aspect ratio 5:1
    public static final float U_SCALE = 0.2f;
    public static final float V_SCALE = 1f;

    public static final float[][] SOLAR_FLARE_UVS = new float[][] {
            new float[] {0.65f, 0.25f}, new float[] {0.75f, 0.25f}, new float[] {0.65f, 0.75f}, new float[] {0.75f, 0.75f}
    };

    private final Set<SolarFlare> solarFlares = new HashSet<>();
    private final Set<SurfaceDecor> surfaceDecor = new HashSet<>();

    private int time = 0;

    private float flareTimer = 0;
    private float decorTimer = 0;

    public float globalSpeed = 0.01f;

    public TheSunSky() {
    }

    @Override
    public void onStartTick(ClientWorld world) {
        time++;

        if (flareTimer <= 0) {
            flareTimer = world.random.nextBetween(25, 110) * 100 * globalSpeed;

            float solarFlareX = world.random.nextBoolean() ? -0.9f : 0.3f;
            solarFlareX += world.random.nextFloat() * 0.6f;
            solarFlares.add(new SolarFlare(world.random.nextInt(SOLAR_FLARE_UVS.length), time, solarFlareX));
        } else flareTimer--;

        if (decorTimer <= 0) {
            decorTimer = world.random.nextBetween(2, 10) * 100 * globalSpeed;

            float decorX = world.random.nextBoolean() ? -0.9f : 0.25f;
            decorX += world.random.nextFloat() * 0.65f;
            surfaceDecor.add(new SurfaceDecor(world.random.nextInt(24) == 0, world.random.nextFloat(), time, decorX));
        } else decorTimer--;

        solarFlares.removeIf(f -> f.z(this, time + 1) < -1);
        surfaceDecor.removeIf(d -> d.z(this, time + 1) < -1);
    }

    @Override
    public void render(WorldRenderContext context) {
        final float tickDelta = context.tickDelta();
        final float time = this.time + tickDelta;

        final var matrices = context.matrixStack();
        final var buffer = Tessellator.getInstance().getBuffer();

        final var projMat = context.projectionMatrix();
        final var world = context.world();
        final var camera = context.camera();
        final var worldRender = context.worldRenderer();
        final var worldRender2 = (WorldRendererDuck) worldRender;
        final var worldRenderAcc = (WorldRendererAccess) worldRender;

        final var fogCallback = worldRender2.perihelion$consumeFogCallback();

        // Sky
        final var skyBuffer = worldRenderAcc.perihelion$getSky();
        final var skyColor = world.getSkyColor(camera.getPos(), tickDelta);
        BackgroundRenderer.applyFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor((float) skyColor.x, (float) skyColor.y, (float) skyColor.z, 1);
        skyBuffer.bind();
        skyBuffer.draw(matrices.peek().getPositionMatrix(), projMat, RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.depthMask(true);

        // Stars
        BackgroundRenderer.clearFog();
        matrices.push();

        final var starBuffer = worldRenderAcc.perihelion$getStars();
        matrices.multiply(RotationAxis.NEGATIVE_X.rotation(time * globalSpeed * 0.3f));
        starBuffer.bind();
        starBuffer.draw(matrices.peek().getPositionMatrix(), context.projectionMatrix(), GameRenderer.getPositionProgram());
        VertexBuffer.unbind();

        matrices.pop();

        // Sun
        matrices.push();

        matrices.translate(0, -3.2, 0);
        matrices.scale(64, 64, 64);

        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.setShaderTexture(0, TEXTURE);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);

        // Lower plasma
        buildOct(matrices, buffer, U_SCALE * 0.5f, V_SCALE * 0.5f, 0.1f, MathHelper.floorMod(0.5f + (time * globalSpeed), 1), 1, 1, 1, 1);

        // Upper plasma
        matrices.translate(0, 0.0001, 0);
        buildOct(matrices, buffer, U_SCALE * 0.5f, V_SCALE * 0.5f, 0.3f, MathHelper.floorMod(0.5f + (time * (globalSpeed + 0.003f)), 1), 1, 1, 1, 0.7f);

        // Distance fade ring
        matrices.translate(0, 0.008, 0);
        buildSquare(matrices, buffer, U_SCALE * 0.5f, V_SCALE * 0.5f, 0.5f, 0.5f, 1, 1, 1, 1);

        // Large solar flares
        for (var flare : solarFlares) {
            matrices.push();
            flare.render(this, matrices, buffer, time);
            matrices.pop();
        }

        // Small surface decor
        for (var decor : surfaceDecor) {
            matrices.push();
            decor.render(this, matrices, buffer, time);
            matrices.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();

        BackgroundRenderer.applyFogColor();
        fogCallback.run();
    }

    // For solar flares
    public void buildBillboard(MatrixStack mat, VertexConsumer buffer, float x, float z, float us, float vs, float u, float v, float r, float g, float b, float a) {
        float sdf = Math.max(0, 1 - MathHelper.sqrt(x * x + z * z)); // How inside the sun circle is this billboard (signed distance field)
        float sqrtsdf = MathHelper.sqrt(sdf);

        float scale = 0.4f + sqrtsdf * 0.6f; // Scale of billboard, shrinks near edges
        float y = (Math.min(1, sqrtsdf * 1.8f) * 0.05f) - 0.1f;
        a *= Math.min(1, sdf * 5); // Fade the billboard out near edges

        double angle = MathHelper.atan2(x, z);

        mat.translate(x, y, z);
        mat.scale(scale, scale, scale);
        mat.multiply(RotationAxis.POSITIVE_Y.rotation((float) angle));

        final var model = mat.peek().getPositionMatrix();

        buffer.vertex(model, -0.5f, 1, 0).color(r, g, b, a).texture(u + us, v - vs).next();
        buffer.vertex(model, 0.5f, 1, 0).color(r, g, b, a).texture(u - us, v - vs).next();
        buffer.vertex(model, 0.5f, 0, 0).color(r, g, b, a).texture(u - us, v + vs).next();
        buffer.vertex(model, -0.5f, 0, 0).color(r, g, b, a).texture(u + us, v + vs).next();
    }

    // For surface decor
    public void buildFancyMiniBillboard(MatrixStack mat, VertexConsumer buffer, float x, float z, float us, float vh, float u, float v, float r, float g, float b, float a) {
        float sdf = Math.max(0, 1 - MathHelper.sqrt(x * x + z * z)); // How inside the sun circle is this billboard (signed distance field)

        float scale = 0.3f + Math.min(1, MathHelper.sqrt(sdf) * 1.8f) * 0.6f; // Shrinks near edges
        a *= Math.min(1, sdf * 5); // Fade out near edges

        double angle = MathHelper.atan2(x, z);

        mat.translate(x, -0.025f, z);
        mat.scale(scale, scale, scale);
        mat.multiply(RotationAxis.POSITIVE_Y.rotation((float) angle));

        final var model = mat.peek().getPositionMatrix();

        buffer.vertex(model, -0.5f, 0.5f, 0).color(r, g, b, 0).texture(u - us, v).next();
        buffer.vertex(model, -0.25f, 0.5f, 0).color(r, g, b, a).texture(u - us * 0.5f, v).next();
        buffer.vertex(model, -0.25f, 0, 0).color(r, g, b, a).texture(u - us * 0.5f, v + vh).next();
        buffer.vertex(model, -0.5f, 0, 0).color(r, g, b, 0).texture(u - us, v + vh).next();

        buffer.vertex(model, -0.25f, 0.5f, 0).color(r, g, b, a).texture(u - us * 0.5f, v).next();
        buffer.vertex(model, 0.25f, 0.5f, 0).color(r, g, b, a).texture(u + us * 0.5f, v).next();
        buffer.vertex(model, 0.25f, 0, 0).color(r, g, b, a).texture(u + us * 0.5f, v + vh).next();
        buffer.vertex(model, -0.25f, 0, 0).color(r, g, b, a).texture(u - us * 0.5f, v + vh).next();

        buffer.vertex(model, 0.25f, 0.5f, 0).color(r, g, b, a).texture(u + us * 0.5f, v).next();
        buffer.vertex(model, 0.5f, 0.5f, 0).color(r, g, b, 0).texture(u + us, v).next();
        buffer.vertex(model, 0.5f, 0, 0).color(r, g, b, 0).texture(u + us, v + vh).next();
        buffer.vertex(model, 0.25f, 0, 0).color(r, g, b, a).texture(u + us * 0.5f, v + vh).next();
    }

    // Make a giant quad, for distance fade ring
    public void buildSquare(MatrixStack mat, VertexConsumer buffer, float us, float vs, float u, float v, float r, float g, float b, float a) {
        final var model = mat.peek().getPositionMatrix();

        buffer.vertex(model, -1, 0, 1).color(r, g, b, a).texture(u - us, v + vs).next();
        buffer.vertex(model, 1, 0, 1).color(r, g, b, a).texture(u + us, v + vs).next();
        buffer.vertex(model, 1, 0, -1).color(r, g, b, a).texture(u + us, v - vs).next();
        buffer.vertex(model, -1, 0, -1).color(r, g, b, a).texture(u - us, v - vs).next();
    }

    // Make an octagon out of four quads, for sun plasma
    public void buildOct(MatrixStack mat, VertexConsumer buffer, float us, float vs, float u, float v, float r, float g, float b, float a) {
        final var model = mat.peek().getPositionMatrix();
        final float sin45 = MathHelper.sqrt(2) * 0.5f;

        // Northeast
        buffer.vertex(model, 0, 0, 1).color(r, g, b, a).texture(u, v + vs).next();
        buffer.vertex(model, sin45, 0, sin45).color(r, g, b, a).texture(u + (us * sin45), v + (vs * sin45)).next();
        buffer.vertex(model, 1, 0, 0).color(r, g, b, a).texture(u + us, v).next();
        buffer.vertex(model, 0, 0, 0).color(r, g, b, a).texture(u, v).next();

        // Northwest
        buffer.vertex(model, 0, 0, 0).color(r, g, b, a).texture(u, v).next();
        buffer.vertex(model, -1, 0, 0).color(r, g, b, a).texture(u - us, v).next();
        buffer.vertex(model, -sin45, 0, sin45).color(r, g, b, a).texture(u - (us * sin45), v + (vs * sin45)).next();
        buffer.vertex(model, 0, 0, 1).color(r, g, b, a).texture(u, v + vs).next();

        // Southeast
        buffer.vertex(model, 0, 0, 0).color(r, g, b, a).texture(u, v).next();
        buffer.vertex(model, 1, 0, 0).color(r, g, b, a).texture(u + us, v).next();
        buffer.vertex(model, sin45, 0, -sin45).color(r, g, b, a).texture(u + (us * sin45), v - (vs * sin45)).next();
        buffer.vertex(model, 0, 0, -1).color(r, g, b, a).texture(u, v - vs).next();

        // Southwest
        buffer.vertex(model, 0, 0, -1).color(r, g, b, a).texture(u, v - vs).next();
        buffer.vertex(model, -sin45, 0, -sin45).color(r, g, b, a).texture(u - (us * sin45), v - (vs * sin45)).next();
        buffer.vertex(model, -1, 0, 0).color(r, g, b, a).texture(u - us, v).next();
        buffer.vertex(model, 0, 0, 0).color(r, g, b, a).texture(u, v).next();
    }

    public static class SolarFlare {
        public final int type; // Index of SOLAR_FLARE_UVS
        public final int creationTime;
        public float x;

        public SolarFlare(int type, int creationTime, float x) {
            this.type = type;
            this.creationTime = creationTime;
            this.x = x;
        }

        public void render(TheSunSky sunSky, MatrixStack mat, VertexConsumer buffer, float time) {
            final float[] uv = SOLAR_FLARE_UVS[type];
            sunSky.buildBillboard(mat, buffer, x, z(sunSky, time), U_SCALE * 0.25f, V_SCALE * 0.25f, uv[0], uv[1], 1, 1, 1, 1);
        }

        public float z(TheSunSky sky, float time) {
            return 1 - ((time - creationTime) * sky.globalSpeed * 2);
        }
    }

    public static class SurfaceDecor {
        public final boolean largeType;
        public final float u;
        public final int creationTime;
        public float x;

        public SurfaceDecor(boolean largeType, float u, int creationTime, float x) {
            this.largeType = largeType;
            this.u = u;
            this.creationTime = creationTime;
            this.x = x;
        }

        public void render(TheSunSky sunSky, MatrixStack mat, VertexConsumer buffer, float time) {
            sunSky.buildFancyMiniBillboard(mat, buffer, x, z(sunSky, time), U_SCALE * 0.25f, V_SCALE * 0.25f, 0.85f + (0.1f * u), largeType ? 0.75f : 0.5f, 1, 1, 1, 1);
        }

        public float z(TheSunSky sky, float time) {
            return 1 - ((time - creationTime) * sky.globalSpeed * 2);
        }
    }

    public static class Effects extends DimensionEffects {
        public Effects() {
            super(Float.NaN, false, SkyType.NORMAL, false, true);
        }

        @Override
        public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
            return color;
        }

        @Override
        public boolean useThickFog(int camX, int camY) {
            return false;
        }
    }
}
