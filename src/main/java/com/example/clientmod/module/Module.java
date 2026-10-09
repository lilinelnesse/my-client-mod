package com.example.clientmod.module;

import com.example.clientmod.module.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/** A single toggleable feature shown in the in-game menu, with optional settings. */
public class Module {
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;

    public Module(String id, String name, String description, boolean enabledByDefault) {
        this(id, name, description, "Utility", enabledByDefault);
    }

    public Module(String id, String name, String description, String category, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabledByDefault;
    }

    /** Registers a setting on this module and returns it, so it can be kept in a field. */
    public <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public List<Setting> getSettings() { return settings; }
    public boolean hasSettings() { return !settings.isEmpty(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}
