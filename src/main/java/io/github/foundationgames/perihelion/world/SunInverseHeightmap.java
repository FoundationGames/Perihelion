package io.github.foundationgames.perihelion.world;

import io.github.foundationgames.perihelion.Perihelion;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SunInverseHeightmap {
    public static final Map<RegistryKey<World>, SunInverseHeightmap> SERVER_HEIGHTMAPS = new HashMap<>();
    public static SunInverseHeightmap clientHeightmap = null;

    private final Map<ChunkPos, ChunkEntry> heightmaps = new HashMap<>();

    public static void serverStart(MinecraftServer server) {
        SERVER_HEIGHTMAPS.put(Perihelion.THE_SUN_DIMENSION, new SunInverseHeightmap());
    }

    public static @Nullable SunInverseHeightmap getServer(ServerWorld world) {
        return SERVER_HEIGHTMAPS.get(world.getRegistryKey());
    }

    public static @Nullable SunInverseHeightmap getClient(World world) {
        if (world.getRegistryKey() == Perihelion.THE_SUN_DIMENSION) {
            if (clientHeightmap == null) {
                clientHeightmap = new SunInverseHeightmap();
            }

            return clientHeightmap;
        }

        return null;
    }

    public static @Nullable SunInverseHeightmap get(World world) {
        if (world.isClient()) {
            return getClient(world);
        } else if (world instanceof ServerWorld sWorld) {
            return getServer(sWorld);
        }

        return null;
    }

    /*
    public static SunInverseHeightmap readNbt(NbtCompound nbt) {
        var state = new SunInverseHeightmap();

        var heightmapsNbt = nbt.getCompound("heightmaps");
        for (var key : heightmapsNbt.getKeys()) {
            var pair = key.split(",");

            if (pair.length != 2) {
                continue;
            }

            try {
                int x = Integer.parseInt(pair[0]);
                int z = Integer.parseInt(pair[1]);

                var chunkPos = new ChunkPos(x, z);
                var entry = ChunkEntry.readNbt(heightmapsNbt.getCompound(key));

                state.heightmaps.put(chunkPos, entry);
            } catch (NumberFormatException ex) {
                Perihelion.LOG.error("Bad chunk position in sun heightmap", ex);
            }
        }

        return state;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        var heightmapsNbt = new NbtCompound();

        for (var pos : heightmaps.keySet()) {
            var posKey = pos.x + "," + pos.z;

            var entry = heightmaps.get(pos);
            var entryNbt = new NbtCompound();

            entry.writeNbt(entryNbt);
            heightmapsNbt.put(posKey, entryNbt);
        }

        nbt.put("heightmaps", heightmapsNbt);

        return nbt;
    }

     */

    public int sample(World world, int x, int z) {
        var chunkPos = new ChunkPos(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
        var chunk = world.getChunk(chunkPos.x, chunkPos.z);
        ChunkEntry entry;

        if (!heightmaps.containsKey(chunkPos)) {
            entry = populate(chunk);
        } else {
            entry = heightmaps.get(chunkPos);
            if (entry.bottomY != world.getBottomY() || entry.topY != world.getTopY()) {
                heightmaps.remove(chunkPos);

                entry = populate(chunk);
            }
        }

        return entry.get(ChunkSectionPos.getLocalCoord(x), ChunkSectionPos.getLocalCoord(z));
    }

    public ChunkEntry populate(Chunk chunk) {
        final var chunkPos = chunk.getPos();
        final ChunkEntry entry = heightmaps.computeIfAbsent(chunkPos, p -> new ChunkEntry(chunk));

        final var pos = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y <= chunk.getTopY(); y++) {
                    pos.set(x, y, z);

                    final var state = chunk.getBlockState(pos);

                    if (!state.isReplaceable()) {
                        entry.set(x, z, y);

                        break;
                    }
                }
            }
        }

        return entry;
    }

    public void update(Chunk chunk, int x, int y, int z, BlockState newState) {
        final var chunkPos = chunk.getPos();
        final ChunkEntry entry = heightmaps.computeIfAbsent(chunkPos, p -> new ChunkEntry(chunk));

        x = ChunkSectionPos.getLocalCoord(x);
        z = ChunkSectionPos.getLocalCoord(z);

        int currentY = entry.get(x, z);

        if (!newState.isReplaceable() && y < currentY) {
            entry.set(x, z, y);
        } else if (newState.isReplaceable() && y == currentY) {
            final var pos = new BlockPos.Mutable();
            boolean foundFloor = false;

            for (int ny = chunk.getBottomY(); ny <= chunk.getTopY(); ny++) {
                pos.set(x, ny, z);

                final var state = chunk.getBlockState(pos);

                if (!state.isReplaceable()) {
                    entry.set(x, z, ny);
                    foundFloor = true;

                    break;
                }
            }

            if (!foundFloor) {
                entry.set(x, z, chunk.getTopY());
            }
        }
    }



    public void dropChunk(Chunk chunk) {
        this.heightmaps.remove(chunk.getPos());
    }

    public static class ChunkEntry {
        public final PaletteStorage heightmap;
        public final int bottomY;
        public final int topY;

        private ChunkEntry(int bottomY, int topY, int height) {
            this.bottomY = bottomY;
            this.topY = topY;
            this.heightmap = new PackedIntegerArray(MathHelper.ceilLog2(height + 1), 256); // 16x16

            for (int i = 0; i < 256; i++) {
                this.heightmap.set(i, height);
            }
        }

        public ChunkEntry(Chunk chunk) {
            this(chunk.getBottomY(), chunk.getTopY(), chunk.getHeight());
        }

        /*
        public static ChunkEntry readNbt(NbtCompound nbt) {
            final int bottomY = nbt.getInt("min_y");
            final int topY = nbt.getInt("max_y");

            if (nbt.getBoolean("empty")) {
                return new ChunkEntry(bottomY, topY, topY - bottomY);
            }

            final var entry = new ChunkEntry(bottomY, topY, topY - bottomY);
            final var data = nbt.getLongArray("heightmap");

            System.arraycopy(data, 0, entry.heightmap.getData(), 0, data.length);

            return entry;
        }

        public void writeNbt(NbtCompound nbt) {
            boolean empty = true;

            search:
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (get(x, z) < topY) {
                        empty = false;

                        break search;
                    }
                }
            }

            nbt.putBoolean("empty", empty);

            if (!empty) {
                nbt.putLongArray("heightmap", heightmap.getData());
                nbt.putInt("min_y", bottomY);
                nbt.putInt("max_y", topY);
            }
        }
         */

        public void set(int x, int z, int y) {
            heightmap.set(x + z * 16, y - bottomY);
        }

        public int get(int x, int z) {
            return heightmap.get(x + z * 16) + bottomY;
        }
    }
}
