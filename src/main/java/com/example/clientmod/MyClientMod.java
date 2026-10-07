package com.example.clientmod;

import com.example.clientmod.gui.ModMenuScreen;
import com.example.clientmod.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class MyClientMod implements ClientModInitializer {
    public static final String MOD_ID = "myclientmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding greetKey;
    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("My Client Mod 1.1.0 loaded (module menu build)");

        ModuleManager.load();

        // Press G in-game to show a chat message (rebindable in Controls)
        greetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclientmod.greet",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.myclientmod"
        ));

        // Press Right Shift to open the module menu (rebindable in Controls)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclientmod.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.myclientmod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (greetKey.wasPressed()) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Hello from My Client Mod!"), false);
                }
            }

            while (menuKey.wasPressed()) {
                LOGGER.info("Menu key pressed, opening module menu");
                client.setScreen(new ModMenuScreen());
            }

            tickAutoSprint(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
    }

    private static void tickAutoSprint(MinecraftClient client) {
        if (!ModuleManager.AUTO_SPRINT.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) {
            return;
        }
        if (client.options.forwardKey.isPressed()
                && !player.isSneaking()
                && !player.horizontalCollision
                && player.getHungerManager().getFoodLevel() > 6) {
            player.setSprinting(true);
        }
    }

    private static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        int x = 4;
        int y = 4;

        if (ModuleManager.FPS.isEnabled()) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("FPS: " + client.getCurrentFps()), x, y, 0xFFFFFF);
            y += 10;
        }

        if (ModuleManager.COORDS.isEnabled()) {
            String coords = String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                    client.player.getX(), client.player.getY(), client.player.getZ());
            context.drawTextWithShadow(client.textRenderer, Text.literal(coords), x, y, 0xFFFFFF);
        }
    }
}
