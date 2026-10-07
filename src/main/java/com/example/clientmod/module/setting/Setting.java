package com.example.clientmod.module.setting;

/** One configurable option of a module (shown in that module's Settings screen). */
public abstract class Setting {
    private final String id;
    private final String label;

    protected Setting(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }

    /** Value as text, for saving to the config file. */
    public abstract String serialize();

    /** Restores the value from the config file. Must ignore bad input. */
    public abstract void deserialize(String value);
}
