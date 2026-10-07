package com.example.clientmod.module;

/** A single toggleable feature shown in the in-game menu. */
public class Module {
    private final String id;
    private final String name;
    private final String description;
    private boolean enabled;

    public Module(String id, String name, String description, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabledByDefault;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}
