package io.github.foundationgames.perihelion.mixin;

import io.github.foundationgames.perihelion.Perihelion;
import io.github.foundationgames.perihelion.block.SingularityProjectorBlock;
import io.github.foundationgames.perihelion.entity.SingularityPortalEnteringEntity;
import io.github.foundationgames.perihelion.world.SunSpawnState;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements SingularityPortalEnteringEntity {
    @Shadow public abstract World getEntityWorld();
    @Shadow public abstract BlockPos getBlockPos();

    protected boolean perihelion$inSingularityPortal;
    protected BlockPos perihelion$homeSingularityPortal;

    @Inject(method = "getJumpVelocityMultiplier", at = @At(value = "RETURN"), cancellable = true)
    private void perihelion$halveSunJumpHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof PlayerEntity player && player.isCreative()) {
            return;
        }

        var world = getEntityWorld();

        if (world.getRegistryKey() == Perihelion.THE_SUN_DIMENSION) {
            cir.setReturnValue(cir.getReturnValueF() * MathHelper.SQUARE_ROOT_OF_TWO * 0.5f);
        }
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void perihelion$tickSingularityPortal(CallbackInfo ci) {
        var world = getEntityWorld();

        if (world instanceof ServerWorld sWorld) {
            var state = world.getBlockState(getBlockPos());

            if (state.getBlock() instanceof SingularityProjectorBlock) {
                if (!perihelion$inSingularityPortal) {
                    var srv = sWorld.getServer();
                    var currDim = world.getRegistryKey();
                    var targetDim = Perihelion.THE_SUN_DIMENSION;

                    var targetPos = srv.getWorld(World.OVERWORLD).getSpawnPos().toCenterPos();
                    var targetWorld = srv.getWorld(targetDim);

                    if (currDim == targetDim) {
                        // Back to overworld
                        if (this.perihelion$homeSingularityPortal != null) {
                            targetPos = Vec3d.ofCenter(this.perihelion$homeSingularityPortal);
                        }
                        targetWorld = srv.getWorld(World.OVERWORLD);
                    } else {
                        // To sun
                        if (currDim == World.OVERWORLD) {
                            this.perihelion$homeSingularityPortal = getBlockPos();
                        }

                        var spawn = SunSpawnState.getOrCreate(targetWorld);

                        if (spawn != null) {
                            targetPos = spawn.spawnPos.toCenterPos();
                        } else {
                            targetPos = null;
                        }
                    }

                    if (targetPos != null) {
                        FabricDimensions.teleport((Entity) (Object) this, targetWorld, new TeleportTarget(targetPos, Vec3d.ZERO, 0, 0));
                    }
                }

                perihelion$inSingularityPortal = true;
            } else {
                perihelion$inSingularityPortal = false;
            }
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void perihelion$writeCustomNbt(NbtCompound d, CallbackInfoReturnable<NbtCompound> cir) {
        var nbt = d.contains("perihelion") ? d.getCompound("perihelion") : new NbtCompound();
        nbt.putBoolean("InSingularityPortal", perihelion$inSingularityPortal);

        if (perihelion$homeSingularityPortal != null) {
            nbt.putLong("HomeSingularityPortal", perihelion$homeSingularityPortal.asLong());
        }

        d.put("perihelion", nbt);
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void perihelion$readCustomNbt(NbtCompound d, CallbackInfo ci) {
        var nbt = d.getCompound("perihelion");
        perihelion$inSingularityPortal = nbt.getBoolean("InSingularityPortal");

        if (nbt.contains("HomeSingularityPortal")) {
            perihelion$homeSingularityPortal = BlockPos.fromLong(nbt.getLong("HomeSingularityPortal"));
        } else {
            perihelion$homeSingularityPortal = null;
        }
    }

    @Override
    public boolean inSingularityPortal() {
        return perihelion$inSingularityPortal;
    }

    @Override
    public void setInSingularityPortal(boolean inPortal) {
        perihelion$inSingularityPortal = inPortal;
    }
}
