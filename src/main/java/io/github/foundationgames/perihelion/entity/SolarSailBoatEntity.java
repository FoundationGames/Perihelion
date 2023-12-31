package io.github.foundationgames.perihelion.entity;

import io.github.foundationgames.perihelion.Perihelion;
import io.github.foundationgames.perihelion.block.SolarBeamBlock;
import io.github.foundationgames.perihelion.item.PerihelionItems;
import io.github.foundationgames.perihelion.world.SunInverseHeightmap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SolarSailBoatEntity extends BoatEntity {
    public static final double SOLAR_WIND_ACCEL = 0.107f;
    public static final double MAX_SOLAR_WIND_VEL = 0.27f;
    public static final double MAX_FALL_VEL = 0.46f;
    public static final int PLASMA_EFFECT_TICKS = 5;
    public static final int BEAM_SEARCH_DIST = 16;

    private boolean pressingDescend = false;
    private boolean plasma = false;
    private Vec3d solarBeam = Vec3d.ZERO;
    private int radiationCheckTimer = 0;
    private int panelGlowTimer = 0;

    private double deltaY = 0;

    private Vec3d solarWindVel = Vec3d.ZERO;

    public SolarSailBoatEntity(EntityType<SolarSailBoatEntity> type, World world) {
        super(type, world);
    }

    public SolarSailBoatEntity(World world, double x, double y, double z) {
        this(Perihelion.SOLAR_SAIL_BOAT_ENTITY, world);
        this.setPosition(x, y, z);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
    }

    @Override
    public void tick() {
        deltaY = -getY();

        super.tick();

        radiationCheckTimer++;

        if (!(this.getControllingPassenger() instanceof PlayerEntity)) {
            this.pressingDescend = false;
        }

        if (radiationCheckTimer >= 3) {
            radiationCheckTimer = 0;

            var world = getEntityWorld();
            var pos = getBlockPos();
            var height = SunInverseHeightmap.get(world);

            if (height != null) {
                plasma = pos.getY() < height.sample(world, pos.getX(), pos.getZ());
                this.solarBeam = plasma ? new Vec3d(0, 1, 0) : Vec3d.ZERO;
            } else {
                plasma = false;
                this.solarBeam = Vec3d.ZERO;
            }

            searchForBeams();
        }

        boolean panelGlow = plasma || solarBeam.lengthSquared() > 0.01;

        if (panelGlow) {
            if (panelGlowTimer < PLASMA_EFFECT_TICKS) {
                panelGlowTimer++;
            }
        } else {
            if (panelGlowTimer > 0) {
                panelGlowTimer--;
            }
        }

        deltaY += getY();
    }

    public double getDeltaY() {
        return deltaY;
    }

    public float getPanelGlow(float tickDelta) {
        boolean panelGlow = plasma || solarBeam.lengthSquared() > 0.01;
        return MathHelper.clamp((panelGlowTimer + (panelGlow ? tickDelta : -tickDelta)) / PLASMA_EFFECT_TICKS, 0, 1);
    }

    @Override
    public void setVariant(Type type) {
        super.setVariant(Type.OAK); // No variants
    }

    public void updateDescendInput(boolean descending) {
        this.pressingDescend = descending;
    }

    @Override
    public Item asItem() {
        return PerihelionItems.SOLAR_SAIL_BOAT;
    }

    @Override
    protected void updateVelocity() {
        super.updateVelocity();

        var vel = getVelocity();
        var world = getEntityWorld();

        var vx = vel.x;
        var vy = vel.y;
        var vz = vel.z;

        if (!pressingDescend) {
            final double accel = SOLAR_WIND_ACCEL;
            final double yAccel = solarBeam.getY() > 0 ? accel * MathHelper.clamp(world.getHeight() - getY(), 0, 10) * 0.1 : accel;

            final double maxV = MAX_SOLAR_WIND_VEL;

            if ((solarBeam.getX() < 0 && vx > -maxV) || (solarBeam.getX() > 0 && vx < maxV)) {
                vx = MathHelper.clamp(vx + accel * solarBeam.getX(), -maxV, maxV);
            }
            if ((solarBeam.getY() < 0 && vy > -maxV) || (solarBeam.getY() > 0 && vy < maxV)) {
                vy = MathHelper.clamp(vy + yAccel * solarBeam.getY(), -maxV, maxV);
            }
            if ((solarBeam.getZ() < 0 && vz > -maxV) || (solarBeam.getZ() > 0 && vz < maxV)) {
                vz = MathHelper.clamp(vz + accel * solarBeam.getZ(), -maxV, maxV);
            }
        }

        setVelocity(vx,
                (world.getRegistryKey() == Perihelion.THE_SUN_DIMENSION ? Math.max(-MAX_FALL_VEL, vy) : Math.max(-1.2 * MAX_FALL_VEL, vy)),
                vz);
    }

    protected void searchForBeams() {
        var world = getEntityWorld();
        var pos = new BlockPos.Mutable();

        for (var dir : Direction.values()) {
            var opp = dir.getOpposite();
            pos.set(getBlockPos());

            for (int i = 0; i < BEAM_SEARCH_DIST; i++) {
                pos.move(dir);
                var state = world.getBlockState(pos);

                if (state.getBlock() instanceof SolarBeamBlock && state.get(Properties.POWERED) && state.get(Properties.FACING) == opp) {
                    var push = opp.getVector();

                    this.solarBeam = this.solarBeam.add(push.getX(), push.getY(), push.getZ());
                    break;
                }

                if (state.isSideSolidFullSquare(world, pos, dir) || state.isSideSolidFullSquare(world, pos, opp)) {
                    break;
                }
            }
        }
    }
}
