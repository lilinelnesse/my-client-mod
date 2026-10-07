package com.example.clientmod.module;

import com.example.clientmod.MyClientMod;
import com.example.clientmod.module.setting.BoolSetting;
import com.example.clientmod.module.setting.ChoiceSetting;
import com.example.clientmod.module.setting.IntSetting;
import com.example.clientmod.module.setting.Setting;
import com.example.clientmod.module.setting.StringSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Holds all modules and their settings, and saves/loads them. Add new modules here. */
public final class ModuleManager {
    public static final List<String> CORNERS = List.of("Top Left", "Top Right", "Bottom Right");
    public static final List<String> SIDES = List.of("Left", "Right");
    public static final List<String> COLOR_NAMES =
            List.of("Cyan", "Red", "Green", "Yellow", "Magenta", "Orange", "White", "Blue");
    public static final int[] COLOR_RGB =
            {0x00FFFF, 0xFF3030, 0x30FF30, 0xFFFF30, 0xFF30FF, 0xFFA020, 0xFFFFFF, 0x3060FF};

    // --- Coordinates HUD ---
    public static final Module COORDS = new Module("coords", "Coordinates HUD",
            "Shows your X / Y / Z position.", true);
    public static final ChoiceSetting COORDS_CORNER =
            COORDS.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));
    public static final IntSetting COORDS_DECIMALS =
            COORDS.add(new IntSetting("decimals", "Decimals", 0, 3, 1));

    // --- FPS counter ---
    public static final Module FPS = new Module("fps", "FPS Counter",
            "Shows your frames per second.", true);
    public static final ChoiceSetting FPS_CORNER =
            FPS.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));

    // --- Armor & item HUD ---
    public static final Module ARMOR_HUD = new Module("armorhud", "Armor & Item HUD",
            "Shows your armor and held items with durability.", true);
    public static final ChoiceSetting ARMOR_SIDE =
            ARMOR_HUD.add(new ChoiceSetting("side", "Side of screen", SIDES, 0));
    public static final BoolSetting ARMOR_NUMBERS =
            ARMOR_HUD.add(new BoolSetting("numbers", "Durability numbers", true));
    public static final BoolSetting ARMOR_HANDS =
            ARMOR_HUD.add(new BoolSetting("hands", "Show held items", true));

    // --- Block highlighter ---
    public static final Module HIGHLIGHT = new Module("highlight", "Block Highlight",
            "Outlines the blocks you choose near you.", false);
    public static final StringSetting HL_BLOCKS = HIGHLIGHT.add(new StringSetting("blocks", "Blocks (comma separated)",
            "diamond_ore,deepslate_diamond_ore,ancient_debris"));
    public static final ChoiceSetting HL_COLOR =
            HIGHLIGHT.add(new ChoiceSetting("color", "Color", COLOR_NAMES, 0));
    public static final IntSetting HL_RANGE =
            HIGHLIGHT.add(new IntSetting("range", "Range", 8, 32, 16));
    public static final IntSetting HL_MAX =
            HIGHLIGHT.add(new IntSetting("max", "Max blocks", 50, 1000, 300));
    public static final BoolSetting HL_THROUGH_WALLS =
            HIGHLIGHT.add(new BoolSetting("walls", "See through walls", true));

    // --- Fullbright ---
    public static final Module FULLBRIGHT = new Module("fullbright", "Fullbright",
            "Makes everything fully bright, even in caves and at night.", false);

    // --- Auto sprint ---
    public static final Module AUTO_SPRINT = new Module("autosprint", "Auto Sprint",
            "Sprints automatically while you hold forward.", false);

    public static final List<Module> ALL = List.of(
            COORDS, FPS, ARMOR_HUD, HIGHLIGHT, FULLBRIGHT, AUTO_SPRINT);

    private ModuleManager() {}

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("myclientmod.properties");
    }

    public static void load() {
        Path file = configFile();
        if (!Files.exists(file)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not read config", e);
            return;
        }
        for (Module module : ALL) {
            String enabled = props.getProperty(module.getId());
            if (enabled != null) {
                module.setEnabled(Boolean.parseBoolean(enabled));
            }
            for (Setting setting : module.getSettings()) {
                String value = props.getProperty(module.getId() + "." + setting.getId());
                if (value != null) {
                    setting.deserialize(value);
                }
            }
        }
    }

    public static void save() {
        Properties props = new Properties();
        for (Module module : ALL) {
            props.setProperty(module.getId(), Boolean.toString(module.isEnabled()));
            for (Setting setting : module.getSettings()) {
                props.setProperty(module.getId() + "." + setting.getId(), setting.serialize());
            }
        }
        try (OutputStream out = Files.newOutputStream(configFile())) {
            props.store(out, "My Client Mod settings");
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not save config", e);
        }
    }
}
