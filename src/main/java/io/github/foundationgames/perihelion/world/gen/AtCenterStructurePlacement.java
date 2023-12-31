package io.github.foundationgames.perihelion.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.perihelion.Perihelion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;

import java.util.Optional;

public class AtCenterStructurePlacement extends StructurePlacement {
    public static final Codec<AtCenterStructurePlacement> CODEC = RecordCodecBuilder.create((instance) ->
            buildCodec(instance).apply(instance, AtCenterStructurePlacement::new));

    public AtCenterStructurePlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt, Optional<ExclusionZone> exclusionZone) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
    }

    @Override
    protected boolean isStartChunk(StructurePlacementCalculator calculator, int chunkX, int chunkZ) {
        return chunkX == 0 && chunkZ == 0;
    }

    @Override
    public StructurePlacementType<?> getType() {
        return Perihelion.AT_CENTER_PLACEMENT_TYPE;
    }

    public static <P extends StructurePlacement> StructurePlacementType<P> register(Identifier id, Codec<P> codec) {
        return Registry.register(Registries.STRUCTURE_PLACEMENT, id, () -> codec);
    }
}
