package io.github.foundationgames.perihelion.item;

import io.github.foundationgames.perihelion.Perihelion;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class PerihelionItems {
    public static final Item SILICON_CRYSTAL_SHARD = reg("silicon_crystal_shard", new Item(new Item.Settings()));
    public static final Item SOLAR_SAIL_BOAT = reg("solar_sail_boat", new SolarSailBoatItem(new Item.Settings().maxCount(1)));

    public static void init() {}

    public static <T extends Item> T reg(String name, T item) {
        var entry = Registry.register(Registries.ITEM, Perihelion.id(name), item);
        Perihelion.ITEM_GROUP.queue(entry);
        return entry;
    }
}
