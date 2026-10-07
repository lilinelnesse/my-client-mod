package com.example.clientmod.module.setting;

public class StringSetting extends Setting {
    private String value;

    public StringSetting(String id, String label, String defaultValue) {
        super(id, label);
        this.value = defaultValue;
    }

    public String get() { return value; }
    public void set(String value) { this.value = value; }

    @Override
    public String serialize() { return value; }

    @Override
    public void deserialize(String s) { this.value = s; }
}
