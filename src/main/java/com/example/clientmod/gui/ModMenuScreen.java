package com.example.clientmod.gui;

import com.example.clientmod.module.Module;
import com.example.clientmod.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** The in-game menu (press Right Shift) with one toggle button per module. */
public class ModMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;

    public ModMenuScreen() {
        super(Text.literal("My Client Mod"));
    }

    @Override
    protected void init() {
        int count = ModuleManager.ALL.size();
        int totalHeight = count * SPACING + SPACING; // modules + Done button
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        int y = this.height / 2 - totalHeight / 2 + 10;

        for (Module module : ModuleManager.ALL) {
            ButtonWidget button = ButtonWidget.builder(label(module), btn -> {
                module.toggle();
                btn.setMessage(label(module));
                ModuleManager.save();
            }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            button.setTooltip(Tooltip.of(Text.literal(module.getDescription())));
            this.addDrawableChild(button);
            y += SPACING;
        }

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> this.close())
                .dimensions(x, y + 6, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private static Text label(Module module) {
        String state = module.isEnabled() ? "ON" : "OFF";
        return Text.literal(module.getName() + ": " + state)
                .formatted(module.isEnabled() ? Formatting.GREEN : Formatting.RED);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}
