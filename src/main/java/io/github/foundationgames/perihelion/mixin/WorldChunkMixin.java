package io.github.foundationgames.perihelion.mixin;

import io.github.foundationgames.perihelion.world.SunInverseHeightmap;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
    @Shadow @Final World world;

    @Inject(method = "setBlockState", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/chunk/ChunkSection;isEmpty()Z", ordinal = 1, shift = At.Shift.BEFORE))
    private void perihelion$updateSunHeightmaps(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        var heightmap = SunInverseHeightmap.get(world);

        if (heightmap != null) {
            heightmap.update((WorldChunk)(Object)this, pos.getX(), pos.getY(), pos.getZ(), state);
        }
    }
}
