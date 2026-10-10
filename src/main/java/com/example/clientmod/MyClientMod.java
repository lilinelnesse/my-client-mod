package com.example.clientmod;

import com.example.clientmod.feature.AutoMine;
import com.example.clientmod.feature.BlockHighlighter;
import com.example.clientmod.feature.Freecam;
import com.example.clientmod.feature.Fullbright;
import com.example.clientmod.feature.Hud;
import com.example.clientmod.feature.NetheriteFinder;
import com.example.clientmod.feature.StorageEsp;
import com.example.clientmod.gui.ModMenuScreen;
import com.example.clientmod.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClientMod implements ClientModInitializer {
    public static final String MOD_ID = "myclientmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding greetKey;
    private static KeyBinding menuKey;
    private static KeyBinding freecamKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("My Client Mod 1.14.0 loaded (modules: HUD, armor HUD, fullbright, block highlight)");

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

        // Press F6 to turn Freecam on or off (rebindable in Controls)
        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclientmod.freecam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "category.myclientmod"
        ));

        // Freecam runs at the start of the tick, before the player reads the movement keys
        ClientTickEvents.START_CLIENT_TICK.register(Freecam::tick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (freecamKey.wasPressed()) {
                ModuleManager.FREECAM.toggle();
                ModuleManager.save();
            }

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
            NetheriteFinder.tick(client);
            AutoMine.tick(client);
            StorageEsp.tick(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> Hud.render(context));
        WorldRenderEvents.LAST.register(BlockHighlighter::render);
        WorldRenderEvents.LAST.register(NetheriteFinder::render);
        WorldRenderEvents.LAST.register(StorageEsp::render);
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
