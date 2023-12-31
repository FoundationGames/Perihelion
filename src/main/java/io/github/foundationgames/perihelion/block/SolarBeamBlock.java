package io.github.foundationgames.perihelion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SolarBeamBlock extends FacingBlock {
    public static final MapCodec<SolarBeamBlock> CODEC = createCodec(SolarBeamBlock::new);

    public static final BooleanProperty POWERED = Properties.POWERED;

    protected SolarBeamBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.UP).with(POWERED, false));
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);

        if (state.get(POWERED)) {
            var rng = world.random;
            var dir = state.get(FACING);
            var vel = new Vec3d(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ()).multiply(0.5f);

            float os = 0.2f;
            float w = 1 - (2 * os);

            for (int i = 0; i < 1 + rng.nextInt(2); i++) {
                var particlePos = switch (dir) {
                    case NORTH -> new Vec3d(
                            os + random.nextFloat() * w,
                            os + random.nextFloat() * w,
                            0
                    );
                    case SOUTH -> new Vec3d(
                            os + random.nextFloat() * w,
                            os + random.nextFloat() * w,
                            1
                    );
                    case EAST -> new Vec3d(
                            1,
                            os + random.nextFloat() * w,
                            os + random.nextFloat() * w
                    );
                    case WEST -> new Vec3d(
                            0,
                            os + random.nextFloat() * w,
                            os + random.nextFloat() * w
                    );
                    case DOWN -> new Vec3d(
                            os + random.nextFloat() * w,
                            0,
                            os + random.nextFloat() * w
                    );
                    default -> new Vec3d(
                            os + random.nextFloat() * w,
                            1,
                            os + random.nextFloat() * w
                    );
                };

                world.addParticle(ParticleTypes.WHITE_SMOKE,
                        pos.getX() + particlePos.x, pos.getY() + particlePos.y, pos.getZ() + particlePos.z,
                        vel.x, vel.y, vel.z);
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);

        builder.add(FACING, POWERED);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        boolean powered = world.isReceivingRedstonePower(pos);

        if (powered != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, powered));
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx)
                .with(FACING, ctx.getPlayerLookDirection().getOpposite())
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(FACING, mirror.apply(state.get(FACING)));
    }

    @Override
    protected MapCodec<? extends FacingBlock> getCodec() {
        return CODEC;
    }
}
