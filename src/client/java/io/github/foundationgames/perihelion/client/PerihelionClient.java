package io.github.foundationgames.perihelion.client;

import io.github.foundationgames.jsonem.JsonEM;
import io.github.foundationgames.perihelion.Perihelion;
import io.github.foundationgames.perihelion.block.PerihelionBlocks;
import io.github.foundationgames.perihelion.client.entity.SolarSailBoatEntityRenderer;
import io.github.foundationgames.perihelion.client.ui.RadiationBurnHudEffect;
import io.github.foundationgames.perihelion.client.ui.WarningTextHud;
import io.github.foundationgames.perihelion.client.world.TheSunSky;
import io.github.foundationgames.perihelion.world.SunInverseHeightmap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;

public class PerihelionClient implements ClientModInitializer {
    public static final TheSunSky THE_SUN_SKY = new TheSunSky();
    public static final RadiationBurnHudEffect RADIATION_BURN_HUD = new RadiationBurnHudEffect();
    public static final WarningTextHud WARNING_TEXT_HUD = new WarningTextHud();

    @Override
    public void onInitializeClient() {
        JsonEM.registerModelLayer(SolarSailBoatEntityRenderer.MODEL_LAYER);

        ClientTickEvents.START_WORLD_TICK.register(THE_SUN_SKY);
        ClientTickEvents.END_WORLD_TICK.register(RADIATION_BURN_HUD);
        ClientTickEvents.END_WORLD_TICK.register(WARNING_TEXT_HUD);

        DimensionRenderingRegistry.registerDimensionEffects(Perihelion.THE_SUN_DIMENSION.getValue(), new TheSunSky.Effects());

        DimensionRenderingRegistry.registerSkyRenderer(Perihelion.THE_SUN_DIMENSION, THE_SUN_SKY);
        DimensionRenderingRegistry.registerCloudRenderer(Perihelion.THE_SUN_DIMENSION, c -> {});

        setupClientBlocks();

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == MinecraftClient.getInstance().player) {
                createClientSunHeightmap();
            }
        });
        ClientEntityEvents.ENTITY_LOAD.register(RADIATION_BURN_HUD);
        ClientEntityEvents.ENTITY_LOAD.register(WARNING_TEXT_HUD);

        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            var heightmap = SunInverseHeightmap.getClient(world);

            if (heightmap != null) {
                heightmap.dropChunk(chunk);
            }
        });

        ClientChunkEvents.CHUNK_LOAD.register(((world, chunk) -> {
            var heightmap = SunInverseHeightmap.getClient(world);

            if (heightmap != null) {
                heightmap.populate(chunk);
            }
        }));

        HudRenderCallback.EVENT.register(WARNING_TEXT_HUD);

        EntityRendererRegistry.register(Perihelion.SOLAR_SAIL_BOAT_ENTITY, SolarSailBoatEntityRenderer::new);
    }

    public static void createClientSunHeightmap() {
        SunInverseHeightmap.clientHeightmap = new SunInverseHeightmap();
    }

    public static void setupClientBlocks() {
        BlockRenderLayerMap.INSTANCE.putBlock(PerihelionBlocks.SINGULARITY_PROJECTOR, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(PerihelionBlocks.SMALL_SILICON_CRYSTAL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PerihelionBlocks.LARGE_SILICON_CRYSTAL, RenderLayer.getCutout());
    }
}
