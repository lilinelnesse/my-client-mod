package com.example.clientmod.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

/** A slider that picks a whole number between min and max. */
public class IntSlider extends SliderWidget {
    private final String label;
    private final int min;
    private final int max;
    private final String suffix;
    private final IntConsumer onChange;

    public IntSlider(int x, int y, int width, int height, String label, String suffix,
                     int min, int max, int current, IntConsumer onChange) {
        super(x, y, width, height, Text.empty(), (current - min) / (double) (max - min));
        this.label = label;
        this.suffix = suffix;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
    }

    private int currentValue() {
        return min + (int) Math.round(this.value * (max - min));
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Text.literal(label + ": " + currentValue() + (suffix == null ? "" : suffix)));
    }

    @Override
    protected void applyValue() {
        onChange.accept(currentValue());
    }
}
