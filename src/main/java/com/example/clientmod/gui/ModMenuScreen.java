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

/** The in-game menu (press Right Shift): one toggle per module, plus a Settings button where available. */
public class ModMenuScreen extends Screen {
    private static final int TOGGLE_WIDTH = 150;
    private static final int SETTINGS_WIDTH = 66;
    private static final int GAP = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;

    public ModMenuScreen() {
        super(Text.literal("My Client Mod"));
    }

    @Override
    protected void init() {
        int rowWidth = TOGGLE_WIDTH + GAP + SETTINGS_WIDTH;
        int count = ModuleManager.ALL.size();
        int totalHeight = count * SPACING + SPACING; // modules + Done button
        int x = this.width / 2 - rowWidth / 2;
        int y = Math.max(40, this.height / 2 - totalHeight / 2 + 10);

        for (Module module : ModuleManager.ALL) {
            ButtonWidget toggle = ButtonWidget.builder(label(module), btn -> {
                module.toggle();
                btn.setMessage(label(module));
                ModuleManager.save();
            }).dimensions(x, y, TOGGLE_WIDTH, BUTTON_HEIGHT).build();
            toggle.setTooltip(Tooltip.of(Text.literal(module.getDescription())));
            this.addDrawableChild(toggle);

            if (module.hasSettings()) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Settings"),
                        btn -> this.client.setScreen(new ModuleSettingsScreen(this, module)))
                        .dimensions(x + TOGGLE_WIDTH + GAP, y, SETTINGS_WIDTH, BUTTON_HEIGHT).build());
            }
            y += SPACING;
        }

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> this.close())
                .dimensions(x, y + 6, rowWidth, BUTTON_HEIGHT).build());
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
