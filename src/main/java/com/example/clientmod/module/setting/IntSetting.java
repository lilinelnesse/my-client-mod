package com.example.clientmod.module.setting;

public class IntSetting extends Setting {
    private final int min;
    private final int max;
    private int value;

    public IntSetting(String id, String label, int min, int max, int defaultValue) {
        super(id, label);
        this.min = min;
        this.max = max;
        this.value = clamp(defaultValue);
    }

    private int clamp(int v) { return Math.max(min, Math.min(max, v)); }

    public int get() { return value; }
    public void set(int value) { this.value = clamp(value); }
    public int getMin() { return min; }
    public int getMax() { return max; }

    @Override
    public String serialize() { return Integer.toString(value); }

    @Override
    public void deserialize(String s) {
        try {
            set(Integer.parseInt(s.trim()));
        } catch (NumberFormatException ignored) {
            // keep current value
        }
    }
}
