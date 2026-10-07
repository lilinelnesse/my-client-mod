package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Fullbright, done by giving the player a hidden client-side Night Vision effect.
 * It is refreshed while the module is on and removed again when it is turned off.
 */
public final class Fullbright {
    private static boolean applied = false;

    private Fullbright() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            applied = false;
            return;
        }

        if (ModuleManager.FULLBRIGHT.isEnabled()) {
            StatusEffectInstance current = player.getStatusEffect(StatusEffects.NIGHT_VISION);
            // Leave real night vision (e.g. from a potion) alone.
            if (current == null || (applied && current.getDuration() < 600)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, 1200, 0, false, false, false));
                applied = true;
            }
        } else if (applied) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            applied = false;
        }
    }
}
