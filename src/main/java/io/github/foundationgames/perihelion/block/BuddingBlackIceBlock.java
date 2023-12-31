package io.github.foundationgames.perihelion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class BuddingBlackIceBlock extends Block {
    public static final MapCodec<BuddingBlackIceBlock> CODEC = createCodec(BuddingBlackIceBlock::new);
    public static final int GROW_CHANCE = 3;

    public MapCodec<BuddingBlackIceBlock> getCodec() {
        return CODEC;
    }

    public BuddingBlackIceBlock(Settings settings) {
        super(settings);
    }

    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(GROW_CHANCE) == 0) {
            var crystalFacing = Direction.values()[random.nextInt(Direction.values().length)];
            var crystalPos = pos.offset(crystalFacing);
            var toGrowState = world.getBlockState(crystalPos);
            Block replaceWith = null;

            if (toGrowState.isAir()) {
                replaceWith = PerihelionBlocks.SMALL_SILICON_CRYSTAL;
            } else if (toGrowState.isOf(PerihelionBlocks.SMALL_SILICON_CRYSTAL) && toGrowState.get(AmethystClusterBlock.FACING) == crystalFacing) {
                replaceWith = PerihelionBlocks.LARGE_SILICON_CRYSTAL;
            }

            if (replaceWith != null) {
                var replaceState = replaceWith.getDefaultState()
                        .with(AmethystClusterBlock.FACING, crystalFacing);
                world.setBlockState(crystalPos, replaceState);
            }
        }
    }
}
