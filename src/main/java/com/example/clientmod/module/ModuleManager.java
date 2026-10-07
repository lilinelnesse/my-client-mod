package com.example.clientmod.module;

import com.example.clientmod.MyClientMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Holds all modules and saves/loads which ones are on. Add new modules to ALL below. */
public final class ModuleManager {
    public static final Module COORDS = new Module("coords", "Coordinates HUD",
            "Shows your X / Y / Z position in the top-left corner.", true);
    public static final Module FPS = new Module("fps", "FPS Counter",
            "Shows your frames per second in the top-left corner.", true);
    public static final Module AUTO_SPRINT = new Module("autosprint", "Auto Sprint",
            "Sprints automatically while you hold forward.", false);

    public static final List<Module> ALL = List.of(COORDS, FPS, AUTO_SPRINT);

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
            String value = props.getProperty(module.getId());
            if (value != null) {
                module.setEnabled(Boolean.parseBoolean(value));
            }
        }
    }

    public static void save() {
        Properties props = new Properties();
        for (Module module : ALL) {
            props.setProperty(module.getId(), Boolean.toString(module.isEnabled()));
        }
        try (OutputStream out = Files.newOutputStream(configFile())) {
            props.store(out, "My Client Mod settings");
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not save config", e);
        }
    }
}
