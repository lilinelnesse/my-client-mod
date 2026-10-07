package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Draws the on-screen info (FPS, coordinates) and the armor / item list. */
public final class Hud {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private Hud() {}

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        int[] used = new int[ModuleManager.CORNERS.size()];

        if (ModuleManager.FPS.isEnabled()) {
            drawInfo(context, client, "FPS: " + client.getCurrentFps(),
                    ModuleManager.FPS_CORNER.get(), used);
        }

        if (ModuleManager.COORDS.isEnabled()) {
            int decimals = ModuleManager.COORDS_DECIMALS.get();
            String format = "XYZ: %." + decimals + "f / %." + decimals + "f / %." + decimals + "f";
            String coords = String.format(Locale.ROOT, format,
                    client.player.getX(), client.player.getY(), client.player.getZ());
            drawInfo(context, client, coords, ModuleManager.COORDS_CORNER.get(), used);
        }

        if (ModuleManager.ARMOR_HUD.isEnabled()) {
            drawArmor(context, client);
        }
    }

    private static void drawInfo(DrawContext context, MinecraftClient client, String text, int corner, int[] used) {
        TextRenderer font = client.textRenderer;
        int textWidth = font.getWidth(text);
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int x;
        int y;
        switch (corner) {
            case 1 -> { // Top Right
                x = screenWidth - 4 - textWidth;
                y = 4 + used[1];
            }
            case 2 -> { // Bottom Right
                x = screenWidth - 4 - textWidth;
                y = screenHeight - 4 - 9 - used[2];
            }
            default -> { // Top Left
                x = 4;
                y = 4 + used[0];
            }
        }
        context.drawTextWithShadow(font, text, x, y, 0xFFFFFF);
        used[corner] += 10;
    }

    private static void drawArmor(DrawContext context, MinecraftClient client) {
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        if (ModuleManager.ARMOR_HANDS.get()) {
            ItemStack main = client.player.getEquippedStack(EquipmentSlot.MAINHAND);
            ItemStack off = client.player.getEquippedStack(EquipmentSlot.OFFHAND);
            if (!main.isEmpty()) {
                stacks.add(main);
            }
            if (!off.isEmpty()) {
                stacks.add(off);
            }
        }
        if (stacks.isEmpty()) {
            return;
        }

        TextRenderer font = client.textRenderer;
        boolean right = ModuleManager.ARMOR_SIDE.get() == 1;
        boolean numbers = ModuleManager.ARMOR_NUMBERS.get();
        int rowHeight = 18;
        int y = context.getScaledWindowHeight() / 2 - (stacks.size() * rowHeight) / 2;

        for (ItemStack stack : stacks) {
            String text = "";
            if (numbers && stack.isDamageable()) {
                text = Integer.toString(stack.getMaxDamage() - stack.getDamage());
            }
            int textWidth = font.getWidth(text);
            int rowWidth = 16 + (text.isEmpty() ? 0 : 3 + textWidth);
            int x = right ? context.getScaledWindowWidth() - 4 - rowWidth : 4;

            context.drawItem(stack, x, y);
            context.drawStackOverlay(font, stack, x, y);
            if (!text.isEmpty()) {
                context.drawTextWithShadow(font, text, x + 19, y + 4, stack.getItemBarColor() | 0xFF000000);
            }
            y += rowHeight;
        }
    }
}
