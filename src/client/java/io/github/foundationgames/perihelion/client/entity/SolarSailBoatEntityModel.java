package io.github.foundationgames.perihelion.client.entity;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BoatEntityModel;

public class SolarSailBoatEntityModel extends BoatEntityModel {
    public final ModelPart bottomPanel;

    public SolarSailBoatEntityModel(ModelPart root) {
        super(root);

        this.bottomPanel = root.getChild("bottom_panel");
    }
}
