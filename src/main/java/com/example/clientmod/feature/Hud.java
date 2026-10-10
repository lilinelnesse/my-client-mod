package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import net.minecraft.util.math.Vec3d;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Draws the on-screen info (FPS, coordinates) and the armor / item list. */
public final class Hud {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static Vec3d lastPos = null;
    private static long lastNanos = 0;
    private static double speedBps = 0;

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

        if (ModuleManager.NETHERITE.isEnabled() && ModuleManager.NF_INFO.get()) {
            NetheriteFinder.Mark m = NetheriteFinder.getNearest();
            String text = m == null
                    ? "Netherite: none in range"
                    : "Netherite: chunk " + m.chunkX() + ", " + m.chunkZ() + " (" + m.count() + " debris, "
                            + NetheriteFinder.distanceTo(client, m) + " blocks)";
            drawInfo(context, client, text, ModuleManager.NF_CORNER.get(), used);
        }

        if (ModuleManager.DIRECTION.isEnabled()) {
            net.minecraft.util.math.Direction facing = client.player.getHorizontalFacing();
            String axis = switch (facing) {
                case NORTH -> "North (-Z)";
                case SOUTH -> "South (+Z)";
                case EAST -> "East (+X)";
                default -> "West (-X)";
            };
            drawInfo(context, client, "Facing: " + axis, ModuleManager.DIRECTION_CORNER.get(), used);
        }

        if (ModuleManager.SPEED.isEnabled()) {
            long now = System.nanoTime();
            Vec3d pos = client.player.getPos();
            if (lastPos != null && now > lastNanos) {
                double dt = (now - lastNanos) / 1.0e9;
                double dx = pos.x - lastPos.x, dz = pos.z - lastPos.z;
                double instant = Math.sqrt(dx * dx + dz * dz) / dt;
                speedBps = speedBps * 0.9 + Math.min(instant, 200) * 0.1;
            }
            lastPos = pos;
            lastNanos = now;
            drawInfo(context, client, String.format(Locale.ROOT, "Speed: %.1f b/s", speedBps),
                    ModuleManager.SPEED_CORNER.get(), used);
        }

        if (ModuleManager.CLOCK.isEnabled()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(ModuleManager.CLOCK_24H.get() ? "HH:mm" : "h:mm a",
                    Locale.ROOT);
            drawInfo(context, client, LocalTime.now().format(fmt), ModuleManager.CLOCK_CORNER.get(), used);
        }

        if (ModuleManager.HEALTH.isEnabled()) {
            float hp = client.player.getHealth();
            float abs = client.player.getAbsorptionAmount();
            String text = String.format(Locale.ROOT, "Health: %.1f / %.0f", hp, client.player.getMaxHealth())
                    + (abs > 0 ? String.format(Locale.ROOT, " (+%.1f)", abs) : "");
            drawInfo(context, client, text, ModuleManager.HEALTH_CORNER.get(), used);
        }

        PvpHud.render(context, client, used);

        if (ModuleManager.ARMOR_HUD.isEnabled()) {
            drawArmor(context, client);
        }
    }

    static void drawInfo(DrawContext context, MinecraftClient client, String text, int corner, int[] used) {
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
