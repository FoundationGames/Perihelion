package io.github.foundationgames.perihelion.client.entity;

import com.mojang.datafixers.util.Pair;
import io.github.foundationgames.perihelion.Perihelion;
import io.github.foundationgames.perihelion.entity.SolarSailBoatEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

import java.util.HashMap;

public class SolarSailBoatEntityRenderer extends BoatEntityRenderer {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(Perihelion.id("solar_sail_boat"), "main");

    public SolarSailBoatEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, false);

        var texturedModels = new HashMap<BoatEntity.Type, Pair<Identifier, CompositeEntityModel<BoatEntity>>>();
        texturedModels.putAll(this.texturesAndModels);
        texturedModels.put(BoatEntity.Type.OAK,
                Pair.of(Perihelion.id("textures/entity/solar_sail_boat.png"), new SolarSailBoatEntityModel(ctx.getPart(MODEL_LAYER))));

        this.texturesAndModels = texturedModels;
    }

    @Override
    public void render(BoatEntity boat, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers, int light) {
        matrices.push();
        matrices.translate(0, 0.001, 0);

        super.render(boat, yaw, tickDelta, matrices, buffers, light);

        var pair = texturesAndModels.get(BoatEntity.Type.OAK);
        var model = pair.getSecond();
        var texture = pair.getFirst();

        if (boat instanceof SolarSailBoatEntity solarBoat && model instanceof SolarSailBoatEntityModel boatModel) {
            matrices.push();
            matrices.translate(0, 0.375f, 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 - yaw));

            float dmgTimer = boat.getDamageWobbleTicks() - tickDelta;
            float dmgStrength = Math.max(0, boat.getDamageWobbleStrength() - tickDelta);

            if (dmgTimer > 0.0F) {
                matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(MathHelper.sin(dmgTimer) * dmgTimer * dmgStrength / 10 * boat.getDamageWobbleSide()));
            }

            float bubble = boat.interpolateBubbleWobble(tickDelta);
            if (!MathHelper.approximatelyEquals(bubble, 0)) {
                matrices.multiply(new Quaternionf().setAngleAxis(bubble * 0.017453292f, 1, 0, 1));
            }

            matrices.scale(-1.0F, -1.0F, 1.0F);

            var buffer = buffers.getBuffer(RenderLayer.getBeaconBeam(texture, true));

            float pl = 0.3f + 0.7f * solarBoat.getPanelGlow(tickDelta);

            boatModel.bottomPanel.render(matrices, buffer, light, OverlayTexture.DEFAULT_UV, pl, pl, pl, 1);

            matrices.pop();
        }

        matrices.pop();
    }
}
