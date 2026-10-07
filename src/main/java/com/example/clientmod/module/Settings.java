package com.example.clientmod.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** User-adjustable values. Edited in the in-game Settings screen and saved with the modules. */
public final class Settings {
    public static final String[] PRESET_NAMES = {
            "Diamonds", "Valuable Ores", "All Ores", "Chests & Spawners", "Custom (config file)"
    };

    /** How far (in blocks) the block highlighter scans around you. */
    public static int highlightRange = 20;
    /** Opacity of the highlight boxes, in percent. */
    public static int highlightOpacity = 40;
    /** Index into PRESET_NAMES. */
    public static int highlightPreset = 1;
    /** Armor/item HUD on the right side of the screen (false = left). */
    public static boolean hudRight = true;
    /** Show remaining durability numbers next to armor/tool icons. */
    public static boolean showDurability = true;
    /** Block ids used by the "Custom" preset, comma separated. Edit in config/myclientmod.properties. */
    public static String customBlocks = "minecraft:diamond_ore,minecraft:deepslate_diamond_ore";

    private static Set<Block>[] presetCache;
    private static Set<Block> customCache;

    private Settings() {}

    public static void load(Properties props) {
        highlightRange = readInt(props, "setting.highlightRange", highlightRange, 8, 40);
        highlightOpacity = readInt(props, "setting.highlightOpacity", highlightOpacity, 10, 80);
        highlightPreset = readInt(props, "setting.highlightPreset", highlightPreset, 0, PRESET_NAMES.length - 1);
        hudRight = Boolean.parseBoolean(props.getProperty("setting.hudRight", Boolean.toString(hudRight)));
        showDurability = Boolean.parseBoolean(props.getProperty("setting.showDurability", Boolean.toString(showDurability)));
        customBlocks = props.getProperty("setting.customBlocks", customBlocks);
        customCache = null;
    }

    public static void save(Properties props) {
        props.setProperty("setting.highlightRange", Integer.toString(highlightRange));
        props.setProperty("setting.highlightOpacity", Integer.toString(highlightOpacity));
        props.setProperty("setting.highlightPreset", Integer.toString(highlightPreset));
        props.setProperty("setting.hudRight", Boolean.toString(hudRight));
        props.setProperty("setting.showDurability", Boolean.toString(showDurability));
        props.setProperty("setting.customBlocks", customBlocks);
    }

    private static int readInt(Properties props, String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(props.getProperty(key, Integer.toString(fallback)).trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** The set of blocks the highlighter should look for, based on the chosen preset. */
    @SuppressWarnings("unchecked")
    public static Set<Block> targets() {
        if (highlightPreset == 4) {
            if (customCache == null) {
                customCache = parseCustom(customBlocks);
            }
            return customCache;
        }
        if (presetCache == null) {
            presetCache = (Set<Block>[]) new Set[4];
            presetCache[0] = Set.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
            presetCache[1] = Set.of(
                    Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                    Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.ANCIENT_DEBRIS,
                    Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
            presetCache[2] = Set.of(
                    Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                    Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                    Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                    Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
                    Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
                    Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                    Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                    Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS);
            presetCache[3] = Set.of(
                    Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL,
                    Blocks.ENDER_CHEST, Blocks.SPAWNER);
        }
        return presetCache[Math.max(0, Math.min(3, highlightPreset))];
    }

    private static Set<Block> parseCustom(String text) {
        Set<Block> result = new HashSet<>();
        for (String part : text.split(",")) {
            Identifier id = Identifier.tryParse(part.trim());
            if (id != null && Registries.BLOCK.containsId(id)) {
                result.add(Registries.BLOCK.get(id));
            }
        }
        return result;
    }
}
