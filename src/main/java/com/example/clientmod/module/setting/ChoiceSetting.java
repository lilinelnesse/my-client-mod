package com.example.clientmod.module.setting;

import java.util.List;

/** A setting that cycles through a fixed list of named options when clicked. */
public class ChoiceSetting extends Setting {
    private final List<String> options;
    private int index;

    public ChoiceSetting(String id, String label, List<String> options, int defaultIndex) {
        super(id, label);
        this.options = options;
        this.index = Math.max(0, Math.min(options.size() - 1, defaultIndex));
    }

    /** Index of the selected option. */
    public int get() { return index; }
    public String getValue() { return options.get(index); }
    public void cycle() { index = (index + 1) % options.size(); }

    @Override
    public String serialize() { return options.get(index); }

    @Override
    public void deserialize(String s) {
        int found = options.indexOf(s.trim());
        if (found >= 0) {
            index = found;
        }
    }
}
