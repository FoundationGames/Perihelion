package io.github.foundationgames.perihelion.entity;

public interface RadiationBurningEntity {
    int RADIATION_BURN_CHECK_TIME_TICKS = 5;
    float MIN_RADIATION_DAMAGE = 1.5f;
    float MAX_RADIATION_DAMAGE = 6f;
    float RADIATION_DAMAGE_RATIO = 0.2f;

    boolean isRadiationBurning();
}
