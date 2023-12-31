package io.github.foundationgames.perihelion.block;

import io.github.foundationgames.perihelion.Perihelion;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class PerihelionBlocks {
    public static final SingularityProjectorBlock SINGULARITY_PROJECTOR = reg("singularity_projector", new SingularityProjectorBlock(FabricBlockSettings.copy(Blocks.END_PORTAL_FRAME).luminance(s -> 10).emissiveLighting(PerihelionBlocks::always)));
    public static final Block FROZEN_ASH = reg("frozen_ash", new Block(FabricBlockSettings.copy(Blocks.PACKED_MUD).strength(6, 250)));
    public static final Block BLACK_ICE = reg("black_ice", new Block(FabricBlockSettings.copy(Blocks.COBBLED_DEEPSLATE).slipperiness(0.7f).strength(20, 500)));
    public static final BuddingBlackIceBlock BUDDING_BLACK_ICE = reg("budding_black_ice", new BuddingBlackIceBlock(FabricBlockSettings.copy(BLACK_ICE).ticksRandomly()));
    public static final Block DEEPSLATE_MONITOR = reg("deepslate_monitor", new Block(FabricBlockSettings.copy(Blocks.DEEPSLATE_TILES).luminance(s -> 5)));
    public static final AmethystClusterBlock SMALL_SILICON_CRYSTAL = reg("small_silicon_crystal", new AmethystClusterBlock(3, 4, FabricBlockSettings.copy(Blocks.SMALL_AMETHYST_BUD).emissiveLighting(PerihelionBlocks::always)));
    public static final AmethystClusterBlock LARGE_SILICON_CRYSTAL = reg("large_silicon_crystal", new AmethystClusterBlock(8, 3, FabricBlockSettings.copy(Blocks.SMALL_AMETHYST_BUD).emissiveLighting(PerihelionBlocks::always)));
    public static final SolarBeamBlock SOLAR_BEAM = reg("solar_beam", new SolarBeamBlock(FabricBlockSettings.copy(Blocks.DEEPSLATE_TILES)
            .luminance(s -> s.get(Properties.POWERED) ? 10 : 0).emissiveLighting((s, w, p) -> s.get(Properties.POWERED))));

    public static void init() {}

    private static <T extends Block> T reg(String name, T block) {
        var item = Registry.register(Registries.ITEM, Perihelion.id(name), new BlockItem(block, new Item.Settings()));
        Perihelion.ITEM_GROUP.queue(item);
        return regBlockOnly(name, block);
    }

    private static <T extends Block> T regBlockOnly(String name, T block) {
        return Registry.register(Registries.BLOCK, Perihelion.id(name), block);
    }

    private static boolean always(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }
}
