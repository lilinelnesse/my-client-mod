package com.example.clientmod.gui;

import com.example.clientmod.module.Module;
import com.example.clientmod.module.ModuleManager;
import com.example.clientmod.module.setting.BoolSetting;
import com.example.clientmod.module.setting.ChoiceSetting;
import com.example.clientmod.module.setting.IntSetting;
import com.example.clientmod.module.setting.Setting;
import com.example.clientmod.module.setting.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Settings page for one module. Builds one widget per setting. Saved when closed. */
public class ModuleSettingsScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int ROW = 24;
    private static final int TEXT_ROW = 38;

    private final Screen parent;
    private final Module module;
    private final List<int[]> labelPositions = new ArrayList<>();
    private final List<String> labelTexts = new ArrayList<>();

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Text.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        labelPositions.clear();
        labelTexts.clear();

        int total = ROW; // Done button
        for (Setting setting : module.getSettings()) {
            total += setting instanceof StringSetting ? TEXT_ROW : ROW;
        }

        int x = this.width / 2 - WIDTH / 2;
        int y = Math.max(40, this.height / 2 - total / 2 + 10);

        for (Setting setting : module.getSettings()) {
            if (setting instanceof BoolSetting bool) {
                this.addDrawableChild(ButtonWidget.builder(boolLabel(bool), btn -> {
                    bool.toggle();
                    btn.setMessage(boolLabel(bool));
                }).dimensions(x, y, WIDTH, 20).build());
                y += ROW;
            } else if (setting instanceof ChoiceSetting choice) {
                this.addDrawableChild(ButtonWidget.builder(choiceLabel(choice), btn -> {
                    choice.cycle();
                    btn.setMessage(choiceLabel(choice));
                }).dimensions(x, y, WIDTH, 20).build());
                y += ROW;
            } else if (setting instanceof IntSetting number) {
                this.addDrawableChild(new IntSlider(x, y, WIDTH, 20, number));
                y += ROW;
            } else if (setting instanceof StringSetting text) {
                labelTexts.add(text.getLabel());
                labelPositions.add(new int[]{x, y});
                TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y + 12, WIDTH, 20,
                        Text.literal(text.getLabel()));
                field.setMaxLength(512);
                field.setText(text.get());
                field.setChangedListener(text::set);
                this.addDrawableChild(field);
                y += TEXT_ROW;
            }
        }

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> this.close())
                .dimensions(x, y + 4, WIDTH, 20).build());
    }

    private static Text boolLabel(BoolSetting setting) {
        return Text.literal(setting.getLabel() + ": " + (setting.get() ? "ON" : "OFF"));
    }

    private static Text choiceLabel(ChoiceSetting setting) {
        return Text.literal(setting.getLabel() + ": " + setting.getValue());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        for (int i = 0; i < labelTexts.size(); i++) {
            int[] pos = labelPositions.get(i);
            context.drawTextWithShadow(this.textRenderer, labelTexts.get(i), pos[0], pos[1], 0xA0A0A0);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }

    /** A slider that edits an IntSetting. */
    private static class IntSlider extends SliderWidget {
        private final IntSetting setting;

        IntSlider(int x, int y, int width, int height, IntSetting setting) {
            super(x, y, width, height, Text.empty(), toRatio(setting));
            this.setting = setting;
            updateMessage();
        }

        private static double toRatio(IntSetting s) {
            return (s.get() - s.getMin()) / (double) (s.getMax() - s.getMin());
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(setting.getLabel() + ": " + setting.get()));
        }

        @Override
        protected void applyValue() {
            setting.set((int) Math.round(setting.getMin() + this.value * (setting.getMax() - setting.getMin())));
        }
    }
}
