package com.example.clientmod.module.setting;

public class BoolSetting extends Setting {
    private boolean value;

    public BoolSetting(String id, String label, boolean defaultValue) {
        super(id, label);
        this.value = defaultValue;
    }

    public boolean get() { return value; }
    public void set(boolean value) { this.value = value; }
    public void toggle() { this.value = !this.value; }

    @Override
    public String serialize() { return Boolean.toString(value); }

    @Override
    public void deserialize(String s) { this.value = Boolean.parseBoolean(s.trim()); }
}
