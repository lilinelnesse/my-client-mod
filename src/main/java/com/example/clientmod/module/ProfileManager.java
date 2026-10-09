package com.example.clientmod.module;

import com.example.clientmod.MyClientMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Profiles: named snapshots of every module on/off state and setting.
 * Stored as one file per profile in config/myclientmod-profiles/.
 * The active profile is kept up to date automatically whenever settings change.
 */
public final class ProfileManager {
    public static final String DEFAULT = "default";
    private static final String EXTENSION = ".properties";

    private static String active = DEFAULT;

    private ProfileManager() {}

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("myclientmod-profiles");
    }

    private static Path file(String name) {
        return dir().resolve(name + EXTENSION);
    }

    public static String getActive() {
        return active;
    }

    public static void setActive(String name) {
        active = (name == null || name.isBlank()) ? DEFAULT : name.trim();
    }

    /** All profile names, "default" first. */
    public static List<String> list() {
        if (!Files.exists(file(DEFAULT))) {
            writeProfile(DEFAULT, ModuleManager.snapshot());
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir())) {
            files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(EXTENSION))
                    .map(n -> n.substring(0, n.length() - EXTENSION.length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(names::add);
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not list profiles", e);
        }
        names.remove(DEFAULT);
        names.add(0, DEFAULT);
        return names;
    }

    private static void writeProfile(String name, Properties props) {
        try {
            Files.createDirectories(dir());
            try (OutputStream out = Files.newOutputStream(file(name))) {
                props.store(out, "My Client Mod profile: " + name);
            }
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not save profile " + name, e);
        }
    }

    private static Properties readProfile(String name) {
        Path path = file(name);
        if (!Files.exists(path)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            return props;
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not read profile " + name, e);
            return null;
        }
    }

    /** Writes the current settings into the active profile. */
    public static void writeActive() {
        writeProfile(active, ModuleManager.snapshot());
    }

    /** Loads a profile and makes it the active one. */
    public static void switchTo(String name) {
        Properties props = readProfile(name);
        if (props == null) {
            return;
        }
        ModuleManager.apply(props);
        active = name;
        ModuleManager.save();
    }

    /** Keeps only letters, digits, space, '-' and '_' (safe for a file name), max 20 characters. */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
                out.append(c);
            }
        }
        String cleaned = out.toString().trim();
        return cleaned.length() > 20 ? cleaned.substring(0, 20).trim() : cleaned;
    }

    private static boolean nameTaken(String name) {
        for (String existing : list()) {
            if (existing.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new profile from the current settings and makes it active.
     * An empty name becomes "Profile 1", "Profile 2", ...; a taken name gets " 2", " 3", ... added.
     */
    public static String create(String requested) {
        String base = sanitize(requested);
        String name;
        if (base.isEmpty()) {
            int number = 1;
            while (nameTaken("Profile " + number)) {
                number++;
            }
            name = "Profile " + number;
        } else {
            name = base;
            int number = 2;
            while (nameTaken(name)) {
                name = base + " " + number++;
            }
        }
        active = name;
        ModuleManager.save(); // also writes the new profile file
        return name;
    }

    public static String create() {
        return create("");
    }

    public static void delete(String name) {
        if (DEFAULT.equals(name)) {
            return;
        }
        try {
            Files.deleteIfExists(file(name));
        } catch (IOException e) {
            MyClientMod.LOGGER.warn("Could not delete profile " + name, e);
        }
        if (name.equals(active)) {
            switchTo(DEFAULT);
        }
    }
}
