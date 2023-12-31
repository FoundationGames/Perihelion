package io.github.foundationgames.perihelion.client.mixin;

import io.github.foundationgames.perihelion.entity.SolarSailBoatEntity;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Shadow public Input input;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void perihelion$updateSolarSailBoat(CallbackInfo ci) {
        var self = (ClientPlayerEntity) (Object) this;

        if (self.getVehicle() instanceof SolarSailBoatEntity boat) {
            boat.updateDescendInput(input.jumping);
        }
    }
}
