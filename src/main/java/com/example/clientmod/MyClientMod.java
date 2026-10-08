package com.example.clientmod;

import com.example.clientmod.feature.BlockHighlighter;
import com.example.clientmod.feature.Fullbright;
import com.example.clientmod.feature.Hud;
import com.example.clientmod.gui.ModMenuScreen;
import com.example.clientmod.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClientMod implements ClientModInitializer {
    public static final String MOD_ID = "myclientmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding greetKey;
    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("My Client Mod 1.2.0 (Minecraft 1.21.11 build) loaded");

        ModuleManager.load();

        // Since 1.21.9 key bindings need a category object (label key: key.category.myclientmod.main)
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

        // Press G in-game to show a chat message (rebindable in Controls)
        greetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclientmod.greet",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                category
        ));

        // Press Right Shift to open the module menu (rebindable in Controls)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclientmod.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
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
            Fullbright.tick(client);
            BlockHighlighter.tick(client);
        });

        // HUD: HudRenderCallback was replaced by the HudElementRegistry
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of(MOD_ID, "hud"),
                (context, tickCounter) -> Hud.render(context));

        // World drawing: the block outlines are drawn right after entities
        WorldRenderEvents.AFTER_ENTITIES.register(BlockHighlighter::render);
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
}
