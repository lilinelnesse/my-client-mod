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
            List.of("Cyan", "Red", "Green", "Yellow", "Magenta", "Orange", "White", "Blue", "Pink");
    public static final int[] COLOR_RGB =
            {0x00FFFF, 0xFF3030, 0x30FF30, 0xFFFF30, 0xFF30FF, 0xFFA020, 0xFFFFFF, 0x3060FF, 0xFF2D78};

    /** Order of the columns in the menu. Every module's category must be one of these. */
    public static final List<String> CATEGORIES = List.of("HUD", "PvP", "Render", "Utility");

    /** Accent color of the menu (index into COLOR_RGB). Saved in the main config, not in profiles. */
    public static final int DEFAULT_HUE = 345, DEFAULT_SAT = 91, DEFAULT_VIB = 100;
    public static final IntSetting GUI_HUE = new IntSetting("hue", "Hue", 0, 360, DEFAULT_HUE);
    public static final IntSetting GUI_SAT = new IntSetting("sat", "Saturation", 0, 100, DEFAULT_SAT);
    public static final IntSetting GUI_VIB = new IntSetting("vib", "Vibrance", 0, 100, DEFAULT_VIB);

    /** HSB (hue 0-360, saturation and brightness 0-100) to 0xRRGGBB. */
    public static int hsb(int hue, int sat, int bri) {
        float h = (hue % 360) / 60f;
        float sv = sat / 100f;
        float v = bri / 100f;
        int i = (int) Math.floor(h);
        float f = h - i;
        float p = v * (1 - sv), q = v * (1 - sv * f), t = v * (1 - sv * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    /** The menu's accent color as 0xFFRRGGBB. */
    public static int accentArgb() {
        return 0xFF000000 | hsb(GUI_HUE.get(), GUI_SAT.get(), GUI_VIB.get());
    }

    public static void resetAccent() {
        GUI_HUE.set(DEFAULT_HUE);
        GUI_SAT.set(DEFAULT_SAT);
        GUI_VIB.set(DEFAULT_VIB);
    }

    // --- Coordinates HUD ---
    public static final Module COORDS = new Module("coords", "Coordinates",
            "Shows your X / Y / Z position.", "HUD", true);
    public static final ChoiceSetting COORDS_CORNER =
            COORDS.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));
    public static final IntSetting COORDS_DECIMALS =
            COORDS.add(new IntSetting("decimals", "Decimals", 0, 3, 1));

    // --- FPS counter ---
    public static final Module FPS = new Module("fps", "FPS Counter",
            "Shows your frames per second.", "HUD", true);
    public static final ChoiceSetting FPS_CORNER =
            FPS.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));

    // --- Armor & item HUD ---
    public static final Module ARMOR_HUD = new Module("armorhud", "Armor & Items",
            "Shows your armor and held items with durability.", "HUD", true);
    public static final ChoiceSetting ARMOR_SIDE =
            ARMOR_HUD.add(new ChoiceSetting("side", "Side", SIDES, 0));
    public static final BoolSetting ARMOR_NUMBERS =
            ARMOR_HUD.add(new BoolSetting("numbers", "Durability numbers", true));
    public static final BoolSetting ARMOR_HANDS =
            ARMOR_HUD.add(new BoolSetting("hands", "Show held items", true));

    // --- Block highlighter ---
    public static final Module HIGHLIGHT = new Module("highlight", "Block Highlight",
            "Outlines the blocks you choose near you.", "Render", false);
    public static final StringSetting HL_BLOCKS = HIGHLIGHT.add(new StringSetting("blocks", "Blocks (comma separated)",
            "diamond_ore,deepslate_diamond_ore,ancient_debris"));
    public static final ChoiceSetting HL_COLOR =
            HIGHLIGHT.add(new ChoiceSetting("color", "Color", COLOR_NAMES, 0));
    public static final IntSetting HL_RANGE =
            HIGHLIGHT.add(new IntSetting("range", "Range", 8, 256, 32));
    public static final IntSetting HL_MAX =
            HIGHLIGHT.add(new IntSetting("max", "Max blocks", 50, 2000, 300));
    public static final BoolSetting HL_THROUGH_WALLS =
            HIGHLIGHT.add(new BoolSetting("walls", "See through walls", true));

    // --- Netherite (ancient debris) chunk finder ---
    public static final Module NETHERITE = new Module("netherite", "Netherite Finder",
            "Marks loaded chunks that contain ancient debris.", "Render", false);
    public static final IntSetting NF_RANGE =
            NETHERITE.add(new IntSetting("range", "Range (chunks)", 2, 32, 12));
    public static final IntSetting NF_MIN =
            NETHERITE.add(new IntSetting("min", "Min debris per chunk", 1, 8, 1));
    public static final ChoiceSetting NF_COLOR =
            NETHERITE.add(new ChoiceSetting("color", "Color", COLOR_NAMES, 5));
    public static final BoolSetting NF_INFO =
            NETHERITE.add(new BoolSetting("info", "Show info text", true));
    public static final ChoiceSetting NF_CORNER =
            NETHERITE.add(new ChoiceSetting("corner", "Info corner", CORNERS, 1));

    // --- More HUD info ---
    public static final Module DIRECTION = new Module("direction", "Direction",
            "Shows which way you are facing.", "HUD", false);
    public static final ChoiceSetting DIRECTION_CORNER =
            DIRECTION.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));
    public static final Module SPEED = new Module("speed", "Speed",
            "Shows how fast you are moving in blocks per second.", "HUD", false);
    public static final ChoiceSetting SPEED_CORNER =
            SPEED.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));
    public static final Module CLOCK = new Module("clock", "Clock",
            "Shows the real time of day.", "HUD", false);
    public static final ChoiceSetting CLOCK_CORNER =
            CLOCK.add(new ChoiceSetting("corner", "Corner", CORNERS, 1));
    public static final BoolSetting CLOCK_24H =
            CLOCK.add(new BoolSetting("h24", "24 hour clock", true));

    // --- Projectile path preview ---
    public static final Module PROJECTILES = new Module("projectiles", "Projectiles",
            "Draws where a bow shot or throwable you are holding will land.", "Render", false);
    public static final ChoiceSetting PROJ_COLOR =
            PROJECTILES.add(new ChoiceSetting("color", "Color", COLOR_NAMES, 2));

    // --- Hide screen debuff effects ---
    public static final Module ANTI_DEBUFF = new Module("antidebuff", "AntiDebuff",
            "Hides the nausea wobble and the darkness pulse on your screen.", "Render", false);

    // --- PvP (HUD helpers that PvP clients normally ship) ---
    public static final Module CPS = new Module("cps", "CPS Counter",
            "Shows how many times per second you click.", "PvP", false);
    public static final ChoiceSetting CPS_CORNER =
            CPS.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));
    public static final BoolSetting CPS_BOTH =
            CPS.add(new BoolSetting("both", "Show right click too", true));

    public static final Module KEYSTROKES = new Module("keystrokes", "Keystrokes",
            "Shows which movement keys and mouse buttons you are pressing.", "PvP", false);

    public static final Module POTION_HUD = new Module("potionhud", "Potion Effects",
            "Lists your active potion effects with time left.", "PvP", false);
    public static final ChoiceSetting POTION_CORNER =
            POTION_HUD.add(new ChoiceSetting("corner", "Corner", CORNERS, 1));

    public static final Module PING = new Module("ping", "Ping",
            "Shows your latency to the server.", "PvP", false);
    public static final ChoiceSetting PING_CORNER =
            PING.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));

    // --- Storage ESP ---
    public static final Module STORAGE_ESP = new Module("storageesp", "Storage ESP",
            "Outlines the chests, barrels and other storage you choose.", "Render", false);
    public static final StringSetting SE_BLOCKS = STORAGE_ESP.add(new StringSetting("blocks", "Storage blocks",
            "chest,trapped_chest,barrel,ender_chest"));
    public static final ChoiceSetting SE_COLOR =
            STORAGE_ESP.add(new ChoiceSetting("color", "Color", COLOR_NAMES, 3));
    public static final IntSetting SE_RANGE =
            STORAGE_ESP.add(new IntSetting("range", "Range", 8, 512, 128));
    public static final BoolSetting SE_THROUGH_WALLS =
            STORAGE_ESP.add(new BoolSetting("walls", "See through walls", true));

    // --- Freecam ---
    public static final Module FREECAM = new Module("freecam", "Freecam",
            "Fly the camera around while your player stays put.", "Render", false);
    public static final IntSetting FC_SPEED =
            FREECAM.add(new IntSetting("speed", "Speed", 1, 50, 10));
    public static final IntSetting FC_SPRINT =
            FREECAM.add(new IntSetting("sprint", "Sprint multiplier (Ctrl)", 1, 6, 2));

    // --- Health display ---
    public static final Module HEALTH = new Module("health", "Health",
            "Shows your health (and absorption) as a number.", "Render", false);
    public static final ChoiceSetting HEALTH_CORNER =
            HEALTH.add(new ChoiceSetting("corner", "Corner", CORNERS, 0));

    // --- Item ESP ---
    public static final Module ITEM_ESP = new Module("itemesp", "ItemESP",
            "Outlines dropped items near you.", "Render", false);
    public static final ChoiceSetting IE_COLOR =
            ITEM_ESP.add(new ChoiceSetting("color", "Color", COLOR_NAMES, 6));
    public static final IntSetting IE_RANGE =
            ITEM_ESP.add(new IntSetting("range", "Range", 4, 128, 32));
    public static final BoolSetting IE_THROUGH_WALLS =
            ITEM_ESP.add(new BoolSetting("walls", "See through walls", true));

    // --- Fullbright ---
    public static final Module FULLBRIGHT = new Module("fullbright", "Fullbright",
            "Makes everything fully bright, even in caves and at night.", "Render", false);

    // --- Panic: switches every module off at once ---
    public static final Module PANIC = new Module("panic", "Panic",
            "Click to switch every module off at once (also bound to the End key).", "Utility", false);

    // --- Auto sprint ---
    public static final Module AUTO_SPRINT = new Module("autosprint", "Auto Sprint",
            "Sprints automatically while you hold forward.", "Utility", false);

    // --- Auto mine ---
    public static final Module AUTO_MINE = new Module("automine", "Auto Mine",
            "Walks to and mines the Block Highlight blocks (turn Block Highlight on too).", "Utility", false);
    public static final IntSetting AM_MIN_HEALTH =
            AUTO_MINE.add(new IntSetting("minhealth", "Stop below health", 0, 19, 6));
    public static final BoolSetting AM_WALK =
            AUTO_MINE.add(new BoolSetting("walk", "Walk to blocks", true));
    public static final BoolSetting AM_LOOK =
            AUTO_MINE.add(new BoolSetting("look", "Look at blocks", true));

    public static final List<Module> ALL = List.of(
            COORDS, FPS, ARMOR_HUD, DIRECTION, SPEED, CLOCK, CPS, KEYSTROKES, POTION_HUD, PING, HIGHLIGHT, NETHERITE, STORAGE_ESP, ITEM_ESP, PROJECTILES, ANTI_DEBUFF, HEALTH, FREECAM, FULLBRIGHT, AUTO_SPRINT, AUTO_MINE, PANIC);

    private ModuleManager() {}

    public static List<Module> inCategory(String category) {
        return ALL.stream().filter(m -> m.getCategory().equals(category)).toList();
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("myclientmod.properties");
    }

    /** Current on/off state and settings of every module, as text key/values (used by profiles too). */
    public static Properties snapshot() {
        Properties props = new Properties();
        for (Module module : ALL) {
            props.setProperty(module.getId(), Boolean.toString(module.isEnabled()));
            for (Setting setting : module.getSettings()) {
                props.setProperty(module.getId() + "." + setting.getId(), setting.serialize());
            }
        }
        return props;
    }

    /** Applies a snapshot. Anything missing from it keeps its current value. */
    public static void apply(Properties props) {
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
        apply(props);
        String hue = props.getProperty("gui.hue");
        if (hue != null) GUI_HUE.deserialize(hue);
        String sat = props.getProperty("gui.sat");
        if (sat != null) GUI_SAT.deserialize(sat);
        String vib = props.getProperty("gui.vib");
        if (vib != null) GUI_VIB.deserialize(vib);
        ProfileManager.setActive(props.getProperty("profile.active", ProfileManager.DEFAULT));
    }

    /** Saves the main config and keeps the active profile in sync with it. */
    public static void save() {
        Properties props = snapshot();
        props.setProperty("profile.active", ProfileManager.getActive());
        props.setProperty("gui.hue", GUI_HUE.serialize());
        props.setProperty("gui.sat", GUI_SAT.serialize());
        props.setProperty("gui.vib", GUI_VIB.serialize());
        try (OutputStream out = Files.newOutputStream(configFile())) {
            props.store(out, "My Client Mod settings");
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not save config", e);
        }
        ProfileManager.writeActive();
    }
}
