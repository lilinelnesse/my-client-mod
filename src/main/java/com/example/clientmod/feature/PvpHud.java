package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/** The on-screen PvP info: CPS counter, keystrokes, potion effects and ping. */
public final class PvpHud {
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static boolean leftWas = false;
    private static boolean rightWas = false;

    private PvpHud() {}

    static void render(DrawContext context, MinecraftClient client, int[] used) {
        long now = System.currentTimeMillis();
        long handle = client.getWindow().getHandle();

        // click detection: poll the mouse every frame and count the moments a button goes down
        boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (client.currentScreen == null) {
            if (left && !leftWas) LEFT_CLICKS.addLast(now);
            if (right && !rightWas) RIGHT_CLICKS.addLast(now);
        }
        leftWas = left;
        rightWas = right;
        while (!LEFT_CLICKS.isEmpty() && now - LEFT_CLICKS.peekFirst() > 1000) LEFT_CLICKS.removeFirst();
        while (!RIGHT_CLICKS.isEmpty() && now - RIGHT_CLICKS.peekFirst() > 1000) RIGHT_CLICKS.removeFirst();

        if (ModuleManager.CPS.isEnabled()) {
            String text = ModuleManager.CPS_BOTH.get()
                    ? "CPS: " + LEFT_CLICKS.size() + " | " + RIGHT_CLICKS.size()
                    : "CPS: " + LEFT_CLICKS.size();
            Hud.drawInfo(context, client, text, ModuleManager.CPS_CORNER.get(), used);
        }

        if (ModuleManager.PING.isEnabled() && client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) {
                Hud.drawInfo(context, client, "Ping: " + entry.getLatency() + " ms",
                        ModuleManager.PING_CORNER.get(), used);
            }
        }

        if (ModuleManager.POTION_HUD.isEnabled()) {
            for (StatusEffectInstance effect : client.player.getStatusEffects()) {
                String name = effect.getEffectType().value().getName().getString();
                int level = effect.getAmplifier() + 1;
                String time = effect.isInfinite() ? "inf" : formatTicks(effect.getDuration());
                Hud.drawInfo(context, client, name + (level > 1 ? " " + level : "") + " " + time,
                        ModuleManager.POTION_CORNER.get(), used);
            }
        }

        if (ModuleManager.KEYSTROKES.isEnabled()) {
            drawKeystrokes(context, client, left, right);
        }
    }

    private static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static boolean down(MinecraftClient client, KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        if (key.getCategory() != InputUtil.Type.KEYSYM) {
            return false;
        }
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
    }

    /** W / A S D, then the space bar and the two mouse buttons, in the bottom left corner. */
    private static void drawKeystrokes(DrawContext context, MinecraftClient client, boolean left, boolean right) {
        int size = 18;
        int gap = 2;
        int x0 = 6;
        int y0 = context.getScaledWindowHeight() - 6 - (size * 3 + gap * 3 + 12);

        key(context, client, "W", down(client, client.options.forwardKey), x0 + size + gap, y0, size, size);
        int y1 = y0 + size + gap;
        key(context, client, "A", down(client, client.options.leftKey), x0, y1, size, size);
        key(context, client, "S", down(client, client.options.backKey), x0 + size + gap, y1, size, size);
        key(context, client, "D", down(client, client.options.rightKey), x0 + 2 * (size + gap), y1, size, size);

        int y2 = y1 + size + gap;
        int half = (size * 3 + gap * 2 - gap) / 2;
        key(context, client, "LMB " + LEFT_CLICKS.size(), left, x0, y2, half, 14);
        key(context, client, "RMB " + RIGHT_CLICKS.size(), right, x0 + half + gap, y2, half, 14);
        key(context, client, "____", down(client, client.options.jumpKey), x0, y2 + 14 + gap,
                size * 3 + gap * 2, 8);
    }

    private static void key(DrawContext context, MinecraftClient client, String label, boolean pressed,
                            int x, int y, int w, int h) {
        int accent = ModuleManager.accentArgb();
        context.fill(x, y, x + w, y + h, pressed ? (accent & 0x00FFFFFF) | 0xC0000000 : 0x90000000);
        if (h >= 14) {
            int tx = x + (w - client.textRenderer.getWidth(label)) / 2;
            context.drawTextWithShadow(client.textRenderer, label, tx, y + (h - 8) / 2 + 1, 0xFFFFFFFF);
        }
    }
}
