package dev.eren.oneplayersleep.furnace;

import org.bukkit.Material;

import java.util.Map;

final class FuelDurations {
    private FuelDurations() {
    }

    static int ticks(Material material, Map<String, Integer> customValues) {
        String name = material.name();
        Integer custom = customValues.get(name);
        if (custom != null) {
            return custom;
        }
        if (!material.isFuel()) {
            return 0;
        }

        if (name.equals("LAVA_BUCKET")) return 20000;
        if (name.equals("COAL_BLOCK")) return 16000;
        if (name.equals("DRIED_KELP_BLOCK")) return 4000;
        if (name.equals("BLAZE_ROD")) return 2400;
        if (name.equals("COAL") || name.equals("CHARCOAL")) return 1600;
        if (name.endsWith("_CHEST_BOAT") || name.endsWith("_BOAT") || name.endsWith("_RAFT")) return 1200;
        if (name.endsWith("_HANGING_SIGN")) return 800;
        if (name.equals("BAMBOO_BLOCK") || name.equals("STRIPPED_BAMBOO_BLOCK")) return 300;
        if (name.endsWith("_SLAB")) return 150;
        if (name.endsWith("_DOOR") || name.endsWith("_SIGN") || name.endsWith("_WOODEN_SWORD")
                || name.endsWith("_WOODEN_SHOVEL") || name.endsWith("_WOODEN_PICKAXE")
                || name.endsWith("_WOODEN_AXE") || name.endsWith("_WOODEN_HOE")) return 200;
        if (name.endsWith("_CARPET")) return 67;
        if (name.endsWith("_SAPLING") || name.endsWith("_WOOL") || name.endsWith("_BUTTON")
                || name.equals("STICK")
                || name.equals("BOWL") || name.equals("DEAD_BUSH") || name.equals("AZALEA")
                || name.equals("FLOWERING_AZALEA")) return 100;
        if (name.equals("BAMBOO") || name.equals("SCAFFOLDING")) return 50;

        if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM")
                || name.endsWith("_HYPHAE") || name.endsWith("_PLANKS") || name.endsWith("_STAIRS")
                || name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE")
                || name.endsWith("_TRAPDOOR") || name.endsWith("_PRESSURE_PLATE")
                || name.equals("MANGROVE_ROOTS")
                || name.equals("CHEST") || name.equals("TRAPPED_CHEST") || name.equals("BARREL")
                || name.equals("CRAFTING_TABLE") || name.equals("BOOKSHELF") || name.equals("LECTERN")
                || name.equals("COMPOSTER") || name.equals("LOOM") || name.equals("CARTOGRAPHY_TABLE")
                || name.equals("FLETCHING_TABLE") || name.equals("SMITHING_TABLE") || name.equals("JUKEBOX")
                || name.equals("NOTE_BLOCK") || name.equals("BEEHIVE") || name.equals("BEE_NEST")
                || name.equals("LADDER") || name.equals("BOW") || name.equals("CROSSBOW")
                || name.equals("FISHING_ROD")) return 300;

        // Keep unknown future fuels opt-in so a guessed value cannot duplicate or waste items.
        return 0;
    }
}
