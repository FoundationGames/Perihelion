package io.github.foundationgames.perihelion.mixin;

import io.github.foundationgames.perihelion.entity.RadiationBurningEntity;
import io.github.foundationgames.perihelion.entity.SolarSailBoatEntity;
import io.github.foundationgames.perihelion.world.SunInverseHeightmap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements RadiationBurningEntity {
    @Shadow public abstract boolean damage(DamageSource source, float amount);
    @Shadow public abstract float getHealth();

    private int perihelion$radiationCheckTimer = 0;
    private int perihelion$radiationDamageTicks = 0;
    private boolean perihelion$radiationBurning = false;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void perihelion$updateRadiationState(CallbackInfo ci) {
        if (getVehicle() instanceof SolarSailBoatEntity || ((Object) this instanceof PlayerEntity player && player.isCreative())) {
            perihelion$radiationBurning = false;
            perihelion$radiationDamageTicks = 0;
            perihelion$radiationCheckTimer = 0;

            return;
        }

        var world = getEntityWorld();
        var heightmap = SunInverseHeightmap.get(world);

        if (heightmap != null) {
            perihelion$radiationCheckTimer++;

            if (perihelion$radiationCheckTimer >= RADIATION_BURN_CHECK_TIME_TICKS) {
                perihelion$radiationCheckTimer = 0;

                var pos = getBlockPos();
                var height = SunInverseHeightmap.get(world);

                perihelion$radiationBurning = pos.getY() < height.sample(world, pos.getX(), pos.getZ());
            }
        } else {
            perihelion$radiationBurning = false;
            perihelion$radiationCheckTimer = 0;
        }

        if (perihelion$radiationBurning && !world.isClient()) {
            perihelion$radiationDamageTicks--;

            if (perihelion$radiationDamageTicks <= 0) {
                perihelion$radiationDamageTicks = 6;

                this.damage(world.getDamageSources().inFire(),
                        MathHelper.clamp(getHealth() * RADIATION_DAMAGE_RATIO, MIN_RADIATION_DAMAGE, MAX_RADIATION_DAMAGE));
            }
        } else {
            perihelion$radiationDamageTicks = 0;
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void perihelion$writeRadiationNbt(NbtCompound d, CallbackInfo ci) {
        var nbt = d.contains("perihelion") ? d.getCompound("perihelion") : new NbtCompound();
        nbt.putBoolean("RadiationBurning", perihelion$radiationBurning);
        nbt.putInt("RadiationHurtTime", perihelion$radiationDamageTicks);
        d.put("perihelion", nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void perihelion$readRadiationNbt(NbtCompound d, CallbackInfo ci) {
        var nbt = d.getCompound("perihelion");
        perihelion$radiationBurning = nbt.getBoolean("RadiationBurning");
        perihelion$radiationDamageTicks = nbt.getInt("RadiationHurtTime");
    }

    @Override
    public boolean isRadiationBurning() {
        return perihelion$radiationBurning;
    }
}
