package com.example.clientmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Draws your armor and held item (with durability) down the side of the screen. */
public final class ArmorHud {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };

    private ArmorHud() {}

    public static void render(DrawContext context, MinecraftClient client) {
        if (!ModuleManager.ARMOR_HUD.isEnabled() || client.player == null) {
            return;
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int rowHeight = 20;
        int y = screenH / 2 - (SLOTS.length * rowHeight) / 2;
        int iconX = Settings.hudRight ? screenW - 22 : 6;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                context.drawItem(stack, iconX, y);
                context.drawStackOverlay(client.textRenderer, stack, iconX, y);

                if (Settings.showDurability && stack.isDamageable()) {
                    int max = stack.getMaxDamage();
                    int left = max - stack.getDamage();
                    String text = Integer.toString(left);
                    float fraction = max > 0 ? (float) left / max : 1f;
                    int color = fraction > 0.5f ? 0x55FF55 : (fraction > 0.25f ? 0xFFFF55 : 0xFF5555);
                    int textX = Settings.hudRight
                            ? iconX - 4 - client.textRenderer.getWidth(text)
                            : iconX + 20;
                    context.drawTextWithShadow(client.textRenderer, text, textX, y + 4, color);
                }
            }
            y += rowHeight;
        }
    }
}
