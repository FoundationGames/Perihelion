package io.github.foundationgames.perihelion;

import io.github.foundationgames.perihelion.block.PerihelionBlocks;
import io.github.foundationgames.perihelion.entity.SolarSailBoatEntity;
import io.github.foundationgames.perihelion.item.ItemGroupQueue;
import io.github.foundationgames.perihelion.item.PerihelionItems;
import io.github.foundationgames.perihelion.world.SunInverseHeightmap;
import io.github.foundationgames.perihelion.world.SunSpawnState;
import io.github.foundationgames.perihelion.world.gen.AtCenterStructurePlacement;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Perihelion implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Perihelion");
    public static final String NAMESPACE = "perihelion";

    public static final ItemGroupQueue ITEM_GROUP = new ItemGroupQueue(id(NAMESPACE));

    public static final RegistryKey<World> THE_SUN_DIMENSION = RegistryKey.of(RegistryKeys.WORLD, id("the_sun"));

    public static final PersistentState.Type<SunSpawnState> SUN_SPAWN_STATE = new PersistentState.Type<>(
            SunSpawnState::new, SunSpawnState::readNbt, DataFixTypes.LEVEL);

    public static final SoundEvent NEAR_SUN_ORBIT_AMBIENT_SOUND = Registry.register(Registries.SOUND_EVENT,
            id("ambient.near_sun_orbit.loop"), SoundEvent.of(id("ambient.near_sun_orbit.loop")));
    public static final SoundEvent NEAR_SUN_ORBIT_ADDITION_SOUND = Registry.register(Registries.SOUND_EVENT,
            id("additions.near_sun_orbit.loop"), SoundEvent.of(id("additions.near_sun_orbit.loop")));

    public static final StructurePlacementType<AtCenterStructurePlacement> AT_CENTER_PLACEMENT_TYPE = AtCenterStructurePlacement.register(
            id("at_center"), AtCenterStructurePlacement.CODEC);

    public static final EntityType<SolarSailBoatEntity> SOLAR_SAIL_BOAT_ENTITY = FabricEntityTypeBuilder.<SolarSailBoatEntity>create()
            .entityFactory(SolarSailBoatEntity::new)
            .dimensions(new EntityDimensions(1.375f, 0.5625f, true))
            .trackRangeChunks(10).build();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM_GROUP, ITEM_GROUP.id, FabricItemGroup.builder()
                .icon(PerihelionBlocks.DEEPSLATE_MONITOR.asItem()::getDefaultStack)
                .displayName(ITEM_GROUP.displayName())
                .entries(ITEM_GROUP)
                .build());

        PerihelionBlocks.init();
        PerihelionItems.init();

        Registry.register(Registries.ENTITY_TYPE, id("solar_sail_boat"), SOLAR_SAIL_BOAT_ENTITY);

        ServerLifecycleEvents.SERVER_STARTED.register(SunInverseHeightmap::serverStart);

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            var heightmap = SunInverseHeightmap.getServer(world);

            if (heightmap != null) {
                heightmap.dropChunk(chunk);
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            var heightmap = SunInverseHeightmap.getServer(world);

            if (heightmap != null) {
                heightmap.populate(chunk);
            }
        });
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }
}
