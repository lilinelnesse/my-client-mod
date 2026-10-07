package com.example.clientmod.gui;

import com.example.clientmod.module.ModuleManager;
import com.example.clientmod.module.Settings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/** Settings page: sliders and options for the modules. Opened from the main module menu. */
public class SettingsScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;

    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        int rows = 6;
        int y = Math.max(40, this.height / 2 - (rows * SPACING) / 2 + 10);

        // Block highlighter: what to look for
        ButtonWidget targetButton = ButtonWidget.builder(targetLabel(), btn -> {
            Settings.highlightPreset = (Settings.highlightPreset + 1) % Settings.PRESET_NAMES.length;
            btn.setMessage(targetLabel());
        }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        targetButton.setTooltip(Tooltip.of(Text.literal(
                "Which blocks the Block Highlighter looks for. 'Custom' uses the list in config/myclientmod.properties.")));
        this.addDrawableChild(targetButton);
        y += SPACING;

        this.addDrawableChild(new IntSlider(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                "Highlight Range", " blocks", 8, 40, Settings.highlightRange,
                value -> Settings.highlightRange = value));
        y += SPACING;

        this.addDrawableChild(new IntSlider(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                "Highlight Opacity", "%", 10, 80, Settings.highlightOpacity,
                value -> Settings.highlightOpacity = value));
        y += SPACING;

        // Armor HUD options
        this.addDrawableChild(ButtonWidget.builder(sideLabel(), btn -> {
            Settings.hudRight = !Settings.hudRight;
            btn.setMessage(sideLabel());
        }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += SPACING;

        this.addDrawableChild(ButtonWidget.builder(durabilityLabel(), btn -> {
            Settings.showDurability = !Settings.showDurability;
            btn.setMessage(durabilityLabel());
        }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += SPACING;

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> this.close())
                .dimensions(x, y + 6, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private static Text targetLabel() {
        return Text.literal("Highlight Targets: " + Settings.PRESET_NAMES[Settings.highlightPreset]);
    }

    private static Text sideLabel() {
        return Text.literal("Armor HUD Side: " + (Settings.hudRight ? "Right" : "Left"));
    }

    private static Text durabilityLabel() {
        return Text.literal("Durability Numbers: " + (Settings.showDurability ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
