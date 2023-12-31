package io.github.foundationgames.perihelion.world;

import io.github.foundationgames.perihelion.Perihelion;
import io.github.foundationgames.perihelion.block.SingularityProjectorBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

public class SunSpawnState extends PersistentState {
    public BlockPos spawnPos = new BlockPos(0, 67, 0);

    public static @Nullable SunSpawnState getOrCreate(ServerWorld world) {
        if (world.getRegistryKey() != Perihelion.THE_SUN_DIMENSION) {
            return null;
        }

        final var ps = world.getPersistentStateManager();
        var state = ps.get(Perihelion.SUN_SPAWN_STATE, "sun_spawn");

        if (state == null) {
            state = ps.getOrCreate(Perihelion.SUN_SPAWN_STATE, "sun_spawn");

            var pos = new BlockPos.Mutable();
            pos.set(state.spawnPos);

            // Search chunk -1, 0 for portal
            int y;
            for (int h = 0; h < world.getHeight() / 2; h++) {
                y = ((pos.getY() + world.getBottomY() + h) % world.getHeight()) - world.getBottomY();
                for (int x = -15; x < 16; x++) {
                    for (int z = -15; z < 16; z++) {
                        pos.set(x, y, z);

                        if (world.getBlockState(pos).getBlock() instanceof SingularityProjectorBlock) {
                            state.spawnPos = pos.toImmutable();
                            return state;
                        }
                    }
                }
            }
        }

        return state;
    }

    public static SunSpawnState readNbt(NbtCompound nbt) {
        var state = new SunSpawnState();
        state.spawnPos = BlockPos.fromLong(nbt.getLong("spawn_pos"));

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("spawn_pos", spawnPos.asLong());
        return nbt;
    }
}
