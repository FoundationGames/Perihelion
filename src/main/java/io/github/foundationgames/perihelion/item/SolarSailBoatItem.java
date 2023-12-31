package io.github.foundationgames.perihelion.item;

import io.github.foundationgames.perihelion.entity.SolarSailBoatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.BoatItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class SolarSailBoatItem extends BoatItem {
    public SolarSailBoatItem(Settings settings) {
        super(false, BoatEntity.Type.OAK, settings);
    }

    @Override
    protected BoatEntity createEntity(World world, HitResult hitResult, ItemStack stack, PlayerEntity player) {
        var pos = hitResult.getPos();
        var boat = new SolarSailBoatEntity(world, pos.x, pos.y, pos.z);
        if (world instanceof ServerWorld serverWorld) {
            EntityType.copier(serverWorld, stack, player).accept(boat);
        }

        return boat;
    }
}
