package com.example.clientmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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

    @Override
    public void onInitializeClient() {
        LOGGER.info("My Client Mod loaded");

        // Sample keybind: press G in-game to show a chat message (rebindable in Controls)
        greetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclientmod.greet",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.myclientmod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (greetKey.wasPressed()) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Hello from My Client Mod!"), false);
                }
            }
        });
    }
}
